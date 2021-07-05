/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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

package org.optaplanner.core.impl.localsearch;

import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.config.solver.SolverMetric;
import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.localsearch.decider.LocalSearchDecider;
import org.optaplanner.core.impl.localsearch.event.LocalSearchPhaseLifecycleListener;
import org.optaplanner.core.impl.localsearch.scope.LocalSearchPhaseScope;
import org.optaplanner.core.impl.localsearch.scope.LocalSearchStepScope;
import org.optaplanner.core.impl.phase.AbstractPhase;
import org.optaplanner.core.impl.score.definition.ScoreDefinition;
import org.optaplanner.core.impl.score.director.InnerScoreDirector;
import org.optaplanner.core.impl.solver.recaller.BestSolutionRecaller;
import org.optaplanner.core.impl.solver.scope.SolverScope;
import org.optaplanner.core.impl.solver.termination.Termination;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

/**
 * Default implementation of {@link LocalSearchPhase}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class DefaultLocalSearchPhase<Solution_> extends AbstractPhase<Solution_> implements LocalSearchPhase<Solution_>,
        LocalSearchPhaseLifecycleListener<Solution_> {

    protected LocalSearchDecider<Solution_> decider;

    public DefaultLocalSearchPhase(int phaseIndex, String logIndentation,
            BestSolutionRecaller<Solution_> bestSolutionRecaller, Termination<Solution_> termination) {
        super(phaseIndex, logIndentation, bestSolutionRecaller, termination);
    }

    public LocalSearchDecider<Solution_> getDecider() {
        return decider;
    }

    public void setDecider(LocalSearchDecider<Solution_> decider) {
        this.decider = decider;
    }

    @Override
    public String getPhaseTypeString() {
        return "Local Search";
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void solve(SolverScope<Solution_> solverScope) {
        LocalSearchPhaseScope<Solution_> phaseScope = new LocalSearchPhaseScope<>(solverScope);
        phaseStarted(phaseScope);

        while (!termination.isPhaseTerminated(phaseScope)) {
            LocalSearchStepScope<Solution_> stepScope = new LocalSearchStepScope<>(phaseScope);
            stepScope.setTimeGradient(termination.calculatePhaseTimeGradient(phaseScope));
            stepStarted(stepScope);
            decider.decideNextStep(stepScope);
            if (stepScope.getStep() == null) {
                if (termination.isPhaseTerminated(phaseScope)) {
                    logger.trace("{}    Step index ({}), time spent ({}) terminated without picking a nextStep.",
                            logIndentation,
                            stepScope.getStepIndex(),
                            stepScope.getPhaseScope().calculateSolverTimeMillisSpentUpToNow());
                } else if (stepScope.getSelectedMoveCount() == 0L) {
                    logger.warn("{}    No doable selected move at step index ({}), time spent ({})."
                            + " Terminating phase early.",
                            logIndentation,
                            stepScope.getStepIndex(),
                            stepScope.getPhaseScope().calculateSolverTimeMillisSpentUpToNow());
                } else {
                    throw new IllegalStateException("The step index (" + stepScope.getStepIndex()
                            + ") has accepted/selected move count (" + stepScope.getAcceptedMoveCount() + "/"
                            + stepScope.getSelectedMoveCount()
                            + ") but failed to pick a nextStep (" + stepScope.getStep() + ").");
                }
                // Although stepStarted has been called, stepEnded is not called for this step
                break;
            }
            doStep(stepScope);
            stepEnded(stepScope);
            phaseScope.setLastCompletedStepScope(stepScope);
        }
        phaseEnded(phaseScope);
    }

    protected void doStep(LocalSearchStepScope<Solution_> stepScope) {
        Move<Solution_> step = stepScope.getStep();
        Move<Solution_> undoStep = step.doMove(stepScope.getScoreDirector());
        stepScope.setUndoStep(undoStep);
        predictWorkingStepScore(stepScope, step);
        bestSolutionRecaller.processWorkingSolutionDuringStep(stepScope);
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        decider.solvingStarted(solverScope);
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        decider.phaseStarted(phaseScope);
        // TODO maybe this restriction should be lifted to allow LocalSearch to initialize a solution too?
        assertWorkingSolutionInitialized(phaseScope);
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        decider.stepStarted(stepScope);
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        decider.stepEnded(stepScope);
        collectMetrics(stepScope);
        LocalSearchPhaseScope<Solution_> phaseScope = stepScope.getPhaseScope();
        if (logger.isDebugEnabled()) {
            logger.debug("{}    LS step ({}), time spent ({}), score ({}), {} best score ({})," +
                    " accepted/selected move count ({}/{}), picked move ({}).",
                    logIndentation,
                    stepScope.getStepIndex(),
                    phaseScope.calculateSolverTimeMillisSpentUpToNow(),
                    stepScope.getScore(),
                    (stepScope.getBestScoreImproved() ? "new" : "   "), phaseScope.getBestScore(),
                    stepScope.getAcceptedMoveCount(),
                    stepScope.getSelectedMoveCount(),
                    stepScope.getStepString());
        }
    }

    private void collectMetrics(LocalSearchStepScope<Solution_> stepScope) {
        LocalSearchPhaseScope<Solution_> phaseScope = stepScope.getPhaseScope();
        SolverScope<Solution_> solverScope = phaseScope.getSolverScope();
        final String solverId = phaseScope.getSolverScope().getSolverId();
        if (solverScope.isMetricEnabled(SolverMetric.MOVE_COUNT_PER_STEP)) {
            Metrics.gauge(SolverMetric.MOVE_COUNT_PER_STEP.getMeterId() + ".accepted",
                    Tags.of("solver.id", solverId), stepScope.getAcceptedMoveCount());
            Metrics.gauge(SolverMetric.MOVE_COUNT_PER_STEP.getMeterId() + ".selected",
                    Tags.of("solver.id", solverId), stepScope.getSelectedMoveCount());
        }
        if (solverScope.isMetricEnabled(SolverMetric.CONSTRAINT_MATCH_TOTAL_STEP_SCORE)
                || solverScope.isMetricEnabled(SolverMetric.CONSTRAINT_MATCH_TOTAL_BEST_SCORE)) {
            InnerScoreDirector<Solution_, ?> scoreDirector = stepScope.getScoreDirector();
            ScoreDefinition scoreDefinition = solverScope.getScoreDefinition();
            if (scoreDirector.isConstraintMatchEnabled()) {
                for (ConstraintMatchTotal<?> constraintMatchTotal : scoreDirector.getConstraintMatchTotalMap()
                        .values()) {
                    if (solverScope.isMetricEnabled(SolverMetric.CONSTRAINT_MATCH_TOTAL_STEP_SCORE)) {
                        Metrics.gauge(SolverMetric.CONSTRAINT_MATCH_TOTAL_STEP_SCORE.getMeterId() + ".count",
                                Tags.of("solver.id", solverId,
                                        "constraint.package", constraintMatchTotal.getConstraintPackage(),
                                        "constraint.name", constraintMatchTotal.getConstraintName()),
                                constraintMatchTotal.getConstraintMatchCount());
                        SolverMetric.registerScoreMetrics(SolverMetric.CONSTRAINT_MATCH_TOTAL_STEP_SCORE,
                                Tags.of("solver.id", solverId,
                                        "constraint.package", constraintMatchTotal.getConstraintPackage(),
                                        "constraint.name", constraintMatchTotal.getConstraintName()),
                                scoreDefinition, stepScope.getScore());
                    }
                    if (solverScope.isMetricEnabled(SolverMetric.CONSTRAINT_MATCH_TOTAL_BEST_SCORE)
                            && stepScope.getBestScoreImproved()) {
                        Metrics.gauge(SolverMetric.CONSTRAINT_MATCH_TOTAL_BEST_SCORE.getMeterId() + ".count",
                                Tags.of("solver.id", solverId,
                                        "constraint.package", constraintMatchTotal.getConstraintPackage(),
                                        "constraint.name", constraintMatchTotal.getConstraintName()),
                                constraintMatchTotal.getConstraintMatchCount());

                        SolverMetric.registerScoreMetrics(SolverMetric.CONSTRAINT_MATCH_TOTAL_BEST_SCORE,
                                Tags.of("solver.id", solverId,
                                        "constraint.package", constraintMatchTotal.getConstraintPackage(),
                                        "constraint.name", constraintMatchTotal.getConstraintName()),
                                scoreDefinition, stepScope.getScore());
                    }
                }
            }
        }
        if (solverScope.isMetricEnabled(SolverMetric.PICKED_MOVE_TYPE_BEST_SCORE_DIFF)
                || solverScope.isMetricEnabled(SolverMetric.PICKED_MOVE_TYPE_STEP_SCORE_DIFF)) {

        }
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        decider.phaseEnded(phaseScope);
        phaseScope.endingNow();
        logger.info("{}Local Search phase ({}) ended: time spent ({}), best score ({}),"
                + " score calculation speed ({}/sec), step total ({}).",
                logIndentation,
                phaseIndex,
                phaseScope.calculateSolverTimeMillisSpentUpToNow(),
                phaseScope.getBestScore(),
                phaseScope.getPhaseScoreCalculationSpeed(),
                phaseScope.getNextStepIndex());
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        decider.solvingEnded(solverScope);
    }

}
