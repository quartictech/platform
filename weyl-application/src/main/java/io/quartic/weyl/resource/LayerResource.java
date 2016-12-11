package io.quartic.weyl.resource;

import com.google.common.base.Preconditions;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.response.LayerResponse;
import io.quartic.weyl.response.LayerResponseImpl;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;

import static java.util.stream.Collectors.toList;

@Path("/layer")
public class LayerResource {
    private final LayerStore layerStore;

    public LayerResource(LayerStore layerStore) {
        this.layerStore = layerStore;
    }

    @GET
    @Path("/metadata/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public LayerResponse getLayer(@PathParam("id") LayerId id) {
        return layerStore.getLayer(id)
                .map(this::createLayerResponse)
                .orElseThrow(() -> new NotFoundException("No layer with id " + id));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<LayerResponse> listLayers(@QueryParam("query") String query) {
        Preconditions.checkNotNull(query);
        return layerStore.listLayers()
                .stream()
                .filter(layer -> layer.metadata().name().toLowerCase().contains(query.toLowerCase()) && !layer.features().isEmpty())
                .map(this::createLayerResponse)
                .collect(toList());
    }

    private LayerResponse createLayerResponse(Layer layer) {
        return LayerResponseImpl.builder()
                .id(layer.layerId())
                .metadata(layer.metadata())
                .stats(layer.layerStats())
                .attributeSchema(layer.schema())
                .live(!layer.indexable())
                .build();
    }
}
