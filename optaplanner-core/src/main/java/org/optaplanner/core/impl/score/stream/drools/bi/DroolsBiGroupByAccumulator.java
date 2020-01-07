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

package org.optaplanner.core.impl.score.stream.drools.bi;

import java.io.Serializable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.optaplanner.core.api.function.TriFunction;
import org.optaplanner.core.api.score.stream.bi.BiConstraintCollector;
import org.optaplanner.core.impl.score.stream.drools.common.BiTuple;

final class DroolsBiGroupByAccumulator<A, B, ResultContainer, NewA, NewB> implements Serializable {

    // Containers may be identical in type and contents, yet they should still not count as the same container.
    private final Map<ResultContainer, Long> containersInUse = new IdentityHashMap<>(0);
    // LinkedHashMap to maintain a consistent iteration order of resulting pairs.
    private final Map<NewA, ResultContainer> containers = new LinkedHashMap<>(0);
    private final BiFunction<A, B, NewA> groupKeyMapping;
    private final Supplier<ResultContainer> supplier;
    private final TriFunction<ResultContainer, A, B, Runnable> accumulator;
    private final Function<ResultContainer, NewB> finisher;
    // Transient as Spotbugs complains otherwise ("non-transient non-serializable instance field").
    // It doesn't make sense to serialize this anyway, as it is recreated every time.
    private final transient Set<BiTuple<NewA, NewB>> result = new LinkedHashSet<>(0);

    public DroolsBiGroupByAccumulator(BiFunction<A, B, NewA> groupKeyMapping,
            BiConstraintCollector<A, B, ResultContainer, NewB> collector) {
        this.groupKeyMapping = groupKeyMapping;
        this.supplier = collector.supplier();
        this.accumulator = collector.accumulator();
        this.finisher = collector.finisher();
    }

    private static Long increment(Long count) {
        return count == null ? 1L : count + 1L;
    }

    private static Long decrement(Long count) {
        return count == 1L ? null : count - 1L;
    }

    public Runnable accumulate(A firstKey, B secondKey) {
        NewA key = groupKeyMapping.apply(firstKey, secondKey);
        ResultContainer container = containers.computeIfAbsent(key, __ -> supplier.get());
        Runnable undo = accumulator.apply(container, firstKey, secondKey);
        containersInUse.compute(container, (__, count) -> increment(count)); // Increment use counter.
        return () -> {
            undo.run();
            // Decrement use counter. If 0, container is ignored during finishing. Removes empty groups from results.
            Long currentCount = containersInUse.compute(container, (__, count) -> decrement(count));
            if (currentCount == null) {
                containers.remove(key);
            }
        };
    }

    public Set<BiTuple<NewA, NewB>> finish() {
        result.clear();
        for (Map.Entry<NewA, ResultContainer> entry: containers.entrySet()) {
            ResultContainer container = entry.getValue();
            result.add(new BiTuple<>(entry.getKey(), finisher.apply(container)));
        }
        return result;
    }
}
