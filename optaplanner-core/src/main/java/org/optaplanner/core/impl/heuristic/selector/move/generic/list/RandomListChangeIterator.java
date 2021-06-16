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
import java.util.Random;

import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import org.optaplanner.core.impl.heuristic.selector.entity.EntitySelector;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class RandomListChangeIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final EntitySelector<Solution_> fromEntitySelector;
    private final EntitySelector<Solution_> toEntitySelector;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final Random workingRandom;

    private Iterator<Object> fromEntityIterator;
    private Iterator<Object> toEntityIterator;

    public RandomListChangeIterator(
            EntitySelector<Solution_> fromEntitySelector,
            EntitySelector<Solution_> toEntitySelector,
            ListVariableDescriptor<Solution_> listVariableDescriptor,
            Random workingRandom) {
        this.fromEntitySelector = fromEntitySelector;
        this.fromEntityIterator = fromEntitySelector.iterator();
        this.toEntitySelector = toEntitySelector;
        this.toEntityIterator = toEntitySelector.iterator();
        this.workingRandom = workingRandom;
        this.listVariableDescriptor = listVariableDescriptor;
    }

    @Override
    protected Move<Solution_> createUpcomingSelection() {
        Object fromEntity = null;
        while (fromEntity == null || listVariableDescriptor.getListSize(fromEntity) == 0) {
            if (!fromEntityIterator.hasNext()) {
                fromEntityIterator = fromEntitySelector.iterator();
                if (!fromEntityIterator.hasNext()) {
                    return noUpcomingSelection();
                }
            }
            fromEntity = fromEntityIterator.next();
        }

        if (!toEntityIterator.hasNext()) {
            toEntityIterator = toEntitySelector.iterator();
            if (!toEntityIterator.hasNext()) {
                return noUpcomingSelection();
            }
        }
        Object toEntity = toEntityIterator.next();

        return new ListChangeMove<>(
                fromEntity,
                randomFromIndex(fromEntity),
                toEntity,
                randomToIndex(toEntity),
                listVariableDescriptor);
    }

    private int randomFromIndex(Object entity) {
        return randomIndex(entity, 0);
    }

    private int randomToIndex(Object entity) {
        return randomIndex(entity, 1);
    }

    private int randomIndex(Object entity, int zeroOrOne) {
        return workingRandom.nextInt(listVariableDescriptor.getListSize(entity) + zeroOrOne);
    }
}
