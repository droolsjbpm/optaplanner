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

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

public class MemoryUseStatistic implements Consumer<Solver> {
    @Override
    public void accept(Solver solver) {
        Metrics.gauge(SolverMetric.MEMORY_USE.getMeterId(), Tags.empty(), null,
                obj -> Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
    }
}
