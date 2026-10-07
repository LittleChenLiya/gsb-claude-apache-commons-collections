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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.Unmodifiable;
import org.apache.commons.collections4.collection.AbstractCollectionDecorator;
import org.apache.commons.collections4.collection.UnmodifiableCollection;
import org.apache.commons.collections4.iterators.AbstractIteratorDecorator;
import org.apache.commons.collections4.iterators.UnmodifiableMapIterator;
import org.apache.commons.collections4.keyvalue.AbstractMapEntryDecorator;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.collections4.map.AbstractMapDecorator;
import org.apache.commons.collections4.multiset.UnmodifiableMultiSet;
import org.apache.commons.collections4.set.AbstractSetDecorator;
import org.apache.commons.collections4.set.UnmodifiableSet;

/**
 * Decorates another {@link MultiValuedMap} to ensure it can't be altered, including
 * through the nested views of its {@link #asMap()} map.
 * <p>
 * This is a deep variant of {@link UnmodifiableMultiValuedMap}: on top of the map
 * itself, the per-key value collections contained in the {@code asMap()} view, the
 * entries of its entry set and its values view are unmodifiable as well. Attempts to
 * modify the decorated map through any view, for example the value collections of
 * {@code asMap()}, the {@code entries()} iterator or the {@code keys()} multiset,
 * will result in an {@link UnsupportedOperationException}.
 * </p>
 * <p>
 * The decorator is a live view: it wraps, not copies, so later changes made through
 * the decorated map are reflected immediately.
 * </p>
 *
 * @param <K> The type of key elements
 * @param <V> The type of value elements
 * @since 4.6.1
 */
