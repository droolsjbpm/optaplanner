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

import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import org.optaplanner.core.impl.heuristic.move.AbstractMove;
import org.optaplanner.core.impl.score.director.InnerScoreDirector;

/**
 * Swaps two elements of a list planning variable.
 * Each element is identified by an entity instance and an index in that entity's list variable.
 * The swap move has two sides called left and right. The element at {@code leftIndex} in {@code leftEntity}'s list variable
 * is replaced by the element at {@code rightIndex} in {@code rightEntity}'s list variable and vice versa.
 * Left and right entity can be the same instance.
 * <p>
 * Flipping the left and right-side entity and index produces an undo move.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class ListSwapMove<Solution_> extends AbstractMove<Solution_> {

    private final Object leftEntity;
    private final int leftIndex;
    private final Object rightEntity;
    private final int rightIndex;

    private final ListVariableDescriptor<Solution_> variableDescriptor;

    public ListSwapMove(
            Object leftEntity, int leftIndex,
            Object rightEntity, int rightIndex,
            ListVariableDescriptor<Solution_> variableDescriptor) {
        this.leftEntity = leftEntity;
        this.leftIndex = leftIndex;
        this.rightEntity = rightEntity;
        this.rightIndex = rightIndex;
        this.variableDescriptor = variableDescriptor;
    }

    @Override
    protected AbstractMove<Solution_> createUndoMove(ScoreDirector<Solution_> scoreDirector) {
        return new ListSwapMove<>(rightEntity, rightIndex, leftEntity, leftIndex, variableDescriptor);
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        InnerScoreDirector<Solution_, ?> innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        Object leftElement = variableDescriptor.getElement(leftEntity, leftIndex);
        Object rightElement = variableDescriptor.getElement(rightEntity, rightIndex);

        innerScoreDirector.beforeVariableChanged(variableDescriptor, leftEntity);
        variableDescriptor.setElement(leftEntity, leftIndex, rightElement);
        innerScoreDirector.afterVariableChanged(variableDescriptor, leftEntity);

        innerScoreDirector.beforeVariableChanged(variableDescriptor, rightEntity);
        variableDescriptor.setElement(rightEntity, rightIndex, leftElement);
        innerScoreDirector.afterVariableChanged(variableDescriptor, rightEntity);
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return !(rightEntity.equals(leftEntity) && leftIndex == rightIndex);
    }

    // ************************************************************************
    // Testing methods
    // ************************************************************************

    public Object getLeftEntity() {
        return leftEntity;
    }

    public int getLeftIndex() {
        return leftIndex;
    }

    public Object getRightEntity() {
        return rightEntity;
    }

    public int getRightIndex() {
        return rightIndex;
    }

    @Override
    public String toString() {
        return String.format("%s[%d]<->%s[%d]", leftEntity, leftIndex, rightEntity, rightIndex);
    }
}
