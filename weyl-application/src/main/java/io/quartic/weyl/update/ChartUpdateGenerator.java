package io.quartic.weyl.update;

import io.quartic.weyl.core.attributes.TimeSeriesAttribute;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.Feature;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public class ChartUpdateGenerator implements SelectionDrivenUpdateGenerator {
    private static final AttributeName NAME = new AttributeName("name");

    @Override
    public String name() {
        return "chart";
    }

    public Map<AttributeName, Map<String, TimeSeriesAttribute>> generate(Collection<Feature> entities) {
        Set<AttributeName> eligibleAttributes = entities.stream()
                .filter(feature -> feature.getAttributes().getAttributes().containsKey(NAME))
                .flatMap(feature -> feature.getAttributes().getAttributes().entrySet().stream())
                .filter(entry -> entry.getValue() instanceof TimeSeriesAttribute)
                .map(Map.Entry::getKey)
                .collect(toSet());

        return eligibleAttributes.stream()
                .collect(toMap(identity(), attr -> timeSeriesForAttribute(entities, attr)));
    }

    // Map of { name -> timeseries }
    private Map<String, TimeSeriesAttribute> timeSeriesForAttribute(Collection<Feature> features, AttributeName attribute) {
        return features.stream()
                .filter(feature -> feature.getAttributes().getAttributes().containsKey(NAME))
                .filter(feature -> feature.getAttributes().getAttributes().containsKey(attribute) &&
                        feature.getAttributes().getAttributes().get(attribute) instanceof TimeSeriesAttribute)
                .collect(toMap(
                        feature -> (String) feature.getAttributes().getAttributes().get(NAME),
                        feature -> (TimeSeriesAttribute) feature.getAttributes().getAttributes().get(attribute)));
    }
}
