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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link MultiKeyMapRenamePlan}.
 */
public class MultiKeyMapRenamePlanTest {

    private static MultiKey<String> key(final String... keys) {
        return new MultiKey<>(keys);
    }

    @Test
    public void testSwapTwoKeys() {
        final MultiKeyMap<String, Object> map = new MultiKeyMap<>();
        final Object value1 = new Object();
        final Object value2 = new Object();
        map.put("A", "B", value1);
        map.put("C", "D", value2);

        final boolean committed = MultiKeyMapRenamePlan.on(map)
                .swap(key("A", "B"), key("C", "D"))
                .commit();

        assertTrue(committed);
        assertEquals(2, map.size());
        // both values sit at the swapped positions, reachable via the multi-key API
        assertSame(value2, map.get("A", "B"));
        assertSame(value1, map.get("C", "D"));
        // and via the normal Map entry points, keeping the original value references
        assertSame(value2, map.get(key("A", "B")));
        assertSame(value1, map.get(key("C", "D")));
        assertTrue(map.containsKey(key("A", "B")));
        assertTrue(map.containsKey(key("C", "D")));
    }

    @Test
    public void testRenameToFreshKey() {
        final MultiKeyMap<String, Object> map = new MultiKeyMap<>();
        final Object value = new Object();
        map.put("A", "B", value);

        final boolean committed = MultiKeyMapRenamePlan.on(map)
                .rename(key("A", "B"), key("X", "Y"))
                .commit();

        assertTrue(committed);
        assertEquals(1, map.size());
        assertNull(map.get("A", "B"));
        assertSame(value, map.get("X", "Y"));
    }

    @Test
    public void testTargetConflictWithExistingKeyIsRejected() {
        final MultiKeyMap<String, Object> map = new MultiKeyMap<>();
        final Object value1 = new Object();
        final Object value2 = new Object();
        map.put("A", "B", value1);
        map.put("C", "D", value2);

        // ("C", "D") already exists and is not itself renamed away
        final boolean committed = MultiKeyMapRenamePlan.on(map)
                .rename(key("A", "B"), key("C", "D"))
                .commit();

        assertFalse(committed);
        // the original map is unchanged
        assertEquals(2, map.size());
        assertSame(value1, map.get("A", "B"));
        assertSame(value2, map.get("C", "D"));
    }

    @Test
    public void testDuplicateTargetKeysAreRejected() {
        final MultiKeyMap<String, Object> map = new MultiKeyMap<>();
        final Object value1 = new Object();
        final Object value2 = new Object();
        map.put("A", "B", value1);
        map.put("C", "D", value2);

        final boolean committed = MultiKeyMapRenamePlan.on(map)
                .rename(key("A", "B"), key("X", "Y"))
                .rename(key("C", "D"), key("X", "Y"))
                .commit();

        assertFalse(committed);
        assertEquals(2, map.size());
        assertSame(value1, map.get("A", "B"));
        assertSame(value2, map.get("C", "D"));
    }

    @Test
    public void testMissingSourceKeyIsRejected() {
        final MultiKeyMap<String, Object> map = new MultiKeyMap<>();
        final Object value = new Object();
        map.put("A", "B", value);

        final boolean committed = MultiKeyMapRenamePlan.on(map)
                .rename(key("Z", "Z"), key("X", "Y"))
                .commit();

        assertFalse(committed);
        assertEquals(1, map.size());
        assertSame(value, map.get("A", "B"));
        assertNull(map.get("X", "Y"));
    }

    @Test
    public void testEmptyPlanCommitsWithoutChanges() {
        final MultiKeyMap<String, Object> map = new MultiKeyMap<>();
        final Object value = new Object();
        map.put("A", "B", value);

        assertTrue(MultiKeyMapRenamePlan.on(map).commit());
        assertEquals(1, map.size());
        assertSame(value, map.get("A", "B"));
    }

    @Test
    public void testThreeKeyRename() {
        final MultiKeyMap<String, Object> map = new MultiKeyMap<>();
        final Object value1 = new Object();
        final Object value2 = new Object();
        map.put("A", "B", "C", value1);
        map.put("D", "E", "F", value2);

        final boolean committed = MultiKeyMapRenamePlan.on(map)
                .rename(key("A", "B", "C"), key("X", "Y", "Z"))
                .commit();

        assertTrue(committed);
        assertEquals(2, map.size());
        assertNull(map.get("A", "B", "C"));
        assertSame(value1, map.get("X", "Y", "Z"));
        assertSame(value2, map.get("D", "E", "F"));
    }

    @Test
    public void testThreeKeySwap() {
        final MultiKeyMap<String, Object> map = new MultiKeyMap<>();
        final Object value1 = new Object();
        final Object value2 = new Object();
        final Object value3 = new Object();
        map.put("A", "B", "C", value1);
        map.put("D", "E", "F", value2);
        map.put("G", "H", "I", value3);

        final boolean committed = MultiKeyMapRenamePlan.on(map)
                .swap(key("A", "B", "C"), key("D", "E", "F"))
                .commit();

        assertTrue(committed);
        assertEquals(3, map.size());
        assertSame(value2, map.get("A", "B", "C"));
        assertSame(value1, map.get("D", "E", "F"));
        // the untouched third entry keeps its value reference
        assertSame(value3, map.get("G", "H", "I"));
    }

    @Test
    public void testThreeKeyConflictLeavesMapUnchanged() {
        final MultiKeyMap<String, Object> map = new MultiKeyMap<>();
        final Object value1 = new Object();
        final Object value2 = new Object();
        final Object value3 = new Object();
        map.put("A", "B", "C", value1);
        map.put("D", "E", "F", value2);
        map.put("G", "H", "I", value3);

        // the second rename targets the untouched key ("G", "H", "I")
        final boolean committed = MultiKeyMapRenamePlan.on(map)
                .rename(key("A", "B", "C"), key("X", "Y", "Z"))
                .rename(key("D", "E", "F"), key("G", "H", "I"))
                .commit();

        assertFalse(committed);
        assertEquals(3, map.size());
        assertSame(value1, map.get("A", "B", "C"));
        assertSame(value2, map.get("D", "E", "F"));
        assertSame(value3, map.get("G", "H", "I"));
        assertNull(map.get("X", "Y", "Z"));
    }
}
