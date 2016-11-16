package io.quartic.weyl.resource;

import com.google.common.base.Preconditions;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.compute.ComputationSpec;
import io.quartic.weyl.core.model.AbstractLayer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.response.ImmutableLayerResponse;
import io.quartic.weyl.response.LayerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Path("/layer")
public class LayerResource {
    private static final Logger log = LoggerFactory.getLogger(LayerResource.class);
    private final LayerStore layerStore;

    public LayerResource(LayerStore layerStore) {
        this.layerStore = layerStore;
    }

    @PUT
    @Path("/compute")
    @Produces(MediaType.APPLICATION_JSON)
    public LayerId createComputedLayer(ComputationSpec computationSpec) {
        Optional<LayerId> computedLayer = layerStore.compute(computationSpec);
        return computedLayer.orElseThrow(() -> new ProcessingException("layer computation failed"));
    }

    @GET
    @Path("/attributes/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public LayerResponse getLayer(@PathParam("id") String id) {
        return layerStore.getLayer(LayerId.of(id))
                .map(this::createLayerResponse)
                .orElseThrow(() -> new NotFoundException("No layer with id " + id));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<LayerResponse> listLayers(@QueryParam("query") String query) {
        Preconditions.checkNotNull(query);
        return layerStore.listLayers()
                .stream()
                .filter(layer -> layer.metadata().name().toLowerCase().contains(query.toLowerCase()))
                .map(this::createLayerResponse)
                .collect(toList());
    }

    private LayerResponse createLayerResponse(AbstractLayer layer) {
        return ImmutableLayerResponse.builder()
                .id(layer.layerId())
                .metadata(layer.metadata())
                .stats(layer.layerStats())
                .attributeSchema(layer.schema())
                .live(!layer.indexable())
                .build();
    }
}
