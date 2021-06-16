/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.core.impl.heuristic.selector.move.generic.list;

import java.util.Collections;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import org.optaplanner.core.impl.heuristic.selector.entity.EntitySelector;

/**
 * Iterates over entities and elements of their list planning variables. This approach allows to generate moves
 * without using anchorVariableSupply and indexSupply.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class TmpOriginalListChangeIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final EntitySelector<Solution_> entitySelector;
    private final Iterator<Object> fromEntityIterator;
    private PrimitiveIterator.OfInt fromIndexIterator;
    private Iterator<Object> toEntityIterator;
    private PrimitiveIterator.OfInt toIndexIterator;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;

    private Object upcomingFromEntity;
    private Object upcomingToEntity;
    private int upcomingFromIndex = 0;

    public TmpOriginalListChangeIterator(
            EntitySelector<Solution_> entitySelector,
            ListVariableDescriptor<Solution_> listVariableDescriptor) {
        this.entitySelector = entitySelector;
        this.fromEntityIterator = entitySelector.iterator();
        this.toEntityIterator = Collections.emptyIterator();
        this.listVariableDescriptor = listVariableDescriptor;
        toIndexIterator = IntStream.empty().iterator();
        fromIndexIterator = IntStream.empty().iterator();
    }

    @Override
    protected Move<Solution_> createUpcomingSelection() {
        if (!toIndexIterator.hasNext()) {
            if (!toEntityIterator.hasNext()) {
                while (!fromIndexIterator.hasNext()) {
                    if (!fromEntityIterator.hasNext()) {
                        return noUpcomingSelection();
                    }
                    upcomingFromEntity = fromEntityIterator.next();
                    fromIndexIterator = listIndexIterator(upcomingFromEntity, IntStream::range);
                }
                upcomingFromIndex = fromIndexIterator.nextInt();
                toEntityIterator = entitySelector.iterator();
            }
            upcomingToEntity = toEntityIterator.next();
            toIndexIterator = listIndexIterator(upcomingToEntity, IntStream::rangeClosed);
        }

        return new ListChangeMove<>(
                upcomingFromEntity,
                upcomingFromIndex,
                upcomingToEntity,
                toIndexIterator.nextInt(),
                listVariableDescriptor);
    }

    private PrimitiveIterator.OfInt listIndexIterator(Object entity, BiFunction<Integer, Integer, IntStream> rangeType) {
        return rangeType.apply(0, listVariableDescriptor.getListSize(entity)).iterator();
    }
}
