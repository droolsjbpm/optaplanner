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

package org.optaplanner.core.api.solver.event;

import java.util.EventObject;

import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.score.FeasibilityScore;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.impl.solver.ProblemFactChange;

/**
 * Delivered when the solver ends, including before every {@link Solver#addProblemFactChange(ProblemFactChange) restart}.
 * Delivered in the solver thread (which is the thread that calls {@link Solver#solve}).
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SolvingEndedEvent<Solution_> extends EventObject {

    private final Solver<Solution_> solver;

    /**
     * @param solver never null
     */
    public SolvingEndedEvent(Solver<Solution_> solver) {
        super(solver);
        this.solver = solver;
    }

}
