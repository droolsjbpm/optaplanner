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

import org.optaplanner.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.heuristic.selector.entity.EntitySelector;
import org.optaplanner.core.impl.heuristic.selector.move.generic.GenericMoveSelector;
import org.optaplanner.core.impl.heuristic.selector.value.ValueSelector;

public class ListChangeMoveSelector<Solution_> extends GenericMoveSelector<Solution_> {

    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final EntitySelector<Solution_> entitySelector;
    private final ValueSelector<Solution_> valueSelector;
    private final boolean randomSelection;

    public ListChangeMoveSelector(
            ListVariableDescriptor<Solution_> listVariableDescriptor,
            EntitySelector<Solution_> entitySelector,
            ValueSelector<Solution_> valueSelector,
            boolean randomSelection) {
        this.listVariableDescriptor = listVariableDescriptor;
        this.entitySelector = entitySelector;
        this.valueSelector = valueSelector;
        this.randomSelection = randomSelection;
        phaseLifecycleSupport.addEventListener(entitySelector);
        phaseLifecycleSupport.addEventListener(valueSelector);
    }

    @Override
    public long getSize() {
        long entityCount = entitySelector.getSize();
        long valueCount = valueSelector.getSize(null);
        return valueCount * (valueCount + entityCount);
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        if (randomSelection) {
            return new RandomListChangeIterator<>(entitySelector, entitySelector, listVariableDescriptor, workingRandom);
        } else {
            return new TmpOriginalListChangeIterator<>(entitySelector, listVariableDescriptor);
        }
    }

    @Override
    public boolean isCountable() {
        return entitySelector.isCountable() && valueSelector.isCountable();
    }

    @Override
    public boolean isNeverEnding() {
        return randomSelection || entitySelector.isNeverEnding() || valueSelector.isNeverEnding();
    }
}
