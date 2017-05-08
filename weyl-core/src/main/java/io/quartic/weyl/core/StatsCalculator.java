package io.quartic.weyl.core;

import io.quartic.weyl.core.feature.FeatureCollection;
import io.quartic.weyl.core.model.Attribute;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeStats;
import io.quartic.weyl.core.model.AttributeType;
import io.quartic.weyl.core.model.DynamicSchema;
import io.quartic.weyl.core.model.LayerStats;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static java.lang.Double.MAX_VALUE;
import static java.lang.Double.MIN_VALUE;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.Collectors.toMap;

public class StatsCalculator {
    private static final Set<AttributeType> STATS_ATTRIBUTE_TYPES = EnumSet.of(AttributeType.NUMERIC, AttributeType.TIMESTAMP);

    public static LayerStats calculateStats(DynamicSchema schema, FeatureCollection features) {
        Map<AttributeName, Double> maxNumeric = newHashMap();
        Map<AttributeName, Double> minNumeric = newHashMap();

        features.stream()
                .flatMap(feature -> feature.getAttributes().getAttributes().entrySet().stream())
                .filter(entry -> STATS_ATTRIBUTE_TYPES.contains(getAttribute(schema, entry.getKey()).getType()) && entry.getValue() != null)
                .forEach(entry -> {
                    final double value = Double.valueOf(entry.getValue().toString());
                    maxNumeric.put(entry.getKey(), max(value, maxNumeric.getOrDefault(entry.getKey(), MIN_VALUE)));
                    minNumeric.put(entry.getKey(), min(value, minNumeric.getOrDefault(entry.getKey(), MAX_VALUE)));
                });

        return new LayerStats(schema.getAttributes()
                .keySet()
                .stream()
                .filter(maxNumeric::containsKey)    // Handle case where no features have the attribute
                .collect(toMap(attr -> attr, attr -> new AttributeStats(minNumeric.get(attr), maxNumeric.get(attr))))
        );
    }

    private static Attribute getAttribute(DynamicSchema schema, AttributeName name) {
        if (!schema.getAttributes().containsKey(name)) {
            throw new IllegalStateException("Attribute not present in schema: " + name.getName());
        }
        return schema.getAttributes().get(name);
    }
}