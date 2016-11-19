package io.quartic.weyl.chart;

import io.quartic.weyl.UpdateMessageGenerator;
import io.quartic.weyl.core.attributes.TimeSeriesAttribute;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.AttributeName;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public class ChartUpdateGenerator implements UpdateMessageGenerator {
    private static final AttributeName NAME = AttributeName.of("name");

    public ChartUpdateMessage generate(Collection<AbstractFeature> entities) {
        Set<AttributeName> eligibleAttributes = entities.stream()
                .filter(feature -> feature.attributes().attributes().containsKey(NAME))
                .flatMap(feature -> feature.attributes().attributes().entrySet().stream())
                .filter(entry -> entry.getValue() instanceof TimeSeriesAttribute)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        final ChartUpdateMessage.Builder builder = ChartUpdateMessage.builder();
        eligibleAttributes.forEach(attr -> builder.timeseries(attr, timeSeriesForAttribute(entities, attr)));
        return builder.build();
    }

    // Map of { name -> timeseries }
    private Map<String, TimeSeriesAttribute> timeSeriesForAttribute(Collection<AbstractFeature> features, AttributeName attribute) {
        return features.stream()
                .filter(feature -> feature.attributes().attributes().containsKey(NAME))
                .filter(feature -> feature.attributes().attributes().containsKey(attribute) &&
                        feature.attributes().attributes().get(attribute) instanceof TimeSeriesAttribute)
                .collect(toMap(
                        feature -> (String) feature.attributes().attributes().get(NAME),
                        feature -> (TimeSeriesAttribute) feature.attributes().attributes().get(attribute)));
    }
}
