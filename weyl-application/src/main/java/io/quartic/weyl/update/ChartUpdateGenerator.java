package io.quartic.weyl.update;

import io.quartic.weyl.core.attributes.TimeSeriesAttribute;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.AttributeName;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public class ChartUpdateGenerator implements SelectionDrivenUpdateGenerator {
    private static final AttributeName NAME = AttributeName.of("name");

    @Override
    public String name() {
        return "chart";
    }

    public Map<AttributeName, Map<String, TimeSeriesAttribute>> generate(Collection<AbstractFeature> entities) {
        Set<AttributeName> eligibleAttributes = entities.stream()
                .filter(feature -> feature.attributes().attributes().containsKey(NAME))
                .flatMap(feature -> feature.attributes().attributes().entrySet().stream())
                .filter(entry -> entry.getValue() instanceof TimeSeriesAttribute)
                .map(Map.Entry::getKey)
                .collect(toSet());

        return eligibleAttributes.stream()
                .collect(toMap(identity(), attr -> timeSeriesForAttribute(entities, attr)));
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
