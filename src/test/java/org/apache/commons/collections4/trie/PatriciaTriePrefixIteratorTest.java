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
import java.util.TreeSet;

import org.apache.commons.collections4.Trie;
import org.junit.jupiter.api.Test;

/**
 * Tests the removable prefix iterator cursor contract of {@link PatriciaTrie#prefixIterator(Object)}.
 * <p>
 * Removing the current element must let the iteration continue with the original-order successor, without skipping an unvisited element or returning the
 * deleted node again, while structural changes made outside the iterator stay fail-fast.
 * </p>
 */
public class PatriciaTriePrefixIteratorTest {

    /** The three small fixtures used for the delete-first/delete-middle acceptance checks. */
    private static final String[][] FIXTURES = {
        { "a", "ab", "ac" },
        { "abc", "abd", "abe", "abf" },
        { "foo", "foobar", "foobaz", "foobam", "food" },
    };

    private static PatriciaTrie<Integer> newTrie(final String... keys) {
        final PatriciaTrie<Integer> trie = new PatriciaTrie<>();
        for (int i = 0; i < keys.length; i++) {
            trie.put(keys[i], i);
        }
        return trie;
    }

    private static String commonPrefix(final String[] keys) {
        String prefix = keys[0];
        for (final String key : keys) {
            int i = 0;
            while (i < prefix.length() && i < key.length() && prefix.charAt(i) == key.charAt(i)) {
                i++;
            }
            prefix = prefix.substring(0, i);
        }
        return prefix;
    }

    private static List<String> removeAtAndDrain(final PatriciaTrie<Integer> trie, final String prefix, final int removeIndex) {
        final List<String> visited = new ArrayList<>();
        final Iterator<Map.Entry<String, Integer>> iterator = trie.prefixIterator(prefix);
        int index = 0;
        while (iterator.hasNext()) {
            final Map.Entry<String, Integer> entry = iterator.next();
            visited.add(entry.getKey());
            if (index++ == removeIndex) {
                iterator.remove();
                // The handed entry is a detached snapshot that still reports the removed key.
                assertEquals(visited.get(visited.size() - 1), entry.getKey());
            }
        }
        return visited;
    }

    /**
     * For each of the three small tries, deleting the first element must visit every element once in original order and leave the successors behind.
     */
    @Test
    public void testDeleteFirstElement() {
        for (final String[] keys : FIXTURES) {
            final String prefix = commonPrefix(keys);
            final PatriciaTrie<Integer> trie = newTrie(keys);

            final List<String> visited = removeAtAndDrain(trie, prefix, 0);

            final List<String> expected = new ArrayList<>(Arrays.asList(keys));
            expected.sort(String::compareTo);
            assertEquals(expected, visited, "no successor may be skipped");
            final List<String> expectedRemaining = expected.subList(1, expected.size());
            assertEquals(new TreeSet<>(expectedRemaining), new TreeSet<>(trie.keySet()), "underlying trie content");
        }
    }

    /**
     * For each of the three small tries, deleting a middle element must visit every element once in original order and leave the rest behind.
     */
    @Test
    public void testDeleteMiddleElement() {
        for (final String[] keys : FIXTURES) {
            final String prefix = commonPrefix(keys);
            for (int removeIndex = 1; removeIndex < keys.length; removeIndex++) {
                final PatriciaTrie<Integer> trie = newTrie(keys);

                final List<String> visited = removeAtAndDrain(trie, prefix, removeIndex);

                final List<String> expected = new ArrayList<>(Arrays.asList(keys));
                expected.sort(String::compareTo);
                assertEquals(expected, visited, "deleting position " + removeIndex + " must not skip successors");
                final String removed = expected.get(removeIndex);
                final TreeSet<String> expectedRemaining = new TreeSet<>(expected);
                expectedRemaining.remove(removed);
                assertEquals(expectedRemaining, new TreeSet<>(trie.keySet()), "deleted node must be gone: " + removed);
            }
        }
    }

    /**
     * Two consecutive removals must still resume at the live original-order successor.
     */
    @Test
    public void testConsecutiveRemovesResumeAtSuccessor() {
        final PatriciaTrie<Integer> trie = newTrie("foo", "foobar", "foobaz", "foobam", "food");
        final Iterator<Map.Entry<String, Integer>> iterator = trie.prefixIterator("foo");

        assertEquals("foo", iterator.next().getKey());
        iterator.remove();
        assertEquals("foobam", iterator.next().getKey());
        iterator.remove();

        final List<String> rest = new ArrayList<>();
        while (iterator.hasNext()) {
            rest.add(iterator.next().getKey());
        }
        assertEquals(Arrays.asList("foobar", "foobaz", "food"), rest);
        assertEquals(new TreeSet<>(Arrays.asList("foobar", "foobaz", "food")), new TreeSet<>(trie.keySet()));
    }

