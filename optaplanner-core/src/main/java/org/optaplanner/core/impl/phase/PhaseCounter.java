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

package org.optaplanner.core.impl.phase;

import java.util.concurrent.atomic.AtomicInteger;

import org.optaplanner.core.impl.phase.event.PhaseLifecycleListener;
import org.optaplanner.core.impl.phase.scope.AbstractPhaseScope;
import org.optaplanner.core.impl.phase.scope.AbstractStepScope;
import org.optaplanner.core.impl.solver.scope.SolverScope;

/**
 * Keeps track of solver phase lifecycle, so that it can provide phase indexes for upcoming phases and phase total when
 * the solver ends.
 *
 * @param <Solution_> generic type of the Solution
 */
public final class PhaseCounter<Solution_> implements PhaseLifecycleListener<Solution_> {

    private final AtomicInteger phaseStartedCounter = new AtomicInteger();
    private final AtomicInteger phaseEndedCounter = new AtomicInteger();

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        this.phaseStartedCounter.incrementAndGet();
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        // No-op.
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        // No-op.
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        this.phaseEndedCounter.incrementAndGet();
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        this.phaseStartedCounter.set(0);
        this.phaseEndedCounter.set(0);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // No-op.
    }

    public int getPhasesStarted() {
        return phaseStartedCounter.get();
    }

    public int getPhasesEnded() {
        return phaseEndedCounter.get();
    }

}
