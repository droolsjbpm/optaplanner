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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.optaplanner.core.impl.testdata.util.PlannerAssert.assertAllCodesOfMoveSelector;
import static org.optaplanner.core.impl.testdata.util.PlannerAssert.assertCodesOfNeverEndingMoveSelector;

import java.util.Random;

import org.junit.jupiter.api.Test;
import org.optaplanner.core.impl.heuristic.selector.SelectorTestUtils;
import org.optaplanner.core.impl.heuristic.selector.entity.EntitySelector;
import org.optaplanner.core.impl.heuristic.selector.value.ValueSelector;
import org.optaplanner.core.impl.solver.scope.SolverScope;
import org.optaplanner.core.impl.testdata.domain.TestdataValue;
import org.optaplanner.core.impl.testdata.domain.list.TestdataListEntity;
import org.optaplanner.core.impl.testdata.domain.list.TestdataListSolution;

class ListChangeMoveSelectorTest {

    @Test
    void original() {
        TestdataValue v1 = new TestdataValue("1");
        TestdataValue v2 = new TestdataValue("2");
        TestdataValue v3 = new TestdataValue("3");
        EntitySelector<TestdataListSolution> entitySelector = SelectorTestUtils.mockEntitySelector(
                TestdataListEntity.class,
                new TestdataListEntity("A", v1, v2),
                new TestdataListEntity("B"),
                new TestdataListEntity("C", v3));
        ValueSelector<TestdataListSolution> valueSelector = SelectorTestUtils.mockValueSelector(
                TestdataListEntity.class, "valueList", v1, v2, v3);

        ListChangeMoveSelector<TestdataListSolution> moveSelector = new ListChangeMoveSelector<>(
                TestdataListEntity.buildVariableDescriptorForValueList(),
                entitySelector,
                valueSelector,
                false);

        assertAllCodesOfMoveSelector(moveSelector,
                // moving A[0]
                "A[0]->A[0]", // undoable
                "A[0]->A[1]",
                "A[0]->A[2]", // undoable
                "A[0]->B[0]",
                "A[0]->C[0]",
                "A[0]->C[1]",
                // moving A[1]
                "A[1]->A[0]",
                "A[1]->A[1]", // undoable
                "A[1]->A[2]", // undoable
                "A[1]->B[0]",
                "A[1]->C[0]",
                "A[1]->C[1]",
                // B has no elements, so no moves
                // moving C[0]
                "C[0]->A[0]",
                "C[0]->A[1]",
                "C[0]->A[2]",
                "C[0]->B[0]",
                "C[0]->C[0]", // undoable
                "C[0]->C[1]" // undoable
        );
    }

    @Test
    void random() {
        TestdataValue v1 = new TestdataValue("1");
        TestdataValue v2 = new TestdataValue("2");
        TestdataValue v3 = new TestdataValue("3");
        EntitySelector<TestdataListSolution> entitySelector = SelectorTestUtils.mockEntitySelector(
                TestdataListEntity.class,
                new TestdataListEntity("A", v1, v2),
                new TestdataListEntity("B"),
                new TestdataListEntity("C", v3));
        ValueSelector<TestdataListSolution> valueSelector = SelectorTestUtils.mockValueSelector(
                TestdataListEntity.class, "valueList", v1, v2, v3);

        ListChangeMoveSelector<TestdataListSolution> moveSelector = new ListChangeMoveSelector<>(
                TestdataListEntity.buildVariableDescriptorForValueList(),
                entitySelector,
                valueSelector,
                true);

        Random random = mock(Random.class);
        when(random.nextInt(1)).thenReturn(0);
        when(random.nextInt(2)).thenReturn(1);
        when(random.nextInt(3)).thenReturn(2);

        SolverScope<TestdataListSolution> solverScope = mock(SolverScope.class);
        when(solverScope.getWorkingRandom()).thenReturn(random);
        moveSelector.solvingStarted(solverScope);

        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "A[1]->A[2]",
                "C[0]->B[0]",
                "A[1]->C[1]",
                "C[0]->A[2]",
                "A[1]->B[0]",
                "C[0]->C[1]",
                // 2nd round
                "A[1]->A[2]",
                "C[0]->B[0]",
                "A[1]->C[1]",
                "C[0]->A[2]",
                "A[1]->B[0]",
                "C[0]->C[1]");
    }
}
