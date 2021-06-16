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
public class RandomListSwapIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final EntitySelector<Solution_> leftEntitySelector;
    private final EntitySelector<Solution_> rightEntitySelector;

    private final ListVariableDescriptor<Solution_> variableDescriptor;
    private final Random workingRandom;

    private Iterator<Object> leftSubSelectionIterator;
    private Iterator<Object> rightSubSelectionIterator;

    public RandomListSwapIterator(
            EntitySelector<Solution_> leftEntitySelector,
            EntitySelector<Solution_> rightEntitySelector,
            ListVariableDescriptor<Solution_> variableDescriptor,
            Random workingRandom) {
        this.leftEntitySelector = leftEntitySelector;
        this.rightEntitySelector = rightEntitySelector;

        this.leftSubSelectionIterator = this.leftEntitySelector.iterator();
        this.rightSubSelectionIterator = this.rightEntitySelector.iterator();

        this.variableDescriptor = variableDescriptor;
        this.workingRandom = workingRandom;
    }

    @Override
    protected Move<Solution_> createUpcomingSelection() {
        Object leftSubSelection = null;
        while (leftSubSelection == null || variableDescriptor.getListSize(leftSubSelection) == 0) {
            if (!leftSubSelectionIterator.hasNext()) {
                leftSubSelectionIterator = leftEntitySelector.iterator();
                if (!leftSubSelectionIterator.hasNext()) {
                    return noUpcomingSelection();
                }
            }
            leftSubSelection = leftSubSelectionIterator.next();
        }
        Object rightSubSelection = null;
        while (rightSubSelection == null || variableDescriptor.getListSize(rightSubSelection) == 0) {
            if (!rightSubSelectionIterator.hasNext()) {
                rightSubSelectionIterator = rightEntitySelector.iterator();
                if (!rightSubSelectionIterator.hasNext()) {
                    return noUpcomingSelection();
                }
            }
            rightSubSelection = rightSubSelectionIterator.next();
        }
        return new ListSwapMove<>(
                leftSubSelection,
                randomIndex(leftSubSelection),
                rightSubSelection,
                randomIndex(rightSubSelection),
                variableDescriptor);
    }

    private int randomIndex(Object entity) {
        return workingRandom.nextInt(variableDescriptor.getListSize(entity));
    }
}
