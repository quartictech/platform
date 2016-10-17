package io.quartic.weyl.core;

import com.google.common.collect.Maps;
import io.quartic.weyl.core.feature.FeatureCollection;
import io.quartic.weyl.core.model.*;

import java.util.Map;

public class StatsCalculator {
    public static LayerStats calculateStats(AttributeSchema schema, FeatureCollection features) {
        Map<String, Double> maxNumeric = Maps.newConcurrentMap();
        Map<String, Double> minNumeric = Maps.newConcurrentMap();

        features.parallelStream()
                .flatMap(feature -> feature.metadata().entrySet().stream())
                .filter(entry -> schema.attributes().get(entry.getKey()).type()
                        == AttributeType.NUMERIC)
                .forEach(entry -> {
                    Object value = entry.getValue();
                    double doubleValue = Double.valueOf(value.toString());

                    if (!maxNumeric.containsKey(entry.getKey())) {
                        maxNumeric.put(entry.getKey(), doubleValue);
                    } else if (doubleValue > maxNumeric.get(entry.getKey())) {
                        maxNumeric.put(entry.getKey(), doubleValue);
                    }
                    if (!minNumeric.containsKey(entry.getKey())) {
                        minNumeric.put(entry.getKey(), doubleValue);
                    } else if (doubleValue < minNumeric.get(entry.getKey())) {
                        minNumeric.put(entry.getKey(), doubleValue);
                    }
                });

        ImmutableLayerStats.Builder builder = ImmutableLayerStats.builder();
        schema.attributes()
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().type() == AttributeType.NUMERIC)
                .forEach(entry -> builder.putAttributeStats(entry.getKey(),
                        ImmutableAttributeStats.builder()
                                .minimum(minNumeric.get(entry.getKey()))
                                .maximum(maxNumeric.get(entry.getKey()))
                                .build()));

        builder.featureCount(features.size());

        return builder.build();
    }
}