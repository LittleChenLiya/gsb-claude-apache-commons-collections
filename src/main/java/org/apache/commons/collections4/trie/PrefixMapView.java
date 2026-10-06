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

import java.util.SortedMap;

/**
 * A {@link SortedMap} view of a {@link org.apache.commons.collections4.Trie Trie} that contains all elements whose keys are prefixed, on a bit level, by a
 * given search key. This interface adds a checked {@link #putChecked(Object, Object)} entry point to the plain view returned by
 * {@link org.apache.commons.collections4.Trie#prefixMap(Object)}.
 * <p>
 * Whether a key belongs to this view is decided by the backing trie's {@link KeyAnalyzer}, that is by the actual bit prefix of the key and never by a
 * string-level (or otherwise encoded) prefix. For example, with a {@link KeyAnalyzer} that analyzes characters in reverse order, the view for {@code "ab"}
 * contains keys whose <em>bits</em> start with the bits of {@code "ab"} (such as {@code "cab"}), while a key like {@code "abc"}, whose string form starts
 * with {@code "ab"}, is rejected because its bits do not.
 * </p>
 * <p>
 * Example:
 * </p>
 * <pre>{@code
 * PatriciaTrie<String> trie = new PatriciaTrie<>();
 * trie.put("Albert", "1");
 * PrefixMapView<String, String> view = trie.prefixMapView("Al");
 * view.putChecked("Alberta", "2"); // OK: bits of "Alberta" start with bits of "Al"
 * view.putChecked("Carl", "3");    // throws IllegalArgumentException, trie unchanged
 * }</pre>
 *
 * @param <K> The type of the keys in this map.
 * @param <V> The type of the values in this map.
 * @since 4.6.1
 */
public interface PrefixMapView<K, V> extends SortedMap<K, V> {

    /**
     * Associates the specified value with the specified key in this view after checking that the key belongs to this view.
     * <p>
     * The membership check is delegated to the backing trie's {@link KeyAnalyzer#isPrefix(Object, int, int, Object)}, so it is the key's real bit prefix that
     * decides, not any string-level prefix. If the key is outside this view an {@link IllegalArgumentException} is thrown and the backing trie is left
     * completely unchanged. If the key is inside this view, the mapping is written through the backing trie's regular update path.
     * </p>
     *
     * @param key the key with which the specified value is to be associated, must be inside this view.
     * @param value the value to be associated with the specified key.
     * @return the previous value associated with {@code key}, or {@code null} if there was no mapping for {@code key}.
     * @throws IllegalArgumentException if the key's bit prefix places it outside of this view.
     */
    V putChecked(K key, V value);
}
