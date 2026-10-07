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
package org.apache.commons.collections4.multimap;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.Unmodifiable;
import org.apache.commons.collections4.collection.UnmodifiableCollection;
import org.apache.commons.collections4.iterators.TransformIterator;
import org.apache.commons.collections4.iterators.UnmodifiableIterator;
import org.apache.commons.collections4.iterators.UnmodifiableMapIterator;
import org.apache.commons.collections4.keyvalue.AbstractMapEntry;
import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.collections4.multiset.UnmodifiableMultiSet;
import org.apache.commons.collections4.set.UnmodifiableSet;

/**
 * Decorates another {@link MultiValuedMap} to ensure it can't be altered.
 * <p>
 * Attempts to modify it will result in an UnsupportedOperationException.
 * </p>
 * <p>
 * The {@link #asMap()} view returned by {@link #unmodifiableMultiValuedMap(MultiValuedMap)}
 * is a <em>shallow</em> wrapper: the map itself cannot be changed, but the value collections
 * exposed by it can still be used to alter the decorated map. Use
 * {@link #deepUnmodifiableMultiValuedMap(MultiValuedMap)} to obtain a <em>deep</em> view
 * whose {@code asMap()} value collections (including the ones reachable through its entry
 * set and values collection) are unmodifiable as well.
 * </p>
 *
 * @param <K> The type of key elements
 * @param <V> The type of value elements
 * @since 4.1
 */
