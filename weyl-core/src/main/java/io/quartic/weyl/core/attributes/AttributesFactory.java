package io.quartic.weyl.core.attributes;

import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.Attributes;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static com.google.common.collect.Lists.newCopyOnWriteArrayList;
import static com.google.common.collect.Maps.newConcurrentMap;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Arrays.asList;

/**
 * Memory-efficient factory for Attributes instances, maintaining only a single copy of the list of attribute names,
 * and interning string values.
 *
 * Each Attributes instance is backed by a (potentially sparse) list, whose order matches that of the corresponding
 * keys in the shared names list.  This is exposed as an immutable Map with O(1) lookup.
 *
 * **Note:** All Attributes instances will have the same set of keys (i.e. the union).  Thus the size() method for any
 * given instance may be larger than the number of values that were supplied when it was constructed (missing values
 * will be returned as null).
 */
public class AttributesFactory {
    /*
     * We need O(1) lookup of name to position in values lists, and this needs to allow for concurrent read/write.
     * Hence a ConcurrentMap.  However, it's unclear how to expose an iterator over indices.keySet() (as we would need
     * in ViewEntrySet) that remains consistent when subsequent writes occur.  Thus we also maintain a
     * CopyOnWriteArrayList.  Updates to this are expensive, but should occur infrequently.
     */
    private final Map<AttributeName, Integer> indices = newConcurrentMap();
    private final List<AttributeName> names = newCopyOnWriteArrayList();

    public AttributesBuilder builder() {
        return new AttributesBuilder();
    }

    public AttributesBuilder builder(Attributes attributes) {
        return new AttributesBuilder(attributes);
    }

    public class AttributesBuilder {
        private final Map<AttributeName, Object> attributes;

        public AttributesBuilder() {
            this.attributes = newHashMap();
        }

        public AttributesBuilder(Attributes attributes) {
            this.attributes = newHashMap(attributes.attributes());
        }

        public AttributesBuilder put(String name, Object value) {
            attributes.put(AttributeNameImpl.of(name), value);
            return this;
        }

        public Attributes build() {
            updateIndicesAndNames();
            return new ViewAttributes(collectValues());
        }

        private void updateIndicesAndNames() {
            // Only place we need synchronisation, due to required atomicity around containsKey/put.
            // All other accesses to indices are read-only, and are safe due to the strictly additive nature of the
            // mutations here.
            synchronized (indices) {
                attributes.forEach((name, value) -> {
                    if (!indices.containsKey(name)) {
                        indices.put(name, names.size());
                        names.add(name);
                    }
                });
            }
        }

        private List<Object> collectValues() {
            final Object[] values = new Object[indices.size()];
            attributes.forEach((name, value) -> values[indices.get(name)] = internIfString(value));
            return asList(values);
        }

        private Object internIfString(Object value) {
            return (value instanceof String) ? ((String)value).intern() : value;
        }
    }

    // Can't be anonymous in AttributesBuilder.build(), because that would maintain a reference to the builder (and thus not let go to storage)
    private class ViewAttributes implements Attributes {
        private final List<Object> values;

        private ViewAttributes(List<Object> values) {
            this.values = values;
        }

        @Override
        public Map<AttributeName, Object> attributes() {
            return new AbstractMap<AttributeName, Object>() {
                @Override
                public Set<Entry<AttributeName, Object>> entrySet() {
                    return new ViewEntrySet();
                }

                @Override
                public boolean containsValue(Object value) {
                    return values.contains(value);
                }

                @Override
                public boolean containsKey(Object key) {
                    return validIndex(indices.get(key));
                }

                @Override
                public Object get(Object key) {
                    final Integer index = indices.get(key);
                    return (validIndex(index)) ? values.get(index) : null;
                }

                private boolean validIndex(Integer index) {
                    return (index != null) && (index < values.size());
                }
            };
        }

        private class ViewEntrySet extends AbstractSet<Entry<AttributeName, Object>> {
            @Override
            public Iterator<Entry<AttributeName, Object>> iterator() {
                final Iterator<AttributeName> nameIterator = names.iterator();
                final Iterator<Object> valueIterator = values.iterator();

                return new Iterator<Entry<AttributeName, Object>>() {
                    @Override
                    public boolean hasNext() {
                        return nameIterator.hasNext();
                    }

                    @Override
                    public Entry<AttributeName, Object> next() {
                        return new SimpleImmutableEntry<>(nameIterator.next(), valueIterator.hasNext() ? valueIterator.next() : null); // Orders are guaranteed to be the same
                    }
                };
            }

            @Override
            public int size() {
                return names.size();
            }
        }
    }
}
