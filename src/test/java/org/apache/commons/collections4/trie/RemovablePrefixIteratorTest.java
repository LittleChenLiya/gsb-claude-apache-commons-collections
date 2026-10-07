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
package org.apache.commons.collections4.trie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import org.apache.commons.collections4.Trie;
import org.junit.jupiter.api.Test;

/**
 * Tests the cursor contract of {@link Trie#removablePrefixIterator(Object)}: removing the current entry continues iteration at the original-order
 * successor without skipping entries or returning removed ones, external structural modifications fail fast, and {@code remove()} without a preceding
 * {@code next()} is rejected.
 */
class RemovablePrefixIteratorTest {

    /**
     * Creates a three item trie sharing the prefix "And", plus one entry outside the prefix range.
     */
    private PatriciaTrie<Integer> createTrie() {
        final PatriciaTrie<Integer> trie = new PatriciaTrie<>();
        trie.put("Andrea", 1);
        trie.put("Andreas", 2);
        trie.put("Andres", 3);
        trie.put("Bertha", 4); // outside the "And" prefix range
        return trie;
    }

    private List<String> drainKeys(final Iterator<Map.Entry<String, Integer>> it) {
        final List<String> keys = new ArrayList<>();
        it.forEachRemaining(e -> keys.add(e.getKey()));
        return keys;
    }

    @Test
    void testRemoveFirstEntryContinuesWithOriginalSuccessor() {
        final PatriciaTrie<Integer> trie = createTrie();
        final Iterator<Map.Entry<String, Integer>> it = trie.removablePrefixIterator("And");

        assertTrue(it.hasNext());
        final Map.Entry<String, Integer> first = it.next();
        assertEquals("Andrea", first.getKey());
        assertEquals(Integer.valueOf(1), first.getValue());

        it.remove();

        // the original-order successors follow without skipping or resurrecting the removed key
        assertEquals(Arrays.asList("Andreas", "Andres"), drainKeys(it));
        assertFalse(it.hasNext());

        // backing trie contents
        assertFalse(trie.containsKey("Andrea"));
        assertEquals(Integer.valueOf(2), trie.get("Andreas"));
        assertEquals(Integer.valueOf(3), trie.get("Andres"));
        assertEquals(Integer.valueOf(4), trie.get("Bertha"));
        assertEquals(3, trie.size());
    }

    @Test
    void testRemoveMiddleEntryContinuesWithOriginalSuccessor() {
        final PatriciaTrie<Integer> trie = createTrie();
        final Iterator<Map.Entry<String, Integer>> it = trie.removablePrefixIterator("And");

        assertEquals("Andrea", it.next().getKey());
        assertEquals("Andreas", it.next().getKey());
        it.remove();

        // the middle entry is gone, its original successor is still visited exactly once
        assertEquals(Arrays.asList("Andres"), drainKeys(it));
        assertFalse(it.hasNext());

        final TreeMap<String, Integer> expected = new TreeMap<>();
        expected.put("Andrea", 1);
        expected.put("Andres", 3);
        assertEquals(expected, new TreeMap<>(trie.prefixMap("And")));
        assertFalse(trie.containsKey("Andreas"));
        assertEquals(Integer.valueOf(4), trie.get("Bertha"));
        assertEquals(3, trie.size());
    }

    @Test
    void testRemoveLastEntryEndsIteration() {
        final PatriciaTrie<Integer> trie = createTrie();
        final Iterator<Map.Entry<String, Integer>> it = trie.removablePrefixIterator("And");

        assertEquals("Andrea", it.next().getKey());
        assertEquals("Andreas", it.next().getKey());
        assertEquals("Andres", it.next().getKey());
        it.remove();

        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
        assertFalse(trie.containsKey("Andres"));
        assertEquals(3, trie.size());
    }

    @Test
    void testRemoveAllEntriesOfPrefixRange() {
        final PatriciaTrie<Integer> trie = createTrie();
        final Iterator<Map.Entry<String, Integer>> it = trie.removablePrefixIterator("And");

        final List<String> seen = new ArrayList<>();
        while (it.hasNext()) {
            seen.add(it.next().getKey());
            it.remove();
        }

        assertEquals(Arrays.asList("Andrea", "Andreas", "Andres"), seen);
        assertTrue(trie.prefixMap("And").isEmpty());
        assertEquals(Arrays.asList("Bertha"), new ArrayList<>(trie.keySet()));
    }

