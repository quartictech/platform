package io.quartic.weyl.resource;

import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.attributes.TimeSeriesAttribute;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Path("/attributes")
public class AttributesResource {
    private final FeatureStoreQuerier querier;

    public AttributesResource(FeatureStoreQuerier querier) {
        this.querier = querier;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<FeatureId, Map<String, Object>> getAttributes(List<FeatureId> featureIds) {
        return querier.retrieveFeaturesOrThrow(featureIds)
                .collect(toMap(Feature::uid, this::externalAttributes));
    }

    private Map<String, Object> externalAttributes(Feature feature) {
        return feature.metadata().entrySet().stream()
                .filter(e -> !(e.getValue() instanceof ComplexAttribute || e.getValue() instanceof Map))
                .collect(toMap(Entry::getKey, Entry::getValue));
    }

    @POST
    @Path("/time-series")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Map<String, TimeSeriesAttribute>> timeSeriesAttributes(List<FeatureId> featureIds) {
        Collection<Feature> features = querier.retrieveFeaturesOrThrow(featureIds).collect(toList());

        Set<String> eligibleAttributes = features.stream()
                .filter(feature -> feature.metadata().containsKey("name"))
                .flatMap(feature -> feature.metadata().entrySet().stream())
                .filter(entry -> entry.getValue() instanceof TimeSeriesAttribute)
                .map(Entry::getKey)
                .collect(Collectors.toSet());

        return eligibleAttributes.stream()
                .collect(toMap(Function.identity(), attribute -> timeSeriesForAttribute(features, attribute)));
    }

    private Map<String, TimeSeriesAttribute> timeSeriesForAttribute(Collection<Feature> features, String attribute) {
        return features.stream()
                .filter(feature -> feature.metadata().containsKey("name"))
                .filter(feature -> feature.metadata().containsKey(attribute) &&
                        feature.metadata().get(attribute) instanceof TimeSeriesAttribute)
                .collect(toMap(feature -> (String) feature.metadata().get("name"),
                        feature -> (TimeSeriesAttribute) feature.metadata().get(attribute)));
    }
}