public final class DeepUnmodifiableMultiValuedMap<K, V>
        extends AbstractMultiValuedMapDecorator<K, V> implements Unmodifiable {

    /**
     * Deeply unmodifiable entry of the {@code asMap} view: the value collection is
     * unmodifiable and {@code setValue} is unsupported.
     *
     * @param <K> The type of key elements
     * @param <V> The type of value elements
     */
    private static final class AsMapEntry<K, V> extends AbstractMapEntryDecorator<K, Collection<V>> {

        /**
         * Constructor that wraps (not copies).
         *
         * @param entry  The entry to decorate, may not be null
         */
        AsMapEntry(final Entry<K, Collection<V>> entry) {
            super(entry);
        }

        @Override
        public Collection<V> getValue() {
            return unmodifiableValues(super.getValue());
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
     * Deeply unmodifiable, live view of the decorated map's {@code asMap} view.
     *
     * @param <K> The type of key elements
     * @param <V> The type of value elements
     */
    private static final class AsMapView<K, V> extends AbstractMapDecorator<K, Collection<V>> {

        /**
         * Constructor that wraps (not copies).
         *
         * @param map  The map to decorate, may not be null
         */
        AsMapView(final Map<K, Collection<V>> map) {
            super(map);
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
        public Set<Entry<K, Collection<V>>> entrySet() {
            return new UnmodifiableSetView<>(decorated().entrySet(), AsMapEntry::new);
        }

        @Override
        public Collection<V> get(final Object key) {
            final Collection<V> collection = decorated().get(key);
            return collection == null ? null : unmodifiableValues(collection);
        }

        @Override
        public Set<K> keySet() {
            return UnmodifiableSet.unmodifiableSet(decorated().keySet());
        }

        /**
         * Always throws {@link UnsupportedOperationException}.
         *
         * @param key Ignored.
         * @param value Ignored.
         * @throws UnsupportedOperationException Always thrown.
         */
        @Override
        public Collection<V> put(final K key, final Collection<V> value) {
            throw new UnsupportedOperationException();
        }

        /**
         * Always throws {@link UnsupportedOperationException}.
         *
         * @param mapToCopy Ignored.
         * @throws UnsupportedOperationException Always thrown.
         */
        @Override
        public void putAll(final Map<? extends K, ? extends Collection<V>> mapToCopy) {
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

        @Override
        public Collection<Collection<V>> values() {
            return new UnmodifiableCollectionView<>(decorated().values(),
                    DeepUnmodifiableMultiValuedMap::<V>unmodifiableValues);
        }
    }

    /**
     * Unmodifiable collection view that wraps each element it exposes.
     *
     * @param <E> The type of the elements in the collection
     */
    private static final class UnmodifiableCollectionView<E> extends AbstractCollectionDecorator<E> {

        /** Serialization version */
        private static final long serialVersionUID = 20261008L;

        /** Wraps the elements exposed by this view. */
        private final transient UnaryOperator<E> elementWrapper;

        /**
         * Constructor that wraps (not copies).
         *
         * @param collection  The collection to decorate, may not be null
         * @param elementWrapper  Wraps the elements exposed by this view, may not be null
         */
        UnmodifiableCollectionView(final Collection<E> collection, final UnaryOperator<E> elementWrapper) {
            super(collection);
            this.elementWrapper = elementWrapper;
        }

        /**
         * Always throws {@link UnsupportedOperationException}.
         *
         * @param object Ignored.
         * @throws UnsupportedOperationException Always thrown.
         */
        @Override
        public boolean add(final E object) {
            throw new UnsupportedOperationException();
        }

        /**
         * Always throws {@link UnsupportedOperationException}.
         *
         * @param coll Ignored.
         * @throws UnsupportedOperationException Always thrown.
         */
        @Override
        public boolean addAll(final Collection<? extends E> coll) {
            throw new UnsupportedOperationException();
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
        public Iterator<E> iterator() {
            return new AbstractIteratorDecorator<E>(decorated().iterator()) {

                @Override
                public E next() {
                    return elementWrapper.apply(getIterator().next());
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        /**
         * Always throws {@link UnsupportedOperationException}.
         *
         * @param object Ignored.
         * @throws UnsupportedOperationException Always thrown.
         */
        @Override
        public boolean remove(final Object object) {
            throw new UnsupportedOperationException();
        }

        /**
         * Always throws {@link UnsupportedOperationException}.
         *
         * @param coll Ignored.
         * @throws UnsupportedOperationException Always thrown.
         */
        @Override
        public boolean removeAll(final Collection<?> coll) {
            throw new UnsupportedOperationException();
        }

        /**
         * Always throws {@link UnsupportedOperationException}.
         *
         * @param filter Ignored.
         * @throws UnsupportedOperationException Always thrown.
         */
        @Override
        public boolean removeIf(final Predicate<? super E> filter) {
            throw new UnsupportedOperationException();
        }

        /**
         * Always throws {@link UnsupportedOperationException}.
         *
         * @param coll Ignored.
         * @throws UnsupportedOperationException Always thrown.
         */
        @Override
        public boolean retainAll(final Collection<?> coll) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Unmodifiable set view that wraps each element it exposes.
     *
     * @param <E> The type of the elements in the set
     */
    private static final class UnmodifiableSetView<E> extends AbstractSetDecorator<E> {

        /** Serialization version */
        private static final long serialVersionUID = 20261008L;

        /** Wraps the elements exposed by this view. */
        private final transient UnaryOperator<E> elementWrapper;

        /**
         * Constructor that wraps (not copies).
         *
         * @param set  The set to decorate, may not be null
         * @param elementWrapper  Wraps the elements exposed by this view, may not be null
         */
        UnmodifiableSetView(final Set<E> set, final UnaryOperator<E> elementWrapper) {
            super(set);
            this.elementWrapper = elementWrapper;
        }

        /**
         * Always throws {@link UnsupportedOperationException}.
         *
         * @param object Ignored.
         * @throws UnsupportedOperationException Always thrown.
         */
        @Override
        public boolean add(final E object) {
            throw new UnsupportedOperationException();
        }

        /**
         * Always throws {@link UnsupportedOperationException}.
         *
         * @param coll Ignored.
         * @throws UnsupportedOperationException Always thrown.
         */
        @Override
        public boolean addAll(final Collection<? extends E> coll) {
            throw new UnsupportedOperationException();
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
        public Iterator<E> iterator() {
            return new AbstractIteratorDecorator<E>(decorated().iterator()) {

                @Override
                public E next() {
                    return elementWrapper.apply(getIterator().next());
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        /**
         * Always throws {@link UnsupportedOperationException}.
         *
         * @param object Ignored.
         * @throws UnsupportedOperationException Always thrown.
         */
        @Override
        public boolean remove(final Object object) {
            throw new UnsupportedOperationException();
        }

        /**
         * Always throws {@link UnsupportedOperationException}.
         *
         * @param coll Ignored.
         * @throws UnsupportedOperationException Always thrown.
         */
        @Override
        public boolean removeAll(final Collection<?> coll) {
            throw new UnsupportedOperationException();
        }

        /**
         * Always throws {@link UnsupportedOperationException}.
         *
         * @param filter Ignored.
         * @throws UnsupportedOperationException Always thrown.
         */
        @Override
        public boolean removeIf(final Predicate<? super E> filter) {
            throw new UnsupportedOperationException();
        }

        /**
         * Always throws {@link UnsupportedOperationException}.
         *
         * @param coll Ignored.
         * @throws UnsupportedOperationException Always thrown.
         */
        @Override
        public boolean retainAll(final Collection<?> coll) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Unmodifiable entry of the entries view.
     *
     * @param <K> The type of key elements
     * @param <V> The type of value elements
     */
    private static final class UnmodifiableEntry<K, V> extends AbstractMapEntryDecorator<K, V> {

        /**
         * Constructor that wraps (not copies).
         *
         * @param entry  The entry to decorate, may not be null
         */
        UnmodifiableEntry(final Entry<K, V> entry) {
            super(entry);
        }

        /**
         * Always throws {@link UnsupportedOperationException}.
         *
         * @param value Ignored.
         * @throws UnsupportedOperationException Always thrown.
         */
        @Override
        public V setValue(final V value) {
            throw new UnsupportedOperationException();
        }
    }

    /** Serialization version */
    private static final long serialVersionUID = 20261008L;

    /**
     * Factory method to create a deeply unmodifiable MultiValuedMap.
     * <p>
     * If the map passed in is already deeply unmodifiable, it is returned.
     * </p>
     *
     * @param <K> The type of key elements
     * @param <V> The type of value elements
     * @param map  The map to decorate, may not be null
     * @return A deeply unmodifiable MultiValuedMap
     * @throws NullPointerException if map is null
     */
    @SuppressWarnings("unchecked")
    public static <K, V> DeepUnmodifiableMultiValuedMap<K, V> deepUnmodifiableMultiValuedMap(
            final MultiValuedMap<? extends K, ? extends V> map) {
        if (map instanceof DeepUnmodifiableMultiValuedMap) {
            return (DeepUnmodifiableMultiValuedMap<K, V>) map;
        }
        return new DeepUnmodifiableMultiValuedMap<>(map);
    }

    /**
     * Wraps a value collection of the {@code asMap} view in an unmodifiable view
     * of the same collection kind.
     *
     * @param <V> The type of value elements
     * @param collection  The collection to wrap, may not be null
     * @return An unmodifiable view of the collection
     */
    @SuppressWarnings("unchecked")
    private static <V> Collection<V> unmodifiableValues(final Collection<V> collection) {
        if (collection instanceof Set) {
            return UnmodifiableSet.unmodifiableSet((Set<V>) collection);
        }
        if (collection instanceof List) {
            return UnmodifiableList.unmodifiableList((List<V>) collection);
        }
        return UnmodifiableCollection.unmodifiableCollection(collection);
    }

    /**
     * Constructor that wraps (not copies).
     *
     * @param map  The MultiValuedMap to decorate, may not be null
     * @throws NullPointerException if the map is null
     */
    @SuppressWarnings("unchecked")
    private DeepUnmodifiableMultiValuedMap(final MultiValuedMap<? extends K, ? extends V> map) {
        super((MultiValuedMap<K, V>) map);
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        return new AsMapView<>(decorated().asMap());
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
        return new UnmodifiableCollectionView<>(decorated().entries(), UnmodifiableEntry::new);
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
     * @param value Ignored.
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
