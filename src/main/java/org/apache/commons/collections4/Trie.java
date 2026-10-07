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
     * Returns an {@link Iterator} over the entries of this {@link Trie} whose keys are prefixed by the given key, in ascending key order, whose
     * {@link Iterator#remove()} deletes the current entry from this {@link Trie} while iteration continues at the successor of the removed entry in the
     * original key order: no remaining entry is skipped and no already removed entry is returned.
     * <p>
     * The iterator follows the usual cursor contract: {@link Iterator#remove()} may only be called once per {@link Iterator#next()} and is rejected with an
     * {@link IllegalStateException} if {@link Iterator#next()} has not been called yet or if it is called twice in a row. A structural modification of the
     * underlying {@link Trie} by any means other than the iterator's own {@link Iterator#remove()} causes a fail-fast
     * {@link java.util.ConcurrentModificationException}.
     * </p>
     * <p>
     * This default implementation delegates to {@code prefixMap(prefix).entrySet().iterator()}; the exact removal and fail-fast behavior then depends on
     * that view's iterator. Implementations are encouraged to override this method with an iterator that guarantees the cursor contract described above.
     * </p>
     *
     * @param prefix The key prefix used in the search.
     * @return A removable {@link Iterator} over the entries whose key is prefixed by the search key.
     * @since 4.6.1
     */
    default Iterator<Map.Entry<K, V>> removablePrefixIterator(K prefix) {
        return prefixMap(prefix).entrySet().iterator();
    }
}
