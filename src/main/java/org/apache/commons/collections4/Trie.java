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

package org.apache.commons.collections4;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

/**
 * Defines the interface for a prefix tree, an ordered tree data structure. For more information, see <a href="https://en.wikipedia.org/wiki/Trie">Tries</a>.
 *
 * @param <K> The type of the keys in this map.
 * @param <V> The type of the values in this map.
 * @since 4.0
 */
public interface Trie<K, V> extends IterableSortedMap<K, V> {

    /**
     * Returns a view of this {@link Trie} of all elements that are prefixed by the given key.
     * <p>
     * In a {@link Trie} with fixed size keys, this is essentially a {@link #get(Object)} operation.
     * </p>
     * <p>
     * For example, if the {@link Trie} contains 'Anna', 'Anael', 'Analu', 'Andreas', 'Andrea', 'Andres', and 'Anatole', then a lookup of 'And' would return
     * 'Andreas', 'Andrea', and 'Andres'.
     * </p>
     *
     * @param key The key used in the search.
     * @return A {@link SortedMap} view of this {@link Trie} with all elements whose key is prefixed by the search key.
     */
    SortedMap<K, V> prefixMap(K key);

    /**
     * Returns an {@link Iterator} over the {@link Map.Entry} objects whose keys are prefixed by the given key, in the same lexicographical order as
     * {@link #prefixMap(Object)}.
     * <p>
     * In contrast to the {@link Iterator} of {@link #prefixMap(Object)}, this adapter supports an interleaved {@link Iterator#remove()} cursor contract:
     * after the current element has been removed, iteration continues with the original-order successor. Removing an element neither skips a not-yet-visited
     * element nor returns the deleted node again.
     * </p>
     * <p>
     * Any structural modification of the {@link Trie} other than calling {@link Iterator#remove()} still invalidates the iterator in the usual fail-fast way
     * by throwing a {@link java.util.ConcurrentModificationException}. {@link Iterator#remove()} called before the first {@link Iterator#next()} (or twice
     * for the same element) throws an {@link IllegalStateException}.
     * </p>
     * <p>
     * The default implementation delegates to the iterator of a read-only copy of the {@link #prefixMap(Object)} view, so its {@link Iterator#remove()}
     * always throws {@link UnsupportedOperationException}; concrete mutable implementations should override it to return a removable iterator.
     * </p>
     *
     * @param key The key used in the search.
     * @return An {@link Iterator} of the {@link Map.Entry} objects whose key is prefixed by the search key.
     * @since 4.6.1
     */
    default Iterator<Map.Entry<K, V>> prefixIterator(final K key) {
        return Collections.unmodifiableMap(prefixMap(key)).entrySet().iterator();
    }
}