    /**
     * A structural insert performed outside the iterator must fail-fast on the next interaction.
     */
    @Test
    public void testExternalInsertFailsFast() {
        final PatriciaTrie<Integer> trie = newTrie("foo", "foobar", "foobaz", "foobam", "food");
        final Iterator<Map.Entry<String, Integer>> iterator = trie.prefixIterator("foo");
        iterator.next();

        trie.put("fooboo", 9);

        assertThrows(ConcurrentModificationException.class, iterator::hasNext);
        assertThrows(ConcurrentModificationException.class, iterator::next);
        assertThrows(ConcurrentModificationException.class, iterator::remove);
    }

    /**
     * A structural remove performed outside the iterator must fail-fast on the next interaction.
     */
    @Test
    public void testExternalRemoveFailsFast() {
        final PatriciaTrie<Integer> trie = newTrie("foo", "foobar", "foobaz", "foobam", "food");
        final Iterator<Map.Entry<String, Integer>> iterator = trie.prefixIterator("foo");
        iterator.next();

        trie.remove("food");

        assertThrows(ConcurrentModificationException.class, iterator::next);
    }

    /**
     * Calling remove before the first next, or twice in a row, must be rejected with {@link IllegalStateException}.
     */
    @Test
    public void testRemoveWithoutNextAndDuplicateRemoveRejected() {
        final PatriciaTrie<Integer> trie = newTrie("foo", "foobar", "foobaz", "foobam", "food");

        final Iterator<Map.Entry<String, Integer>> fresh = trie.prefixIterator("foo");
        assertThrows(IllegalStateException.class, fresh::remove);

        final Iterator<Map.Entry<String, Integer>> iterator = trie.prefixIterator("foo");
        iterator.next();
        iterator.remove();
        assertThrows(IllegalStateException.class, iterator::remove);

        final Iterator<Map.Entry<String, Integer>> empty = trie.prefixIterator("zzz");
        assertFalse(empty.hasNext());
        assertThrows(NoSuchElementException.class, empty::next);
        assertThrows(IllegalStateException.class, empty::remove);
    }

    /**
     * Iteration and interleaved removal over the empty prefix covers the whole trie.
     */
    @Test
    public void testEmptyPrefixIteratesWholeTrie() {
        final PatriciaTrie<Integer> trie = newTrie("c", "cabd", "d");
        final Iterator<Map.Entry<String, Integer>> iterator = trie.prefixIterator("");

        assertEquals("c", iterator.next().getKey());
        iterator.remove();
        final List<String> rest = new ArrayList<>();
        while (iterator.hasNext()) {
            rest.add(iterator.next().getKey());
        }
        assertEquals(Arrays.asList("cabd", "d"), rest);
        assertEquals(new TreeSet<>(Arrays.asList("cabd", "d")), new TreeSet<>(trie.keySet()));
    }

    /**
     * A prefix matching exactly one entry yields that single entry and nothing after its removal.
     */
    @Test
    public void testSingletonPrefixRemoval() {
        final PatriciaTrie<Integer> trie = newTrie("abc", "abd");
        final Iterator<Map.Entry<String, Integer>> iterator = trie.prefixIterator("abc");

        assertTrue(iterator.hasNext());
        assertEquals("abc", iterator.next().getKey());
        iterator.remove();
        assertFalse(iterator.hasNext());
        assertFalse(trie.containsKey("abc"));
        assertTrue(trie.containsKey("abd"));
    }

    /**
     * The new iterator visits the entries in the exact same order as the existing prefixMap view.
     */
    @Test
    public void testOrderMatchesPrefixMapView() {
        final PatriciaTrie<Integer> trie = newTrie("foo", "foobar", "foobaz", "foobam", "food");

        final List<String> viaView = new ArrayList<>();
        trie.prefixMap("foo").forEach((k, v) -> viaView.add(k));

        final List<String> viaIterator = new ArrayList<>();
        for (final Iterator<Map.Entry<String, Integer>> iterator = trie.prefixIterator("foo"); iterator.hasNext();) {
            viaIterator.add(iterator.next().getKey());
        }
        assertEquals(viaView, viaIterator);
        // The existing prefixMap view must remain unchanged and still contain all entries.
        assertEquals(5, trie.prefixMap("foo").size());
    }

    /**
     * The default interface contract on an unmodifiable trie forbids removal while still allowing traversal.
     */
    @Test
    public void testUnmodifiableTrieIteratorDoesNotRemove() {
        final PatriciaTrie<Integer> trie = newTrie("ab", "ac");
        final Trie<String, Integer> unmodifiable = UnmodifiableTrie.unmodifiableTrie(trie);
        final Iterator<Map.Entry<String, Integer>> iterator = unmodifiable.prefixIterator("a");

        assertEquals("ab", iterator.next().getKey());
        assertThrows(UnsupportedOperationException.class, iterator::remove);
        assertTrue(unmodifiable.containsKey("ab"));
    }

    @Test
    public void testNullPrefixRejected() {
        final PatriciaTrie<Integer> trie = new PatriciaTrie<>();
        assertThrows(NullPointerException.class, () -> trie.prefixIterator(null));
    }
}
