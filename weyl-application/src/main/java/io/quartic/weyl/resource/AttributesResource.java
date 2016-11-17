package io.quartic.weyl.resource;

import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.attributes.TimeSeriesAttribute;
import io.quartic.weyl.core.model.*;

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
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Path("/attributes")
public class AttributesResource {
    private static final AttributeName NAME = AttributeName.of("name");
    private final EntityStoreQuerier querier;

    public AttributesResource(EntityStoreQuerier querier) {
        this.querier = querier;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<FeatureId, Attributes> getAttributes(List<EntityId> entityIds) {
        return querier.retrieveEntitiesOrThrow(entityIds)
                .collect(toMap(AbstractFeature::uid, this::externalAttributes));
    }

    private Attributes externalAttributes(AbstractFeature feature) {
        final Attributes.Builder builder = Attributes.builder();
        feature.attributes().attributes().entrySet().stream()
                .filter(e -> !(e.getValue() instanceof ComplexAttribute || e.getValue() instanceof Map))
                .forEach(builder::attribute);
        return builder.build();
    }

    @POST
    @Path("/time-series")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<AttributeName, Map<String, TimeSeriesAttribute>> timeSeriesAttributes(List<EntityId> entityIds) {
        Collection<AbstractFeature> entities = querier.retrieveEntitiesOrThrow(entityIds).collect(toList());

        Set<AttributeName> eligibleAttributes = entities.stream()
                .filter(feature -> feature.attributes().attributes().containsKey(NAME))
                .flatMap(feature -> feature.attributes().attributes().entrySet().stream())
                .filter(entry -> entry.getValue() instanceof TimeSeriesAttribute)
                .map(Entry::getKey)
                .collect(Collectors.toSet());

        return eligibleAttributes.stream()
                .collect(toMap(identity(), attribute -> timeSeriesForAttribute(entities, attribute)));
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