    @Test
    void testExternalInsertionFailsFast() {
        final PatriciaTrie<Integer> trie = createTrie();
        final Iterator<Map.Entry<String, Integer>> it = trie.removablePrefixIterator("And");

        assertEquals("Andrea", it.next().getKey());
        trie.put("Andrew", 5); // external structural modification

        assertThrows(ConcurrentModificationException.class, it::next);
        assertThrows(ConcurrentModificationException.class, it::remove);
    }

    @Test
    void testExternalRemovalFailsFast() {
        final PatriciaTrie<Integer> trie = createTrie();
        final Iterator<Map.Entry<String, Integer>> it = trie.removablePrefixIterator("And");

        assertEquals("Andrea", it.next().getKey());
        trie.remove("Andres"); // external structural modification

        assertThrows(ConcurrentModificationException.class, it::next);
    }

    @Test
    void testRemoveWithoutNextIsRejected() {
        final PatriciaTrie<Integer> trie = createTrie();
        final Iterator<Map.Entry<String, Integer>> it = trie.removablePrefixIterator("And");

        assertThrows(IllegalStateException.class, it::remove);
        // the iterator is still usable afterwards
        assertEquals("Andrea", it.next().getKey());
    }

    @Test
    void testRepeatedRemoveIsRejected() {
        final PatriciaTrie<Integer> trie = createTrie();
        final Iterator<Map.Entry<String, Integer>> it = trie.removablePrefixIterator("And");

        assertEquals("Andrea", it.next().getKey());
        it.remove();
        assertThrows(IllegalStateException.class, it::remove);

        // iteration continues undisturbed at the original successor
        assertEquals(Arrays.asList("Andreas", "Andres"), drainKeys(it));
        assertFalse(trie.containsKey("Andrea"));
        assertEquals(3, trie.size());
    }

    @Test
    void testEmptyPrefixIteratesWholeTrie() {
        final PatriciaTrie<Integer> trie = createTrie();
        final Iterator<Map.Entry<String, Integer>> it = trie.removablePrefixIterator("");

        assertEquals("Andrea", it.next().getKey());
        it.remove();
        assertEquals(Arrays.asList("Andreas", "Andres", "Bertha"), drainKeys(it));
        assertFalse(trie.containsKey("Andrea"));
        assertEquals(3, trie.size());
    }

    @Test
    void testUnknownPrefixIteratesNothing() {
        final PatriciaTrie<Integer> trie = createTrie();
        final Iterator<Map.Entry<String, Integer>> it = trie.removablePrefixIterator("Xyz");

        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
        assertThrows(IllegalStateException.class, it::remove);
        assertEquals(4, trie.size());
    }

    @Test
    void testReturnedEntriesAreStableSnapshots() {
        final PatriciaTrie<Integer> trie = createTrie();
        final Iterator<Map.Entry<String, Integer>> it = trie.removablePrefixIterator("And");

        final Map.Entry<String, Integer> entry = it.next();
        it.remove();

        // the entry handed out before the removal is not mutated by it
        assertEquals("Andrea", entry.getKey());
        assertEquals(Integer.valueOf(1), entry.getValue());
    }

    /**
     * Regression test for the default {@link Trie#removablePrefixIterator(Object)} implementation, exercised through {@link UnmodifiableTrie}, which
     * does not override it: the default delegates to the prefixMap view iterator.
     */
    @Test
    void testDefaultInterfaceMethodDelegatesToPrefixMap() {
        final Trie<String, Integer> trie = UnmodifiableTrie.unmodifiableTrie(createTrie());
        final Iterator<Map.Entry<String, Integer>> it = trie.removablePrefixIterator("And");

        assertEquals(Arrays.asList("Andrea", "Andreas", "Andres"), drainKeys(it));
        assertFalse(it.hasNext());
    }

    @Test
    void testDefaultInterfaceMethodRemoveIsUnsupportedOnUnmodifiableTrie() {
        final Trie<String, Integer> trie = UnmodifiableTrie.unmodifiableTrie(createTrie());
        final Iterator<Map.Entry<String, Integer>> it = trie.removablePrefixIterator("And");

        assertEquals("Andrea", it.next().getKey());
        assertThrows(UnsupportedOperationException.class, it::remove);
        // the backing trie is untouched
        assertEquals(4, trie.size());
        assertTrue(trie.containsKey("Andrea"));
    }
}
