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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.SortedMap;

import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.analyzer.StringKeyAnalyzer;
import org.junit.jupiter.api.Test;

/**
 * Tests for the prefix view's checked {@code put}.
 * <p>
 * The checked {@code put} decides whether a key belongs to the view based on the real bit prefix
 * of the underlying {@link KeyAnalyzer}, not a string prefix. These tests use a custom bit analyzer
 * whose {@code isPrefix} is a permissive string check but whose {@code bitIndex} performs the real
 * bit-level comparison, so keys that look like they have the same string prefix but whose bits
 * differ are rejected.
 * </p>
 */
class PrefixMapPutTest {

    /**
     * A custom {@link KeyAnalyzer} for {@link String} keys whose
     * {@link #isPrefix(String, int, int, String)} is deliberately implemented as a permissive
     * string check (only the first character is compared), while
     * {@link #bitIndex(String, int, int, String, int, int)} performs the real bit-level comparison
     * inherited from {@link StringKeyAnalyzer}.
     * <p>
     * The two methods disagree for keys that share their first character but differ afterwards:
     * {@code isPrefix} accepts them, but {@code bitIndex} rejects them. This lets the tests prove
     * that the prefix view's checked {@code put} uses the real bit prefix rather than the string
     * prefix.
     * </p>
     */
    private static final class PermissivePrefixStringKeyAnalyzer extends StringKeyAnalyzer {

        private static final long serialVersionUID = 1L;

        static final PermissivePrefixStringKeyAnalyzer INSTANCE = new PermissivePrefixStringKeyAnalyzer();

        @Override
        public boolean isPrefix(final String prefix, final int offsetInBits,
                                final int lengthInBits, final String key) {
            if (key == null || prefix == null || key.isEmpty() || prefix.isEmpty()) {
                return false;
            }
            // Deliberately permissive: only the first character is compared.
            return key.charAt(0) == prefix.charAt(0);
        }
    }

    /**
     * A minimal {@link AbstractPatriciaTrie} that accepts a custom {@link KeyAnalyzer}.
     */
    private static final class CustomAnalyzerPatriciaTrie<V> extends AbstractPatriciaTrie<String, V> {

        private static final long serialVersionUID = 1L;

        CustomAnalyzerPatriciaTrie(final KeyAnalyzer<? super String> keyAnalyzer) {
            super(keyAnalyzer);
        }
    }

    @Test
    void testCheckedPutUsesBitPrefixNotStringPrefix() {
        // Build a trie with the custom analyzer. Its isPrefix is a permissive string check
        // (only the first character is compared), but its bitIndex performs the real bit-level
        // comparison.
        final Trie<String, String> trie =
                new CustomAnalyzerPatriciaTrie<>(PermissivePrefixStringKeyAnalyzer.INSTANCE);

        // A prefix view over "AB".
        final SortedMap<String, String> view = trie.prefixMap("AB");

        // "AX" starts with "A" (string prefix matches) but its bits differ from "AB" at the
        // second character, so it must be rejected by the bit check. The underlying trie must
        // be completely unchanged.
        assertThrows(IllegalArgumentException.class, () -> view.put("AX", "v1"));
        assertNull(trie.get("AX"));
        assertTrue(trie.isEmpty());

        // "A" is shorter than the prefix "AB", so it cannot contain the full bit prefix and
        // must also be rejected.
        assertThrows(IllegalArgumentException.class, () -> view.put("A", "v0"));
        assertNull(trie.get("A"));
        assertTrue(trie.isEmpty());

        // "AB" is exactly the prefix, so it is accepted and written through the existing
        // update path. It can be read back from the underlying trie.
        assertNull(view.put("AB", "v2"));
        assertEquals("v2", trie.get("AB"));
        assertEquals("v2", view.get("AB"));

        // "ABX" has the same bit prefix as "AB", so it is accepted and written through the
        // existing update path. It can be read back from the underlying trie.
        assertNull(view.put("ABX", "v3"));
        assertEquals("v3", trie.get("ABX"));
        assertEquals("v3", view.get("ABX"));

        // Updating an existing key through the view also works and goes through the existing
        // update path.
        assertEquals("v3", view.put("ABX", "v4"));
        assertEquals("v4", trie.get("ABX"));
        assertEquals("v4", view.get("ABX"));

        // The rejected key "AX" must still be absent from the underlying trie.
        assertNull(trie.get("AX"));
        assertEquals(2, trie.size());
    }

}
