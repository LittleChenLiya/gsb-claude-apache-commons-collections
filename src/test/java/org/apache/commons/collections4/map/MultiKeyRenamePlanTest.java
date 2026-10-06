/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.collections4.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.junit.jupiter.api.Test;

/**
 * JUnit tests for {@link MultiKeyRenamePlan}.
 */
public class MultiKeyRenamePlanTest {

    private static final Integer I1 = Integer.valueOf(1);
    private static final Integer I2 = Integer.valueOf(2);
    private static final Integer I3 = Integer.valueOf(3);
    private static final Integer I4 = Integer.valueOf(4);
    private static final Integer I5 = Integer.valueOf(5);
    private static final Integer I6 = Integer.valueOf(6);

    /**
     * Tests that two keys can be swapped and values end up in the correct positions.
     */
    @Test
    void testSwapTwoKeys() {
        final MultiKeyMap<Integer, String> map = new MultiKeyMap<>();
        final MultiKey<Integer> keyA = new MultiKey<>(I1, I2);
        final MultiKey<Integer> keyB = new MultiKey<>(I3, I4);
        map.put(keyA, "valueA");
        map.put(keyB, "valueB");

        final MultiKeyRenamePlan<Integer> plan = new MultiKeyRenamePlan<>();
        plan.rename(keyA, keyB);
        plan.rename(keyB, keyA);
        plan.applyTo(map);

        // After the swap, keyA should have valueB and keyB should have valueA.
        assertEquals("valueB", map.get(keyA));
        assertEquals("valueA", map.get(keyB));
        assertEquals(2, map.size());
    }

    /**
     * Tests that three keys can be rotated and values end up in the correct positions.
     */
    @Test
    void testSwapThreeKeys() {
        final MultiKeyMap<Integer, String> map = new MultiKeyMap<>();
        final MultiKey<Integer> keyA = new MultiKey<>(I1, I2);
        final MultiKey<Integer> keyB = new MultiKey<>(I3, I4);
        final MultiKey<Integer> keyC = new MultiKey<>(I5, I6);
        map.put(keyA, "valueA");
        map.put(keyB, "valueB");
        map.put(keyC, "valueC");

        // Rotate: A -> B, B -> C, C -> A
        final MultiKeyRenamePlan<Integer> plan = new MultiKeyRenamePlan<>();
        plan.rename(keyA, keyB);
        plan.rename(keyB, keyC);
        plan.rename(keyC, keyA);
        plan.applyTo(map);

        // After the rotation, keyA should have valueC, keyB should have valueA, keyC should have valueB.
        assertEquals("valueC", map.get(keyA));
        assertEquals("valueA", map.get(keyB));
        assertEquals("valueB", map.get(keyC));
        assertEquals(3, map.size());
    }

    /**
     * Tests that a target conflict is rejected and the original map is unchanged.
     */
    @Test
    void testConflictRejectedMapUnchanged() {
        final MultiKeyMap<Integer, String> map = new MultiKeyMap<>();
        final MultiKey<Integer> keyA = new MultiKey<>(I1, I2);
        final MultiKey<Integer> keyB = new MultiKey<>(I3, I4);
        final MultiKey<Integer> keyX = new MultiKey<>(I5, I6);
        map.put(keyA, "valueA");
        map.put(keyB, "valueB");

        // Both A and B rename to the same target X.
        final MultiKeyRenamePlan<Integer> plan = new MultiKeyRenamePlan<>();
        plan.rename(keyA, keyX);
        plan.rename(keyB, keyX);

        assertThrows(IllegalArgumentException.class, () -> plan.applyTo(map));

        // The original map should be unchanged.
        assertEquals(2, map.size());
        assertEquals("valueA", map.get(keyA));
        assertEquals("valueB", map.get(keyB));
        assertTrue(map.containsKey(keyA));
        assertTrue(map.containsKey(keyB));
    }

    /**
     * Tests that a source key not found in the map is rejected and the original map is unchanged.
     */
    @Test
    void testSourceKeyNotFoundRejectedMapUnchanged() {
        final MultiKeyMap<Integer, String> map = new MultiKeyMap<>();
        final MultiKey<Integer> keyA = new MultiKey<>(I1, I2);
        final MultiKey<Integer> keyB = new MultiKey<>(I3, I4);
        final MultiKey<Integer> keyX = new MultiKey<>(I5, I6);
        map.put(keyA, "valueA");

        final MultiKeyRenamePlan<Integer> plan = new MultiKeyRenamePlan<>();
        plan.rename(keyA, keyX);
        plan.rename(keyB, keyX);

        assertThrows(IllegalArgumentException.class, () -> plan.applyTo(map));

        // The original map should be unchanged.
        assertEquals(1, map.size());
        assertEquals("valueA", map.get(keyA));
    }

    /**
     * Tests that original value references are preserved (not cloned).
     */
    @Test
    void testValueReferencesPreserved() {
        final MultiKeyMap<Integer, Object> map = new MultiKeyMap<>();
        final MultiKey<Integer> keyA = new MultiKey<>(I1, I2);
        final MultiKey<Integer> keyB = new MultiKey<>(I3, I4);
        final Object valueA = new Object();
        final Object valueB = new Object();
        map.put(keyA, valueA);
        map.put(keyB, valueB);

        final MultiKeyRenamePlan<Integer> plan = new MultiKeyRenamePlan<>();
        plan.rename(keyA, keyB);
        plan.rename(keyB, keyA);
        plan.applyTo(map);

        // The same value references should be preserved.
        assertSame(valueB, map.get(keyA));
        assertSame(valueA, map.get(keyB));
    }

    /**
     * Tests that a simple rename (not a swap) works correctly.
     */
    @Test
    void testSimpleRename() {
        final MultiKeyMap<Integer, String> map = new MultiKeyMap<>();
        final MultiKey<Integer> keyA = new MultiKey<>(I1, I2);
        final MultiKey<Integer> keyB = new MultiKey<>(I3, I4);
        map.put(keyA, "valueA");

        final MultiKeyRenamePlan<Integer> plan = new MultiKeyRenamePlan<>();
        plan.rename(keyA, keyB);
        plan.applyTo(map);

        assertEquals("valueA", map.get(keyB));
        assertEquals(1, map.size());
    }

    /**
     * Tests that an empty plan leaves the map unchanged.
     */
    @Test
    void testEmptyPlan() {
        final MultiKeyMap<Integer, String> map = new MultiKeyMap<>();
        final MultiKey<Integer> keyA = new MultiKey<>(I1, I2);
        map.put(keyA, "valueA");

        final MultiKeyRenamePlan<Integer> plan = new MultiKeyRenamePlan<>();
        plan.applyTo(map);

        assertEquals(1, map.size());
        assertEquals("valueA", map.get(keyA));
    }

}
