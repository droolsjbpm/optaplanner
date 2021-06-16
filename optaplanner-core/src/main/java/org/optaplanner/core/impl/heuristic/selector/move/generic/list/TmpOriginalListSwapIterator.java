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
public class TmpOriginalListSwapIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final EntitySelector<Solution_> leftEntitySelector;
    private final Iterator<Object> leftEntityIterator;
    private PrimitiveIterator.OfInt leftIndexIterator;
    private final EntitySelector<Solution_> rightEntitySelector;
    private Iterator<Object> rightEntityIterator;
    private PrimitiveIterator.OfInt rightIndexIterator;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;

    private Object upcomingLeftEntity;
    private Object upcomingRightEntity;
    private int upcomingLeftIndex = 0;

    public TmpOriginalListSwapIterator(
            EntitySelector<Solution_> leftEntitySelector,
            EntitySelector<Solution_> rightEntitySelector,
            ListVariableDescriptor<Solution_> listVariableDescriptor) {
        this.leftEntitySelector = leftEntitySelector;
        this.leftEntityIterator = leftEntitySelector.iterator();
        this.rightEntitySelector = rightEntitySelector;
        this.rightEntityIterator = Collections.emptyIterator();
        this.listVariableDescriptor = listVariableDescriptor;
        rightIndexIterator = IntStream.empty().iterator();
        leftIndexIterator = IntStream.empty().iterator();
    }

    @Override
    protected Move<Solution_> createUpcomingSelection() {
        while (!rightIndexIterator.hasNext()) {
            if (!rightEntityIterator.hasNext()) {
                while (!leftIndexIterator.hasNext()) {
                    if (!leftEntityIterator.hasNext()) {
                        return noUpcomingSelection();
                    }
                    upcomingLeftEntity = leftEntityIterator.next();
                    leftIndexIterator = listIndexIterator(upcomingLeftEntity, IntStream::range);
                }
                upcomingLeftIndex = leftIndexIterator.nextInt();
                rightEntityIterator = rightEntitySelector.iterator();
            }
            upcomingRightEntity = rightEntityIterator.next();
            rightIndexIterator = listIndexIterator(upcomingRightEntity, IntStream::range);
        }

        return new ListSwapMove<>(
                upcomingLeftEntity,
                upcomingLeftIndex,
                upcomingRightEntity,
                rightIndexIterator.nextInt(),
                listVariableDescriptor);
    }

    private PrimitiveIterator.OfInt listIndexIterator(Object entity, BiFunction<Integer, Integer, IntStream> rangeType) {
        return rangeType.apply(0, listVariableDescriptor.getListSize(entity)).iterator();
    }
}
