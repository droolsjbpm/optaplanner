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

import java.util.Iterator;
import java.util.stream.StreamSupport;

import org.optaplanner.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.heuristic.selector.entity.EntitySelector;
import org.optaplanner.core.impl.heuristic.selector.move.generic.GenericMoveSelector;

public class ListSwapMoveSelector<Solution_> extends GenericMoveSelector<Solution_> {

    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final EntitySelector<Solution_> leftEntitySelector;
    private final EntitySelector<Solution_> rightEntitySelector;
    private final boolean randomSelection;

    public ListSwapMoveSelector(
            ListVariableDescriptor<Solution_> listVariableDescriptor,
            EntitySelector<Solution_> leftEntitySelector,
            EntitySelector<Solution_> rightEntitySelector,
            boolean randomSelection) {
        this.listVariableDescriptor = listVariableDescriptor;
        this.leftEntitySelector = leftEntitySelector;
        this.rightEntitySelector = rightEntitySelector;
        this.randomSelection = randomSelection;
        phaseLifecycleSupport.addEventListener(leftEntitySelector);
        if (leftEntitySelector != rightEntitySelector) {
            phaseLifecycleSupport.addEventListener(rightEntitySelector);
        }
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        if (randomSelection) {
            return new RandomListSwapIterator<>(leftEntitySelector, rightEntitySelector, listVariableDescriptor, workingRandom);
        } else {
            return new TmpOriginalListSwapIterator<>(leftEntitySelector, rightEntitySelector, listVariableDescriptor);
        }
    }

    @Override
    public boolean isCountable() {
        return leftEntitySelector.isCountable() && rightEntitySelector.isCountable();
    }

    @Override
    public boolean isNeverEnding() {
        return randomSelection || leftEntitySelector.isNeverEnding() || rightEntitySelector.isNeverEnding();
    }

    @Override
    public long getSize() {
        Iterable<?> i = leftEntitySelector::endingIterator;
        int valueCount = StreamSupport.stream(i.spliterator(), false)
                .mapToInt(listVariableDescriptor::getListSize)
                .sum();
        return (long) valueCount * valueCount;
    }
}
