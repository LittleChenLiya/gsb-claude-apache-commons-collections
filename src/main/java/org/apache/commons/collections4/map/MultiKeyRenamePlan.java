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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections4.keyvalue.MultiKey;

/**
 * A plan for renaming the keys of a {@link MultiKeyMap}.
 * <p>
 * This class allows multiple key renames to be specified and then applied
 * atomically. The target keys are calculated first and checked for conflicts
 * before any changes are made to the map. If a conflict is detected, the map
 * is left unchanged.
 * </p>
 * <p>
 * Two keys may be swapped by specifying both renames:
 * </p>
 * <pre>
 * MultiKeyRenamePlan&lt;K&gt; plan = new MultiKeyRenamePlan&lt;&gt;()
 *     .rename(keyA, keyB)
 *     .rename(keyB, keyA);
 * plan.applyTo(map);
 * </pre>
 * <p>
 * The original value references are preserved; values are not cloned.
 * </p>
 *
 * @param <K> the type of keys
 * @since 4.6.1
 */
public class MultiKeyRenamePlan<K> {

    /** The renames, keyed by the old key. */
    private final Map<MultiKey<? extends K>, MultiKey<? extends K>> renames = new LinkedHashMap<>();

    /**
     * Creates a new empty rename plan.
     */
    public MultiKeyRenamePlan() {
    }

    /**
     * Adds a rename from the specified old key to the specified new key.
     *
     * @param oldKey the old key, not null
     * @param newKey the new key, not null
     * @return this plan, for chaining
     * @throws NullPointerException if either key is null
     */
    public MultiKeyRenamePlan<K> rename(final MultiKey<? extends K> oldKey, final MultiKey<? extends K> newKey) {
        Objects.requireNonNull(oldKey, "oldKey");
        Objects.requireNonNull(newKey, "newKey");
        renames.put(oldKey, newKey);
        return this;
    }

    /**
     * Applies this rename plan to the specified map.
     * <p>
     * The target keys are calculated first and checked for conflicts.
     * If a conflict is detected, an exception is thrown and the map is
     * left unchanged. Otherwise, all renames are applied atomically.
     * </p>
     *
     * @param <V> the type of values
     * @param map the map to apply the renames to, not null
     * @throws NullPointerException if the map is null
     * @throws IllegalArgumentException if a conflict is detected or a source key is not found
     */
    public <V> void applyTo(final MultiKeyMap<K, V> map) {
        Objects.requireNonNull(map, "map");

        // Phase 1: calculate target keys and check for conflicts.
        // The original value references are captured here so that they can
        // be re-attached to the new keys without cloning.
        final Map<MultiKey<? extends K>, V> oldValues = new LinkedHashMap<>();
        final java.util.Set<MultiKey<? extends K>> targetKeys = new java.util.HashSet<>();

        for (final Map.Entry<MultiKey<? extends K>, MultiKey<? extends K>> entry : renames.entrySet()) {
            final MultiKey<? extends K> oldKey = entry.getKey();
            final MultiKey<? extends K> newKey = entry.getValue();

            if (!map.containsKey(oldKey)) {
                throw new IllegalArgumentException("Source key not found in map: " + oldKey);
            }

            // A conflict occurs when two source keys rename to the same target key.
            if (!targetKeys.add(newKey)) {
                throw new IllegalArgumentException("Conflict: duplicate target key: " + newKey);
            }

            oldValues.put(oldKey, map.get(oldKey));
        }

        // Phase 2: apply the renames atomically.
        // First remove all old entries, then add the new entries.
        // This ordering is what makes key swaps work correctly.
        for (final MultiKey<? extends K> oldKey : oldValues.keySet()) {
            map.remove(oldKey);
        }

        for (final Map.Entry<MultiKey<? extends K>, MultiKey<? extends K>> entry : renames.entrySet()) {
            final MultiKey<? extends K> oldKey = entry.getKey();
            final MultiKey<? extends K> newKey = entry.getValue();
            final V value = oldValues.get(oldKey);
            map.put(newKey, value);
        }
    }

}
