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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.keyvalue.MultiKey;

/**
 * A transactional rename plan for the keys of a {@link MultiKeyMap}.
 * <p>
 * A plan collects a set of key renames and applies them in one shot when
 * {@link #commit()} is called. Committing works in two phases:
 * </p>
 * <ol>
 * <li>Every target key is computed up front and checked against the current
 * map content using the normal {@link MultiKey} {@code equals} rules. A rename
 * whose source key is absent, a target key that collides with a key that is
 * not itself being renamed away, or two renames computing the same target key
 * are all conflicts.</li>
 * <li>If and only if no conflict is found, the fully computed new map content
 * is committed to the decorated map in a single batch.</li>
 * </ol>
 * <p>
 * Because all target keys are computed before anything is changed, two keys
 * may be swapped in a single plan, for example via {@link #swap(MultiKey, MultiKey)}.
 * If a plan is rejected, the original map is left completely unchanged.
 * </p>
 * <p>
 * Values are never copied: each value keeps its original object reference, and
 * the committed entries remain ordinary {@link Map} entries keyed by
 * {@link MultiKey}, so the regular {@code Map} API keeps working on them.
 * </p>
 * <p>
 * This class deliberately adds no per-arity key methods and no special null
 * handling; renames are expressed with plain {@link MultiKey} instances and
 * the standard {@code MultiKeyMap} key rules apply.
 * </p>
 * <p>
 * <strong>Note that this class is not synchronized and is not thread-safe.</strong>
 * </p>
 *
 * @param <K> The type of the keys in the map
 * @param <V> The type of the values in the map
 * @since 4.6
 */
public class MultiKeyMapRenamePlan<K, V> {

    /**
     * A single rename from a source key to a target key.
     *
     * @param <K> The type of the keys in the map
     */
    private static final class Rename<K> {

        /** The existing key to rename. */
        private final MultiKey<? extends K> from;

        /** The new key to rename to. */
        private final MultiKey<? extends K> to;

        Rename(final MultiKey<? extends K> from, final MultiKey<? extends K> to) {
            this.from = from;
            this.to = to;
        }
    }

    /**
     * Creates a new rename plan for the specified map.
     *
     * @param <K>  the key type
     * @param <V>  the value type
     * @param map  The map to rename keys in, not null
     * @return A new rename plan for the map
     * @throws NullPointerException if map is null
     */
    public static <K, V> MultiKeyMapRenamePlan<K, V> on(final MultiKeyMap<K, V> map) {
        return new MultiKeyMapRenamePlan<>(map);
    }

    /** The map whose keys are renamed. */
    private final MultiKeyMap<K, V> map;

    /** The planned renames, in the order they were added. */
    private final List<Rename<K>> renames = new ArrayList<>();

    /**
     * Constructs a new rename plan for the specified map.
     *
     * @param map  The map to rename keys in, not null
     * @throws NullPointerException if the map is null
     */
    public MultiKeyMapRenamePlan(final MultiKeyMap<K, V> map) {
        this.map = Objects.requireNonNull(map, "map");
    }

    /**
     * Adds a rename of one key to another key to this plan.
     *
     * @param from  The existing key to rename, not null
     * @param to  The new key, not null
     * @return this plan, for chaining
     * @throws NullPointerException if either key is null
     */
    public MultiKeyMapRenamePlan<K, V> rename(final MultiKey<? extends K> from, final MultiKey<? extends K> to) {
        renames.add(new Rename<>(Objects.requireNonNull(from, "from"), Objects.requireNonNull(to, "to")));
        return this;
    }

    /**
     * Adds a swap of two keys to this plan; each key is renamed to the other.
     *
     * @param first  The first key to swap, not null
     * @param second  The second key to swap, not null
     * @return this plan, for chaining
     * @throws NullPointerException if either key is null
     */
    public MultiKeyMapRenamePlan<K, V> swap(final MultiKey<? extends K> first, final MultiKey<? extends K> second) {
        return rename(first, second).rename(second, first);
    }

    /**
     * Computes all target keys, checks them for conflicts and, if the plan is
     * valid, commits the new map content in one go.
     * <p>
     * If any source key is missing or any computed target key conflicts with a
     * key that stays in the map or with another target key, the plan is
     * rejected and the original map is left unchanged.
     * </p>
     *
     * @return true if the plan was committed, false if it was rejected and the
     *   map was left unchanged
     */
    public boolean commit() {
        // Phase 1: snapshot the current content, keeping the original value references.
        final Map<MultiKey<? extends K>, V> planned = new LinkedHashMap<>();
        map.forEach((key, value) -> planned.put(key, value));

        // Phase 2: resolve and detach every source key.
        final List<V> detachedValues = new ArrayList<>(renames.size());
        for (final Rename<K> rename : renames) {
            if (!planned.containsKey(rename.from)) {
                return false;
            }
            detachedValues.add(planned.remove(rename.from));
        }

        // Phase 3: check every computed target key for conflicts.
        final Set<MultiKey<? extends K>> targets = new HashSet<>();
        for (final Rename<K> rename : renames) {
            if (planned.containsKey(rename.to) || !targets.add(rename.to)) {
                return false;
            }
        }

        // Phase 4: place each original value at its target key.
        for (int i = 0; i < renames.size(); i++) {
            planned.put(renames.get(i).to, detachedValues.get(i));
        }

        // Phase 5: commit the fully computed new map in one batch.
        map.clear();
        map.putAll(planned);
        return true;
    }
}
