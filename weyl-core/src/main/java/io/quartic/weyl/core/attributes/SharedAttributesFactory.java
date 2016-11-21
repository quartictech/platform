package io.quartic.weyl.core.attributes;

import com.google.common.collect.Maps;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.Attributes;

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.util.Arrays.asList;

public class SharedAttributesFactory {
    private final Map<AttributeName, Integer> indices = newLinkedHashMap();

    public AttributesBuilder attributesBuilder() {
        return new AttributesBuilder();
    }

    public class AttributesBuilder {
        private final Map<AttributeName, Object> attributes = Maps.newHashMap();  // TODO: consider concurrency

        public AttributesBuilder put(String name, Object value) {
            attributes.put(AttributeNameImpl.of(name), value);
            return this;
        }

        public Attributes build() {
            updateIndices();
            final List<Object> values = collectValues();
            return () -> new ViewMap(values);
        }

        private void updateIndices() {
            attributes.forEach((name, value) -> indices.putIfAbsent(name, indices.size()));
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
            final Iterator<AttributeName> nameIterator = indices.keySet().iterator();
            final Iterator<Object> valueIterator = values.iterator();

            return new Iterator<Entry<AttributeName, Object>>() {
                @Override
                public boolean hasNext() {
                    return valueIterator.hasNext(); // indices.size() <= values.size() always
                }

                @Override
                public Entry<AttributeName, Object> next() {
                    return new SimpleImmutableEntry<>(nameIterator.next(), valueIterator.next()); // Orders are the same due to LinkedHashMap for indices
                }
            };
        }

        @Override
        public int size() {
            return values.size();
        }
    }

}
