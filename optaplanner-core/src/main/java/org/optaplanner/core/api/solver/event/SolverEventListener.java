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

import java.util.EventListener;

import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.impl.solver.ProblemFactChange;

/**
 * @deprecated in favor of {@link BestSolutionListener}. Will be removed in 8.0.
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@Deprecated // TODO remove in 8.0
public interface SolverEventListener<Solution_> extends EventListener {

    /**
     * @param event never null
     * @deprecated in favor of {@link BestSolutionListener#bestSolutionChanged(BestSolutionChangedEvent)}
     */
    void bestSolutionChanged(BestSolutionChangedEvent<Solution_> event);

}
