package io.quartic.common.test;

import com.google.common.collect.Maps;

import java.util.AbstractMap;
import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableMap;

public final class CollectionUtils {
    private CollectionUtils() {}
    
    public static <K, V> Map<K, V> map(Map.Entry<K, V>... entries) {
        final Map<K, V> map = Maps.newHashMap();
        stream(entries).forEach(e -> map.put(e.getKey(), e.getValue()));
        return unmodifiableMap(map);
    }

    public static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }
}
