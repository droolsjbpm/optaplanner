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

package org.optaplanner.core.impl.statistic;

import java.util.function.Consumer;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.config.solver.metric.SolverMetric;
import org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor;
import org.optaplanner.core.impl.localsearch.scope.LocalSearchPhaseScope;
import org.optaplanner.core.impl.localsearch.scope.LocalSearchStepScope;
import org.optaplanner.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import org.optaplanner.core.impl.phase.scope.AbstractPhaseScope;
import org.optaplanner.core.impl.phase.scope.AbstractStepScope;
import org.optaplanner.core.impl.score.definition.ScoreDefinition;
import org.optaplanner.core.impl.score.director.InnerScoreDirectorFactory;
import org.optaplanner.core.impl.solver.DefaultSolver;

public class PickedMoveBestScoreDiffStatistic implements Consumer<Solver> {
    @Override
    public void accept(Solver solver) {
        DefaultSolver<?> defaultSolver = (DefaultSolver<?>) solver;
        InnerScoreDirectorFactory<?, ?> innerScoreDirectorFactory = defaultSolver.getScoreDirectorFactory();
        SolutionDescriptor<?> solutionDescriptor = innerScoreDirectorFactory.getSolutionDescriptor();
        defaultSolver.addPhaseLifecycleListener(
                new PickedMoveBestScoreDiffStatisticListener(solutionDescriptor.getScoreDefinition()));
    }

    private static class PickedMoveBestScoreDiffStatisticListener extends PhaseLifecycleListenerAdapter {

        private Score oldBestScore = null;
        private final ScoreDefinition scoreDefinition;

        public PickedMoveBestScoreDiffStatisticListener(ScoreDefinition scoreDefinition) {
            this.scoreDefinition = scoreDefinition;
        }

        @Override
        public void phaseStarted(AbstractPhaseScope phaseScope) {
            if (phaseScope instanceof LocalSearchPhaseScope) {
                oldBestScore = phaseScope.getBestScore();
            }
        }

        @Override
        public void phaseEnded(AbstractPhaseScope phaseScope) {
            if (phaseScope instanceof LocalSearchPhaseScope) {
                oldBestScore = null;
            }
        }

        @Override
        public void stepEnded(AbstractStepScope stepScope) {
            if (stepScope instanceof LocalSearchStepScope) {
                localSearchStepEnded((LocalSearchStepScope) stepScope);
            }
        }

        private void localSearchStepEnded(LocalSearchStepScope stepScope) {
            if (stepScope.getBestScoreImproved()) {
                String moveType = stepScope.getStep().getSimpleMoveTypeDescription();
                Score newBestScore = stepScope.getScore();
                Score bestScoreDiff = newBestScore.subtract(oldBestScore);
                oldBestScore = newBestScore;
                SolverMetric.registerScoreMetrics(SolverMetric.PICKED_MOVE_TYPE_BEST_SCORE_DIFF,
                        stepScope.getPhaseScope().getSolverScope().getMetricTags()
                                .and("move.type", moveType),
                        scoreDefinition,
                        bestScoreDiff);
            }
        }
    }
}
