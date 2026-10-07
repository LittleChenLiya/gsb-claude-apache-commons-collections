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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.Unmodifiable;
import org.apache.commons.collections4.collection.AbstractCollectionTest;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link DeepUnmodifiableMultiValuedMap}.
 */
public class DeepUnmodifiableMultiValuedMapTest<K, V> extends AbstractMultiValuedMapTest<K, V> {

    @Override
    protected int getIterationBehaviour() {
        return AbstractCollectionTest.UNORDERED;
    }

    @Override
    public boolean isAddSupported() {
        return false;
    }

    @Override
    public boolean isRemoveSupported() {
        return false;
    }

    @Override
    protected MultiValuedMap<K, V> makeFullMap() {
        final MultiValuedMap<K, V> map = new ArrayListValuedHashMap<>();
        addSampleMappings(map);
        return DeepUnmodifiableMultiValuedMap.<K, V>deepUnmodifiableMultiValuedMap(map);
    }

    @Override
    public String getCompatibilityVersion() {
        return "4.6.1"; // DeepUnmodifiableMultiValuedMap has been added in version 4.6.1
    }

    @Override
    public MultiValuedMap<K, V> makeObject() {
        return DeepUnmodifiableMultiValuedMap.<K, V>deepUnmodifiableMultiValuedMap(
                new ArrayListValuedHashMap<>());
    }

    /**
     * Acceptance scenario: a fixture of two keys and three values is decorated with
     * the deep view. Deletion through the three bypasses (the value collections of
     * {@code asMap()}, the {@code entries()} iterator and the {@code keys()} multiset)
     * must be rejected, the view must reflect later updates of the original map, and
     * the original shallow wrapper must keep its behavior.
     */
    @Test
    void testDeepViewRejectsBypassesAndReflectsUpdates() {
        // two keys, three values
        final MultiValuedMap<String, String> original = new ArrayListValuedHashMap<>();
        original.put("one", "a");
        original.put("one", "b");
        original.put("two", "c");
        final MultiValuedMap<String, String> view =
                DeepUnmodifiableMultiValuedMap.deepUnmodifiableMultiValuedMap(original);

        // bypass 1: the value collections of the asMap view
        assertThrows(UnsupportedOperationException.class, () -> view.asMap().get("one").remove("a"));
        assertThrows(UnsupportedOperationException.class, () -> view.asMap().get("one").clear());
        assertThrows(UnsupportedOperationException.class,
                () -> view.asMap().entrySet().iterator().next().getValue().remove("a"));
        assertThrows(UnsupportedOperationException.class,
                () -> view.asMap().values().iterator().next().remove("a"));
        assertThrows(UnsupportedOperationException.class,
                () -> view.asMap().entrySet().iterator().next().setValue(new ArrayList<>()));
        assertThrows(UnsupportedOperationException.class, () -> view.asMap().remove("one"));
        assertThrows(UnsupportedOperationException.class, () -> view.asMap().clear());

        // bypass 2: the entries iterator
        final Iterator<Entry<String, String>> entriesIt = view.entries().iterator();
        entriesIt.next();
        assertThrows(UnsupportedOperationException.class, entriesIt::remove);
        assertThrows(UnsupportedOperationException.class,
                () -> view.entries().iterator().next().setValue("x"));
        assertThrows(UnsupportedOperationException.class, () -> view.entries().clear());

        // bypass 3: the keys multiset
        assertThrows(UnsupportedOperationException.class, () -> view.keys().remove("one"));
        assertThrows(UnsupportedOperationException.class, () -> view.keys().remove("one", 1));
        final Iterator<String> keysIt = view.keys().iterator();
        keysIt.next();
        assertThrows(UnsupportedOperationException.class, keysIt::remove);

        // nothing was removed through the bypasses
        assertEquals(3, view.size());
        assertEquals(2, view.get("one").size());
        assertEquals(2, view.asMap().size());

        // the view reflects updates through the original map without copying
        original.removeMapping("one", "a");
        original.put("two", "d");
        assertFalse(view.containsMapping("one", "a"));
        assertTrue(view.containsMapping("two", "d"));
        assertEquals(3, view.size());
        assertEquals(2, view.asMap().get("two").size());
        assertTrue(view.keys().contains("two"));
        assertEquals(original.asMap().get("two"), view.asMap().get("two"));

        // the original shallow wrapper keeps its behavior
        final MultiValuedMap<String, String> shallow =
                UnmodifiableMultiValuedMap.unmodifiableMultiValuedMap(original);
        assertThrows(UnsupportedOperationException.class, () -> shallow.put("three", "e"));
        assertThrows(UnsupportedOperationException.class, () -> shallow.asMap().clear());
        assertSame(shallow, UnmodifiableMultiValuedMap.unmodifiableMultiValuedMap(shallow));
        assertTrue(shallow.containsMapping("two", "d"));
    }