public final class UnmodifiableMultiValuedMap<K, V>
        extends AbstractMultiValuedMapDecorator<K, V> implements Unmodifiable {

    /**
     * Deeply unmodifiable {@link Map} view of a {@link MultiValuedMap}.
     * <p>
     * The map is a live view of the decorated map, but neither the map itself nor the value
     * collections exposed by it (directly or through its entry set or values collection) can
     * be modified.
     * </p>
     */
    private final class DeepAsMap extends AbstractMap<K, Collection<V>> implements Unmodifiable {

        /** The live asMap view of the decorated map. */
        private final transient Map<K, Collection<V>> map;

        DeepAsMap(final Map<K, Collection<V>> map) {
            this.map = map;
        }

        /** Always throws {@link UnsupportedOperationException}. */
        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsKey(final Object key) {
            return map.containsKey(key);
        }

        @Override
        public boolean containsValue(final Object value) {
            if (!(value instanceof Collection<?>)) {
                return false;
            }
            for (final Collection<V> values : map.values()) {
                if (CollectionUtils.isEqualCollection(values, (Collection<?>) value)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Set<Map.Entry<K, Collection<V>>> entrySet() {
            return new DeepEntrySet(map);
        }

        @Override
        public boolean equals(final Object object) {
            return this == object || map.equals(object);
        }

        @Override
        public Collection<V> get(final Object key) {
            final Collection<V> collection = map.get(key);
            return collection == null ? null : UnmodifiableCollection.unmodifiableCollection(collection);
        }

        @Override
        public int hashCode() {
            return map.hashCode();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public Set<K> keySet() {
            return UnmodifiableSet.unmodifiableSet(map.keySet());
        }

        /** Always throws {@link UnsupportedOperationException}. */
        @Override
        public Collection<V> put(final K key, final Collection<V> value) {
            throw new UnsupportedOperationException();
        }

        /** Always throws {@link UnsupportedOperationException}. */
        @Override
        public void putAll(final Map<? extends K, ? extends Collection<V>> mapToCopy) {
            throw new UnsupportedOperationException();
        }

        /** Always throws {@link UnsupportedOperationException}. */
        @Override
        public Collection<V> remove(final Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public String toString() {
            return map.toString();
        }

        @Override
        public Collection<Collection<V>> values() {
            return new DeepValues(map.values());
        }
    }

    /**
     * Entry set of the deeply unmodifiable asMap view. Every entry's value collection is
     * exposed through an unmodifiable wrapper.
     */
    private final class DeepEntrySet extends AbstractSet<Map.Entry<K, Collection<V>>> {

        /** The live asMap view of the decorated map. */
        private final transient Map<K, Collection<V>> map;

        DeepEntrySet(final Map<K, Collection<V>> map) {
            this.map = map;
        }

        /** Always throws {@link UnsupportedOperationException}. */
        @Override
        public boolean add(final Map.Entry<K, Collection<V>> entry) {
            throw new UnsupportedOperationException();
        }

        /** Always throws {@link UnsupportedOperationException}. */
        @Override
        public boolean addAll(final Collection<? extends Map.Entry<K, Collection<V>>> collection) {
            throw new UnsupportedOperationException();
        }

        /** Always throws {@link UnsupportedOperationException}. */
        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(final Object object) {
            if (!(object instanceof Map.Entry)) {
                return false;
            }
            final Map.Entry<?, ?> entry = (Map.Entry<?, ?>) object;
            final Collection<V> values = map.get(entry.getKey());
            final Object candidate = entry.getValue();
            return values != null && candidate instanceof Collection<?>
                    && CollectionUtils.isEqualCollection(values, (Collection<?>) candidate);
        }

        @Override
        public Iterator<Map.Entry<K, Collection<V>>> iterator() {
            final Iterator<Map.Entry<K, Collection<V>>> iterator = map.entrySet().iterator();
            return new Iterator<Map.Entry<K, Collection<V>>>() {

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public Map.Entry<K, Collection<V>> next() {
                    return new DeepMapEntry(iterator.next());
                }

                /** Always throws {@link UnsupportedOperationException}. */
                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        /** Always throws {@link UnsupportedOperationException}. */
        @Override
        public boolean remove(final Object object) {
            throw new UnsupportedOperationException();
        }

        /** Always throws {@link UnsupportedOperationException}. */
        @Override
        public boolean removeAll(final Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        /** Always throws {@link UnsupportedOperationException}. */
        @Override
        public boolean removeIf(final Predicate<? super Map.Entry<K, Collection<V>>> filter) {
            throw new UnsupportedOperationException();
        }

        /** Always throws {@link UnsupportedOperationException}. */
        @Override
        public boolean retainAll(final Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return map.size();
        }
    }

    /**
     * Entry of the deeply unmodifiable asMap view. Its value collection is exposed through
     * an unmodifiable wrapper and {@link #setValue(Collection)} is not supported.
     */
    private final class DeepMapEntry extends AbstractMapEntry<K, Collection<V>> {

        DeepMapEntry(final Map.Entry<K, Collection<V>> entry) {
            super(entry.getKey(), UnmodifiableCollection.unmodifiableCollection(entry.getValue()));
        }

        /**
         * Always throws {@link UnsupportedOperationException}.
         *
         * @param value Ignored.
         * @throws UnsupportedOperationException Always thrown.
         */
        @Override
        public Collection<V> setValue(final Collection<V> value) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Values collection of the deeply unmodifiable asMap view. Each contained value
     * collection is exposed through an unmodifiable wrapper and structural modifications
     * are rejected.
     */
    private final class DeepValues extends AbstractCollection<Collection<V>> {

        /** The live values collection of the decorated asMap view. */
        private final transient Collection<Collection<V>> collection;

        DeepValues(final Collection<Collection<V>> collection) {
            this.collection = collection;
        }

        /** Always throws {@link UnsupportedOperationException}. */
        @Override
        public boolean add(final Collection<V> value) {
            throw new UnsupportedOperationException();
        }

        /** Always throws {@link UnsupportedOperationException}. */
        @Override
        public boolean addAll(final Collection<? extends Collection<V>> values) {
            throw new UnsupportedOperationException();
        }

        /** Always throws {@link UnsupportedOperationException}. */
        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(final Object object) {
            return collection.contains(object);
        }

        @Override
        public boolean containsAll(final Collection<?> values) {
            return collection.containsAll(values);
        }

        @Override
        public boolean isEmpty() {
            return collection.isEmpty();
        }

        @Override
        public Iterator<Collection<V>> iterator() {
            return UnmodifiableIterator.unmodifiableIterator(
                    new TransformIterator<>(
                            collection.iterator(),
                            UnmodifiableCollection::unmodifiableCollection));
        }

        /** Always throws {@link UnsupportedOperationException}. */
        @Override
        public boolean remove(final Object object) {
            throw new UnsupportedOperationException();
        }

        /** Always throws {@link UnsupportedOperationException}. */
        @Override
        public boolean removeAll(final Collection<?> values) {
            throw new UnsupportedOperationException();
        }

        /** Always throws {@link UnsupportedOperationException}. */
        @Override
        public boolean removeIf(final Predicate<? super Collection<V>> filter) {
            throw new UnsupportedOperationException();
        }

        /** Always throws {@link UnsupportedOperationException}. */
        @Override
        public boolean retainAll(final Collection<?> values) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return collection.size();
        }

        @Override
        public Object[] toArray() {
            final Object[] result = collection.toArray();
            for (int i = 0; i < result.length; i++) {
                @SuppressWarnings("unchecked")
                final Collection<V> values = (Collection<V>) result[i];
                result[i] = UnmodifiableCollection.unmodifiableCollection(values);
            }
            return result;
        }

        @Override
        public <T> T[] toArray(final T[] array) {
            final T[] result = collection.toArray(array);
            for (int i = 0; i < result.length; i++) {
                @SuppressWarnings("unchecked")
                final Collection<V> values = (Collection<V>) result[i];
                result[i] = (T) UnmodifiableCollection.unmodifiableCollection(values);
            }
            return result;
        }

        @Override
        public String toString() {
            return collection.toString();
        }
    }

    /** Serialization version */
    private static final long serialVersionUID = 20150612L;

    /**
     * Factory method to create a deeply unmodifiable MultiValuedMap.
     * <p>
     * In addition to the guarantees given by
     * {@link #unmodifiableMultiValuedMap(MultiValuedMap)}, the {@link Map} returned by
     * {@link #asMap()} also prevents modification through its value collections: the
     * collections returned by {@code asMap().get(Object)}, {@code asMap().values()} and the
     * values of the entries returned by {@code asMap().entrySet()} are unmodifiable live
     * views. If the map passed in is already deeply unmodifiable, it is returned.
     * </p>
     * <p>
     * The returned map does not copy the contents of the given map, hence legal updates
     * performed directly on the underlying map remain visible through the view.
     * </p>
     *
     * @param <K> The type of key elements
     * @param <V> The type of value elements
     * @param map  The map to decorate, may not be null
     * @return A deeply unmodifiable MultiValuedMap
     * @throws NullPointerException if map is null
     */
    @SuppressWarnings("unchecked")
    public static <K, V> UnmodifiableMultiValuedMap<K, V> deepUnmodifiableMultiValuedMap(
            final MultiValuedMap<? extends K, ? extends V> map) {
        Objects.requireNonNull(map, "map");
        if (map instanceof UnmodifiableMultiValuedMap && ((UnmodifiableMultiValuedMap<?, ?>) map).deep) {
            return (UnmodifiableMultiValuedMap<K, V>) map;
        }
        return new UnmodifiableMultiValuedMap<>(map, true);
    }

    /**
     * Factory method to create an unmodifiable MultiValuedMap.
     * <p>
     * If the map passed in is already unmodifiable, it is returned. Note that the
     * {@link #asMap()} view is only a shallow wrapper: its value collections can still be
     * used to modify this map. Use {@link #deepUnmodifiableMultiValuedMap(MultiValuedMap)}
     * for a deeply unmodifiable view.
     * </p>
     *
     * @param <K> The type of key elements
     * @param <V> The type of value elements
     * @param map  The map to decorate, may not be null
     * @return An unmodifiable MultiValuedMap
     * @throws NullPointerException if map is null
     */
    @SuppressWarnings("unchecked")
    public static <K, V> UnmodifiableMultiValuedMap<K, V> unmodifiableMultiValuedMap(
            final MultiValuedMap<? extends K, ? extends V> map) {
        if (map instanceof Unmodifiable) {
            return (UnmodifiableMultiValuedMap<K, V>) map;
        }
        return new UnmodifiableMultiValuedMap<>(map, false);
    }

    /** Whether {@link #asMap()} returns a deeply unmodifiable view. */
    private final boolean deep;

    /** Cached deeply unmodifiable asMap view. */
    private transient Map<K, Collection<V>> deepAsMapView;

    /**
     * Constructor that wraps (not copies).
     *
     * @param map  The MultiValuedMap to decorate, may not be null
     * @param deep Whether the asMap view is deeply unmodifiable
     * @throws NullPointerException if the map is null
     */
    @SuppressWarnings("unchecked")
    private UnmodifiableMultiValuedMap(final MultiValuedMap<? extends K, ? extends V> map, final boolean deep) {
        super((MultiValuedMap<K, V>) Objects.requireNonNull(map, "map"));
        this.deep = deep;
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        if (deep) {
            if (deepAsMapView == null) {
                @SuppressWarnings("unchecked")
                final Map<K, Collection<V>> liveMap = (Map<K, Collection<V>>) decorated().asMap();
                deepAsMapView = new DeepAsMap(liveMap);
            }
            return deepAsMapView;
        }
        return UnmodifiableMap.unmodifiableMap(decorated().asMap());
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException Always thrown.
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Entry<K, V>> entries() {
        return UnmodifiableCollection.unmodifiableCollection(decorated().entries());
    }

    @Override
    public Collection<V> get(final K key) {
        return UnmodifiableCollection.unmodifiableCollection(decorated().get(key));
    }

    @Override
    public MultiSet<K> keys() {
        return UnmodifiableMultiSet.unmodifiableMultiSet(decorated().keys());
    }

    @Override
    public Set<K> keySet() {
        return UnmodifiableSet.unmodifiableSet(decorated().keySet());
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned map iterator's {@link MapIterator#setValue(Object)} method is not supported
     * and will throw an {@link UnsupportedOperationException}.
     * </p>
     */
    @Override
    public MapIterator<K, V> mapIterator() {
        return UnmodifiableMapIterator.unmodifiableMapIterator(decorated().mapIterator());
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     *
     * @param key Ignored.
     * @throws UnsupportedOperationException Always thrown.
     */
    @Override
    public boolean put(final K key, final V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     *
     * @param key Ignored.
     * @param values Ignored.
     * @throws UnsupportedOperationException Always thrown.
     */
    @Override
    public boolean putAll(final K key, final Iterable<? extends V> values) {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     *
     * @param map Ignored.
     * @throws UnsupportedOperationException Always thrown.
     */
    @Override
    public boolean putAll(final Map<? extends K, ? extends V> map) {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     *
     * @param map Ignored.
     * @throws UnsupportedOperationException Always thrown.
     */
    @Override
    public boolean putAll(final MultiValuedMap<? extends K, ? extends V> map) {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     *
     * @param key Ignored.
     * @throws UnsupportedOperationException Always thrown.
     */
    @Override
    public Collection<V> remove(final Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     *
     * @param key Ignored.
     * @param item Ignored.
     * @throws UnsupportedOperationException Always thrown.
     */
    @Override
    public boolean removeMapping(final Object key, final Object item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> values() {
        return UnmodifiableCollection.unmodifiableCollection(decorated().values());
    }

}
