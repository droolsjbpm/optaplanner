/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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
package org.optaplanner.core.impl.score.director.drools.reproducer;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.optaplanner.core.impl.score.director.drools.reproducer.operation.KieSessionOperation;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class RemoveRandomItemMutatorTest {

    private static final int LIST_SIZE = 10;
    private ArrayList<KieSessionOperation> list = new ArrayList<>();

    @Before
    public void setUp() {
        for (int i = 0; i < LIST_SIZE; i++) {
            list.add(mock(KieSessionOperation.class));
        }
    }

    @Test
    public void testRemoveAll() {
        RemoveRandomItemMutator m = new RemoveRandomItemMutator(list);
        ArrayList<KieSessionOperation> removed = new ArrayList<>();
        for (int i = 0; i < LIST_SIZE; i++) {
            assertTrue(m.canMutate());
            m.mutate();
            removed.add(m.getRemovedItem());
            assertEquals(LIST_SIZE - i - 1, m.getResult().size());
        }
        assertFalse(m.canMutate());

        for (int i = 0; i < LIST_SIZE; i++) {
            assertTrue(removed.contains(list.get(i)));
        }
    }

    @Test
    public void testRevert() {
        RemoveRandomItemMutator m = new RemoveRandomItemMutator(list);
        m.mutate();
        KieSessionOperation removedItem = m.getRemovedItem();
        m.revert();
        assertTrue(m.getResult().contains(removedItem));
        assertEquals(LIST_SIZE, m.getResult().size());
    }

    @Test
    public void testImpossibleMutation() {
        RemoveRandomItemMutator m = new RemoveRandomItemMutator(list);
        ArrayList<KieSessionOperation> removed = new ArrayList<>();
        for (int i = 0; i < LIST_SIZE; i++) {
            assertTrue(m.canMutate());
            m.mutate();
            removed.add(m.getRemovedItem());
            m.revert();
        }
        assertFalse(m.canMutate());

        for (int i = 0; i < LIST_SIZE; i++) {
            assertTrue(removed.contains(list.get(i)));
        }
    }
}