    @Test
    void testDecorateFactory() {
        final MultiValuedMap<K, V> map = makeFullMap();
        assertSame(map, DeepUnmodifiableMultiValuedMap.deepUnmodifiableMultiValuedMap(map));
    }

    @Test
    void testDecoratorFactoryNullMap() {
        assertThrows(NullPointerException.class, () -> DeepUnmodifiableMultiValuedMap.deepUnmodifiableMultiValuedMap(null),
                "map must not be null");
    }

    @Test
    void testUnmodifiable() {
        assertTrue(makeObject() instanceof Unmodifiable);
        assertTrue(makeFullMap() instanceof Unmodifiable);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUnmodifiableAsMap() {
        resetFull();
        final Map<K, Collection<V>> mapCol = getMap().asMap();
        assertThrows(UnsupportedOperationException.class,
                () -> mapCol.put((K) "four", (Collection<V>) new ArrayList<>()));
        assertThrows(UnsupportedOperationException.class, () -> mapCol.remove("four"));
        assertThrows(UnsupportedOperationException.class, mapCol::clear);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUnmodifiableAsMapValueCollections() {
        resetFull();
        final Map<K, Collection<V>> mapCol = getMap().asMap();
        final K key = (K) getSampleKeys()[0];
        final Collection<V> values = mapCol.get(key);
        assertThrows(UnsupportedOperationException.class, () -> values.add((V) "four"));
        assertThrows(UnsupportedOperationException.class, () -> values.remove("four"));
        assertThrows(UnsupportedOperationException.class, values::clear);
        final Iterator<V> it = values.iterator();
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove);

        for (final Collection<V> coll : mapCol.values()) {
            assertThrows(UnsupportedOperationException.class, () -> coll.add((V) "four"));
            assertThrows(UnsupportedOperationException.class, () -> coll.remove("four"));
        }
        assertThrows(UnsupportedOperationException.class, () -> mapCol.values().clear());

        for (final Entry<K, Collection<V>> entry : mapCol.entrySet()) {
            assertThrows(UnsupportedOperationException.class, () -> entry.getValue().remove("four"));
            assertThrows(UnsupportedOperationException.class,
                    () -> entry.setValue((Collection<V>) new ArrayList<>()));
        }
        assertThrows(UnsupportedOperationException.class, () -> mapCol.entrySet().clear());
        assertThrows(UnsupportedOperationException.class, () -> mapCol.keySet().clear());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUnmodifiableEntries() {
        resetFull();
        final Collection<Entry<K, V>> entries = getMap().entries();
        assertThrows(UnsupportedOperationException.class, entries::clear);

        final Iterator<Entry<K, V>> it = entries.iterator();
        final Entry<K, V> entry = it.next();
        assertThrows(UnsupportedOperationException.class, it::remove);
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue((V) "three"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUnmodifiableKeys() {
        resetFull();
        final MultiSet<K> keys = getMap().keys();
        assertThrows(UnsupportedOperationException.class, () -> keys.add((K) "four"));
        assertThrows(UnsupportedOperationException.class, () -> keys.remove("four"));
        assertThrows(UnsupportedOperationException.class, keys::clear);

        final Iterator<K> it = keys.iterator();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUnmodifiableKeySet() {
        resetFull();
        final Set<K> keySet = getMap().keySet();
        assertThrows(UnsupportedOperationException.class, () -> keySet.add((K) "four"));
        assertThrows(UnsupportedOperationException.class, () -> keySet.remove("four"));
        assertThrows(UnsupportedOperationException.class, keySet::clear);

        final Iterator<K> it = keySet.iterator();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUnmodifiableValues() {
        resetFull();
        final Collection<V> values = getMap().values();
        assertThrows(UnsupportedOperationException.class, () -> values.add((V) "four"));
        assertThrows(UnsupportedOperationException.class, () -> values.remove("four"));
        assertThrows(UnsupportedOperationException.class, values::clear);

        final Iterator<V> it = values.iterator();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

//    void testCreate() throws Exception {
//        writeExternalFormToDisk((java.io.Serializable) makeObject(),
//                "src/test/resources/org/apache/commons/collections4/data/test/DeepUnmodifiableMultiValuedMap.emptyCollection.version4.6.1.obj");
//        writeExternalFormToDisk((java.io.Serializable) makeFullMap(),
//                "src/test/resources/org/apache/commons/collections4/data/test/DeepUnmodifiableMultiValuedMap.fullCollection.version4.6.1.obj");
//    }

}
