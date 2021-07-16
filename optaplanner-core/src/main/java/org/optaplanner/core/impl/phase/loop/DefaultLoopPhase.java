/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.core.impl.phase.loop;

import java.util.List;

import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.impl.phase.AbstractPhase;
import org.optaplanner.core.impl.phase.PhaseCounter;
import org.optaplanner.core.impl.phase.event.PhaseLifecycleListener;
import org.optaplanner.core.impl.phase.event.PhaseLifecycleSupport;
import org.optaplanner.core.impl.phase.loop.scope.LoopPhaseScope;
import org.optaplanner.core.impl.phase.loop.scope.LoopStepScope;
import org.optaplanner.core.impl.solver.recaller.BestSolutionRecaller;
import org.optaplanner.core.impl.solver.scope.SolverScope;
import org.optaplanner.core.impl.solver.termination.Termination;

/**
 * Default implementation of {@link LoopPhase}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class DefaultLoopPhase<Solution_> extends AbstractPhase<Solution_> implements LoopPhase<Solution_> {

    private final List<AbstractPhase<Solution_>> phaseList;

    public DefaultLoopPhase(PhaseCounter<Solution_> phaseCounter, String logIndentation,
            BestSolutionRecaller<Solution_> bestSolutionRecaller, Termination<Solution_> termination,
            List<AbstractPhase<Solution_>> phaseList) {
        super(phaseCounter, logIndentation, bestSolutionRecaller, termination);
        this.phaseList = phaseList;
    }

    @Override
    public String getPhaseTypeString() {
        return "Loop";
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void solve(SolverScope<Solution_> solverScope) {
        LoopPhaseScope<Solution_> phaseScope = new LoopPhaseScope<>(solverScope);
        phaseStarted(phaseScope);

        boolean terminated = false;
        do { // Loop the nested phases until termination is reached.
            for (AbstractPhase<Solution_> phase : phaseList) {
                LoopStepScope<Solution_> stepScope = new LoopStepScope<>(phaseScope);
                stepStarted(stepScope);
                doStep(solverScope, stepScope, phase);
                stepEnded(stepScope);
                phaseScope.setLastCompletedStepScope(stepScope);

                solverScope.checkYielding();
                terminated = termination.isPhaseTerminated(phaseScope);
                if (terminated) { // The termination condition may be met inbetween the nested phases.
                    break;
                }
            }
        } while (!terminated);
        phaseEnded(phaseScope);
    }

    @Override
    public void addPhaseLifecycleListener(PhaseLifecycleListener<Solution_> phaseLifecycleListener) {
        super.addPhaseLifecycleListener(phaseLifecycleListener);
        phaseList.forEach(phase -> phase.addPhaseLifecycleListener(phaseLifecycleListener));
    }

    @Override
    public void removePhaseLifecycleListener(PhaseLifecycleListener<Solution_> phaseLifecycleListener) {
        super.removePhaseLifecycleListener(phaseLifecycleListener);
        phaseList.forEach(phase -> phase.removePhaseLifecycleListener(phaseLifecycleListener));
    }

    @Override
    public void setSolverPhaseLifecycleSupport(PhaseLifecycleSupport<Solution_> solverPhaseLifecycleSupport) {
        super.setSolverPhaseLifecycleSupport(solverPhaseLifecycleSupport);
        phaseList.forEach(phase -> phase.setSolverPhaseLifecycleSupport(solverPhaseLifecycleSupport));
    }

    @Override
    public void setAssertStepScoreFromScratch(boolean assertStepScoreFromScratch) {
        super.setAssertStepScoreFromScratch(assertStepScoreFromScratch);
        phaseList.forEach(phase -> phase.setAssertStepScoreFromScratch(assertStepScoreFromScratch));
    }

    @Override
    public void setAssertExpectedStepScore(boolean assertExpectedStepScore) {
        super.setAssertExpectedStepScore(assertExpectedStepScore);
        phaseList.forEach(phase -> phase.setAssertExpectedStepScore(assertExpectedStepScore));
    }

    @Override
    public void setAssertShadowVariablesAreNotStaleAfterStep(boolean assertShadowVariablesAreNotStaleAfterStep) {
        super.setAssertShadowVariablesAreNotStaleAfterStep(assertShadowVariablesAreNotStaleAfterStep);
        phaseList.forEach(
                phase -> phase.setAssertShadowVariablesAreNotStaleAfterStep(assertShadowVariablesAreNotStaleAfterStep));
    }

    private void doStep(SolverScope<Solution_> solverScope, LoopStepScope<Solution_> stepScope,
            AbstractPhase<Solution_> phase) {
        phase.solve(solverScope);
        calculateWorkingStepScore(stepScope, phase);
        bestSolutionRecaller.processWorkingSolutionDuringStep(stepScope);
    }

    public void phaseStarted(LoopPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
    }

    public void stepStarted(LoopStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
    }

    public void stepEnded(LoopStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        boolean bestScoreImproved = stepScope.getBestScoreImproved();
        if (!bestScoreImproved) {
            bestSolutionRecaller.updateBestSolution(stepScope.getPhaseScope().getSolverScope());
        }
        LoopPhaseScope<Solution_> phaseScope = stepScope.getPhaseScope();
        if (logger.isDebugEnabled()) {
            logger.debug("{}    Loop step ({}), time spent ({}), score ({}), {} best score ({}).",
                    logIndentation,
                    stepScope.getStepIndex(),
                    phaseScope.calculateSolverTimeMillisSpentUpToNow(),
                    stepScope.getScore(),
                    bestScoreImproved ? "new" : "   ",
                    phaseScope.getBestScore());
        }
    }

    public void phaseEnded(LoopPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        phaseScope.endingNow();
        logger.info("{}Loop phase ({}) ended: time spent ({}), best score ({}),"
                + " score calculation speed ({}/sec), step total ({}).",
                logIndentation,
                getPhaseIndex(),
                phaseScope.calculateSolverTimeMillisSpentUpToNow(),
                phaseScope.getBestScore(),
                phaseScope.getPhaseScoreCalculationSpeed(),
                phaseScope.getNextStepIndex());
    }

}
