/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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

package org.optaplanner.core.impl.solver.event;

import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.event.SolverListener;
import org.optaplanner.core.api.solver.event.SolvingEndedEvent;
import org.optaplanner.core.api.solver.event.SolvingStartedEvent;
import org.optaplanner.core.impl.solver.scope.DefaultSolverScope;

/**
 * Internal API.
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SolverListenerSupport<Solution_> extends AbstractEventSupport<SolverListener<Solution_>> {

    private final Solver<Solution_> solver;

    public SolverListenerSupport(Solver<Solution_> solver) {
        this.solver = solver;
    }

    public void fireSolvingStarted(DefaultSolverScope<Solution_> solverScope) {
        // Listeners can only be removed/added from the solver thread: no ConcurrentModificationException
        if (eventListenerSet.isEmpty()) {
            return;
        }
        final SolvingStartedEvent<Solution_> event = new SolvingStartedEvent<>(solver);
        for (SolverListener<Solution_> listener : eventListenerSet) {
            listener.solvingStarted(event);
        }
    }

    public void fireSolvingEnded(DefaultSolverScope<Solution_> solverScope) {
        // Listeners can only be removed/added from the solver thread: no ConcurrentModificationException
        if (eventListenerSet.isEmpty()) {
            return;
        }
        long timeMillisSpent = solverScope.getTimeMillisSpent();
        long scoreCalculationCount = solverScope.getScoreCalculationCount();
        SolvingEndedEvent<Solution_> event = new SolvingEndedEvent<>(solver, timeMillisSpent, scoreCalculationCount);
        for (SolverListener<Solution_> listener : eventListenerSet) {
            listener.solvingEnded(event);
        }
    }

}
