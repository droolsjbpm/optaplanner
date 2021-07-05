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
import org.optaplanner.core.api.solver.event.BestSolutionChangedEvent;
import org.optaplanner.core.api.solver.event.SolverEventListener;
import org.optaplanner.core.config.solver.SolverMetric;
import org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor;
import org.optaplanner.core.impl.domain.solution.mutation.MutationCounter;
import org.optaplanner.core.impl.score.director.InnerScoreDirectorFactory;
import org.optaplanner.core.impl.solver.DefaultSolver;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

public class BestSolutionMutationCountStatistic implements Consumer<Solver> {
    @Override
    public void accept(Solver solver) {
        DefaultSolver<?> defaultSolver = (DefaultSolver<?>) solver;
        InnerScoreDirectorFactory<?, ?> innerScoreDirectorFactory = defaultSolver.getScoreDirectorFactory();
        SolutionDescriptor<?> solutionDescriptor = innerScoreDirectorFactory.getSolutionDescriptor();
        MutationCounter mutationCounter = new MutationCounter(solutionDescriptor);
        solver.addEventListener(Metrics.gauge(SolverMetric.BEST_SOLUTION_MUTATION.getMeterId(),
                Tags.of("solver.id", defaultSolver.getSolverScope().getSolverId()),
                new BestSolutionMutationCountStatisticListener(mutationCounter),
                BestSolutionMutationCountStatisticListener::getMutationCount));
    }

    private class BestSolutionMutationCountStatisticListener implements SolverEventListener {
        final MutationCounter mutationCounter;
        int mutationCount = 0;
        Object oldBestSolution = null;

        public BestSolutionMutationCountStatisticListener(MutationCounter mutationCounter) {
            this.mutationCounter = mutationCounter;
        }

        public int getMutationCount() {
            return mutationCount;
        }

        @Override
        public void bestSolutionChanged(BestSolutionChangedEvent event) {
            Object newBestSolution = event.getNewBestSolution();
            if (oldBestSolution == null) {
                mutationCount = 0;
            } else {
                mutationCount = mutationCounter.countMutations(oldBestSolution, newBestSolution);
            }
            oldBestSolution = newBestSolution;
        }
    }
}
