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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.SortedMap;

import org.junit.jupiter.api.Test;

/**
 * Tests the checked {@link PrefixMapView#putChecked(Object, Object)} entry point of the prefix view returned by
 * {@link AbstractPatriciaTrie#prefixMapView(Object)}.
 * <p>
 * The fixture uses a custom {@link KeyAnalyzer} that analyzes the characters of a string in <em>reverse</em> order. Under this analyzer the bit prefix of
 * {@code "ab"} is shared by keys whose bits start with the bits of {@code 'b'} then {@code 'a'} (for example {@code "cab"}), while a key like {@code "abc"}
 * - whose string form starts with {@code "ab"} - does <em>not</em> belong to the view. This proves that view membership is decided by the real bit prefix
 * and never by a string-level prefix.
 * </p>
 */
class PrefixMapViewTest {

    /**
     * A {@link KeyAnalyzer} for strings that reads characters in reverse order, 16 bits per character, most significant bit first. Two keys share a bit
     * prefix exactly when they share a string <em>suffix</em>.
     */
    private static final class ReverseStringKeyAnalyzer extends KeyAnalyzer<String> {

        private static final long serialVersionUID = 1L;

        private static final int BITS = Character.SIZE;

        @Override
        public int bitIndex(final String key, final int offsetInBits, final int lengthInBits,
                            final String other, final int otherOffsetInBits, final int otherLengthInBits) {
            if (lengthInBits == 0 && (other == null || otherLengthInBits == 0)) {
                return NULL_BIT_KEY;
            }
            final int maxLength = Math.max(lengthInBits, otherLengthInBits);
            for (int i = 0; i < maxLength; i++) {
                final boolean bitA = i < lengthInBits && isBitSet(key, offsetInBits + i, lengthInBits(key));
                final boolean bitB = other != null && i < otherLengthInBits && isBitSet(other, otherOffsetInBits + i, lengthInBits(other));
                if (bitA != bitB) {
                    return i;
                }
            }
            return EQUAL_BIT_KEY;
        }

        @Override
        public int bitsPerElement() {
            return BITS;
        }

        @Override
        public boolean isBitSet(final String key, final int bitIndex, final int lengthInBits) {
            if (key == null || bitIndex < 0 || bitIndex >= lengthInBits) {
                return false;
            }
            final int charIndex = key.length() - 1 - bitIndex / BITS;
            final int bit = BITS - 1 - bitIndex % BITS;
            return (key.charAt(charIndex) & 1 << bit) != 0;
        }

        @Override
        public boolean isPrefix(final String prefix, final int offsetInBits, final int lengthInBits, final String key) {
            if (lengthInBits > lengthInBits(key)) {
                return false;
            }
            for (int i = 0; i < lengthInBits; i++) {
                if (isBitSet(prefix, offsetInBits + i, lengthInBits(prefix)) != isBitSet(key, i, lengthInBits(key))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int lengthInBits(final String key) {
            return key != null ? key.length() * BITS : 0;
        }
    }

    /** A trie on string keys analyzed in reverse character order. */
    private static final class ReverseStringTrie<V> extends AbstractPatriciaTrie<String, V> {

        private static final long serialVersionUID = 1L;

        ReverseStringTrie() {
            super(new ReverseStringKeyAnalyzer());
        }
    }

    @Test
    void acceptsKeyWhoseBitsMatchButStringPrefixDoesNot() {
        final ReverseStringTrie<String> trie = new ReverseStringTrie<>();
        final PrefixMapView<String, String> view = trie.prefixMapView("ab");

        // "cab" does not start with "ab" as a string, but its reversed bits start with the bits of "ab".
        assertNull(view.putChecked("cab", "1"));

        // Written through the regular update path: readable from the backing trie and from the view.
        assertEquals("1", trie.get("cab"));
        assertTrue(trie.containsKey("cab"));
        assertTrue(view.containsKey("cab"));
        assertEquals(1, trie.size());
    }

    @Test
    void prefixMapKeepsOriginalInterfaceAndExposesCheckedPut() {
        final ReverseStringTrie<String> trie = new ReverseStringTrie<>();
        // The original view interface is preserved: prefixMap still returns a SortedMap.
        final SortedMap<String, String> view = trie.prefixMap("ab");
        assertTrue(view instanceof PrefixMapView);

        final PrefixMapView<String, String> checked = (PrefixMapView<String, String>) view;
        checked.putChecked("dab", "1");
        assertEquals("1", trie.get("dab"));
        assertThrows(IllegalArgumentException.class, () -> checked.putChecked("abd", "2"));
        assertFalse(trie.containsKey("abd"));
    }

    @Test
    void rejectsKeyWhoseStringPrefixMatchesButBitsDoNot() {
        final ReverseStringTrie<String> trie = new ReverseStringTrie<>();
        trie.put("ab", "0");
        trie.put("cab", "1");
        final PrefixMapView<String, String> view = trie.prefixMapView("ab");

        // "abc" starts with "ab" as a string, but its reversed bits start with 'c', not with the bits of "ab".
        assertThrows(IllegalArgumentException.class, () -> view.putChecked("abc", "2"));

        // The backing trie is completely unchanged by the rejected put.
        assertFalse(trie.containsKey("abc"));
        assertEquals(2, trie.size());
        assertEquals("0", trie.get("ab"));
        assertEquals("1", trie.get("cab"));
    }

    @Test
    void updatesExistingKeyThroughBackingTrie() {
        final ReverseStringTrie<String> trie = new ReverseStringTrie<>();
        trie.put("ab", "old");
        final PrefixMapView<String, String> view = trie.prefixMapView("ab");

        assertEquals("old", view.putChecked("ab", "new"));
        assertEquals("new", trie.get("ab"));
        assertEquals(1, trie.size());
    }

    /**
     * Calling example: obtain a checked prefix view, write legal keys through it and see them in the backing trie, while out-of-view keys are rejected
     * without touching the trie.
     */
    @Test
    void usageExample() {
        final ReverseStringTrie<String> trie = new ReverseStringTrie<>();
        final PrefixMapView<String, String> view = trie.prefixMapView("ab");

        view.putChecked("cab", "1"); // in view: bits of "cab" start with the bits of "ab"
        view.putChecked("ab", "2");  // in view: the prefix key itself
        try {
            view.putChecked("abc", "3"); // out of view: string prefix matches, bit prefix does not
            throw new AssertionError("expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
            // the backing trie is left completely unchanged
        }

        assertEquals(2, trie.size());
        assertEquals("1", trie.get("cab"));
        assertEquals("2", trie.get("ab"));
        assertFalse(trie.containsKey("abc"));
    }
}
