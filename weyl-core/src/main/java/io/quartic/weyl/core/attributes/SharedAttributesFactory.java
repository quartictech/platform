package io.quartic.weyl.core.attributes;

import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.Attributes;

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import static com.google.common.collect.Lists.newCopyOnWriteArrayList;
import static com.google.common.collect.Maps.newConcurrentMap;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Arrays.asList;

public class SharedAttributesFactory {
    /*
     * We need O(1) lookup of name to position in values lists, and this needs to allow for concurrent read/write.
     * Hence a ConcurrentMap.  However, it's unclear how to expose an iterator over indices.keySet() (as we would need
     * in ViewEntrySet) that remains consistent when subsequent writes occur.  Thus we also maintain a
     * CopyOnWriteArrayList.  Updates to this are expensive, but should occur infrequently.
     *
     * Thus we don't need any explicit locking anywhere in this code.
     */
    private final Map<AttributeName, Integer> indices = newConcurrentMap();
    private final List<AttributeName> names = newCopyOnWriteArrayList();

    public AttributesBuilder attributesBuilder() {
        return new AttributesBuilder();
    }

    public class AttributesBuilder {
        private final Map<AttributeName, Object> attributes = newHashMap();

        public AttributesBuilder put(String name, Object value) {
            attributes.put(AttributeNameImpl.of(name), value);
            return this;
        }

        public Attributes build() {
            updateIndicesAndNames();
            final List<Object> values = collectValues();
            return () -> new ViewMap(values);
        }

        private void updateIndicesAndNames() {
            attributes.forEach((name, value) -> {
                if (!indices.containsKey(name)) {
                    indices.put(name, names.size());
                    names.add(name);
                }
            });
        }

        private List<Object> collectValues() {
            final Object[] values = new Object[indices.size()];
            attributes.forEach((name, value) -> values[indices.get(name)] = value);
            return asList(values);
        }
    }

    private class ViewMap extends AbstractMap<AttributeName, Object> {
        private final List<Object> values;

        public ViewMap(List<Object> values) {
            this.values = values;
        }

        @Override
        public Set<Entry<AttributeName, Object>> entrySet() {
            return new ViewEntrySet(values);
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
    }

    private class ViewEntrySet extends AbstractSet<Entry<AttributeName, Object>> {
        private final List<Object> values;

        private ViewEntrySet(List<Object> values) {
            this.values = values;
        }

        @Override
        public Iterator<Entry<AttributeName, Object>> iterator() {
            final Iterator<AttributeName> nameIterator = names.iterator();
            final Iterator<Object> valueIterator = values.iterator();

            return new Iterator<Entry<AttributeName, Object>>() {
                @Override
                public boolean hasNext() {
                    return valueIterator.hasNext(); // names.size() >= values.size() always
                }

                @Override
                public Entry<AttributeName, Object> next() {
                    return new SimpleImmutableEntry<>(nameIterator.next(), valueIterator.next()); // Orders are guaranteed to be the same
                }
            };
        }

        @Override
        public int size() {
            return values.size();
        }
    }

}
