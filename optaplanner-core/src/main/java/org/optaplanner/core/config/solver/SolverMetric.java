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

package org.optaplanner.core.config.solver;

import java.util.List;
import java.util.function.Consumer;

import javax.xml.bind.annotation.XmlEnum;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.impl.score.definition.ScoreDefinition;
import org.optaplanner.core.impl.statistic.BestScoreStatistic;
import org.optaplanner.core.impl.statistic.BestSolutionMutationCountStatistic;
import org.optaplanner.core.impl.statistic.MemoryUseStatistic;
import org.optaplanner.core.impl.statistic.PickedMoveBestScoreDiffStatistic;
import org.optaplanner.core.impl.statistic.PickedMoveStepScoreDiffStatistic;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

@XmlEnum
public enum SolverMetric {
    SOLVE_LENGTH("optaplanner.solver.solve-length"),
    ERROR_COUNT("optaplanner.solver.errors"),
    BEST_SCORE("optaplanner.solver.best-score", new BestScoreStatistic()),
    STEP_SCORE("optaplanner.solver.step-score"),
    SCORE_CALCULATION_COUNT("optaplanner.solver.score-calculation-count"),
    BEST_SOLUTION_MUTATION("optaplanner.solver.best-solution-mutation", new BestSolutionMutationCountStatistic()),
    MOVE_COUNT_PER_STEP("optaplanner.solver.step-move-count"),
    MEMORY_USE("optaplanner.solver.memory-use", new MemoryUseStatistic()),
    CONSTRAINT_MATCH_TOTAL_BEST_SCORE("optaplanner.solver.constraint-match.best-score"),
    CONSTRAINT_MATCH_TOTAL_STEP_SCORE("optaplanner.solver.constraint-match.step-score"),
    PICKED_MOVE_TYPE_BEST_SCORE_DIFF("optaplanner.solver.move-type.best-score-diff", new PickedMoveBestScoreDiffStatistic()),
    PICKED_MOVE_TYPE_STEP_SCORE_DIFF("optaplanner.solver.move-type.step-score-diff", new PickedMoveStepScoreDiffStatistic());

    String meterId;
    Consumer<Solver> registerFunction;

    SolverMetric(String meterId) {
        this(meterId, solver -> {
        });
    }

    SolverMetric(String meterId, Consumer<Solver> registerFunction) {
        this.meterId = meterId;
        this.registerFunction = registerFunction;
    }

    public String getMeterId() {
        return meterId;
    }

    public static void registerScoreMetrics(SolverMetric metric, Tags tags, ScoreDefinition scoreDefinition, Score score) {
        String[] levelLabels = scoreDefinition.getLevelLabels();
        Number[] levelValues = score.toLevelNumbers();
        for (int i = 0; i < levelValues.length; i++) {
            Metrics.gauge(metric.getMeterId() + "." + levelLabels[i],
                    tags, levelValues[i]);
        }
    }

    public static void setupMetrics(String solverId, SolverConfig solverConfig, Solver solver) {
        List<SolverMetric> metricsToAcceptList = solverConfig.determineSolverMetrics();
        metricsToAcceptList.forEach(metric -> metric.register(solver));
    }

    public void register(Solver solver) {
        registerFunction.accept(solver);
    }
}
