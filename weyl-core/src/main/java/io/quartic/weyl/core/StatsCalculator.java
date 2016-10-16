package io.quartic.weyl.core;

import com.google.common.collect.Maps;
import io.quartic.weyl.core.model.*;

import java.util.Map;

public class StatsCalculator {
    public static LayerStats calculateStats(AbstractLayer layer) {
        Map<String, Double> maxNumeric = Maps.newConcurrentMap();
        Map<String, Double> minNumeric = Maps.newConcurrentMap();

        AttributeSchema attributeSchema = layer.schema();

        layer.features().parallelStream()
                .flatMap(feature -> feature.metadata().entrySet().stream())
                .filter(entry -> attributeSchema.attributes().get(entry.getKey()).type()
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
        layer.schema().attributes()
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().type() == AttributeType.NUMERIC)
                .forEach(entry -> builder.putAttributeStats(entry.getKey(),
                        ImmutableAttributeStats.builder()
                                .minimum(minNumeric.get(entry.getKey()))
                                .maximum(maxNumeric.get(entry.getKey()))
                                .build()));

        builder.featureCount(layer.features().size());

        return builder.build();
    }
}