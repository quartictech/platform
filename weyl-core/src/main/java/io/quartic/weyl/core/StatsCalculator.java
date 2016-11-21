package io.quartic.weyl.core;

import io.quartic.weyl.core.feature.FeatureCollection;
import io.quartic.weyl.core.model.*;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static java.lang.Double.MAX_VALUE;
import static java.lang.Double.MIN_VALUE;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class StatsCalculator {
    public static LayerStats calculateStats(AttributeSchema schema, FeatureCollection features) {
        Map<AttributeName, Double> maxNumeric = newHashMap();
        Map<AttributeName, Double> minNumeric = newHashMap();

        features.stream()
                .flatMap(feature -> feature.attributes().attributes().entrySet().stream())
                .filter(entry -> getAttribute(schema, entry.getKey()).type() == AttributeType.NUMERIC)
                .forEach(entry -> {
                    System.out.println("XXX: " + entry.getKey() + " -> " + entry.getValue());
                    final double value = Double.valueOf(entry.getValue().toString());
                    maxNumeric.put(entry.getKey(), max(value, maxNumeric.getOrDefault(entry.getKey(), MIN_VALUE)));
                    minNumeric.put(entry.getKey(), min(value, minNumeric.getOrDefault(entry.getKey(), MAX_VALUE)));
                });

        LayerStatsImpl.Builder builder = LayerStatsImpl.builder();
        schema.attributes()
                .entrySet()
                .stream()
                .filter(entry -> maxNumeric.containsKey(entry.getKey()))    // Handle case where no features have the attribute
                .forEach(entry -> builder.attributeStat(
                        entry.getKey(),
                        AttributeStatsImpl.of(minNumeric.get(entry.getKey()), maxNumeric.get(entry.getKey()))
                ));

        builder.featureCount(features.size());
        return builder.build();
    }

    private static Attribute getAttribute(AttributeSchema schema, AttributeName name) {
        if (!schema.attributes().containsKey(name)) {
            throw new IllegalStateException("Attribute not present in schema: " + name.name());
        }
        return schema.attributes().get(name);
    }
}