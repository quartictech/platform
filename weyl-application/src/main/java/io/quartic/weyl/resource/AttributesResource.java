package io.quartic.weyl.resource;

import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.core.model.EntityId;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Path("/attributes")
public class AttributesResource {
    private final EntityStoreQuerier querier;

    public AttributesResource(EntityStoreQuerier querier) {
        this.querier = querier;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<EntityId, Attributes> getAttributes(List<EntityId> entityIds) {
        return querier.retrieveEntitiesOrThrow(entityIds)
                .collect(toMap(AbstractFeature::entityId, this::externalAttributes));
    }

    private Attributes externalAttributes(AbstractFeature feature) {
        final Attributes.Builder builder = Attributes.builder();
        feature.attributes().attributes().entrySet().stream()
                .filter(e -> !(e.getValue() instanceof ComplexAttribute || e.getValue() instanceof Map))
                .forEach(builder::attribute);
        return builder.build();
    }
}
