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

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.config.solver.SolverMetric;
import org.optaplanner.core.impl.score.definition.ScoreDefinition;
import org.optaplanner.core.impl.solver.DefaultSolver;

import io.micrometer.core.instrument.Tags;

public class BestScoreStatistic implements Consumer<Solver> {
    @Override
    public void accept(Solver solver) {
        DefaultSolver defaultSolver = (DefaultSolver) solver;
        ScoreDefinition scoreDefinition = defaultSolver.getSolverScope().getScoreDefinition();
        defaultSolver.addEventListener(event -> {
            SolverMetric.registerScoreMetrics(SolverMetric.BEST_SCORE, Tags.of("solver.id",
                    defaultSolver.getSolverScope().getSolverId()), scoreDefinition, event.getNewBestScore());
        });
    }
}
