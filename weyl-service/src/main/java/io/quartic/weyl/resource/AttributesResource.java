package io.quartic.weyl.resource;

import io.quartic.weyl.core.attributes.TimeSeriesAttribute;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/attributes")
public class AttributesResource {
    private final FeatureStore featureStore;

    public AttributesResource(FeatureStore featureStore) {
        this.featureStore = featureStore;
    }

    @POST
    @Path("/time_series")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Map<String, TimeSeriesAttribute>> timeSeriesAttributes(List<FeatureId> featureIds) {
        Collection<Feature> features = featureIds.stream()
                .map(featureStore::get)
                .collect(Collectors.toSet());

        Set<String> eligibleAttributes = features.stream()
                .flatMap(feature -> feature.metadata().entrySet().stream())
                .filter(entry -> entry.getValue() instanceof TimeSeriesAttribute)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        return eligibleAttributes.stream()
                .collect(Collectors.toMap(Function.identity(), attribute -> timeSeriesForAttribute(features, attribute)));
    }

    private Map<String, TimeSeriesAttribute> timeSeriesForAttribute(Collection<Feature> features, String attribute) {
        return features.stream()
                .filter(feature -> feature.metadata().containsKey("name"))
                .filter(feature -> feature.metadata().containsKey(attribute) &&
                        feature.metadata().get(attribute) instanceof TimeSeriesAttribute)
                .collect(Collectors.toMap(feature -> (String) feature.metadata().get("name"),
                        feature -> (TimeSeriesAttribute) feature.metadata().get(attribute)));
    }
}
