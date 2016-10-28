package io.quartic.weyl.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.compute.ComputationSpec;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.live.LiveEventId;
import io.quartic.weyl.core.live.LiveImporter;
import io.quartic.weyl.core.live.WebsocketLiveImporter;
import io.quartic.weyl.core.model.AbstractLayer;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.common.uid.UidGenerator;
import io.quartic.weyl.request.LayerUpdateRequest;
import io.quartic.weyl.response.ImmutableLayerResponse;
import io.quartic.weyl.response.LayerResponse;
import org.glassfish.tyrus.client.ClientManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Session;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Path("/layer")
public class LayerResource {
    private static final Logger log = LoggerFactory.getLogger(LayerResource.class);
    private final LayerStore layerStore;
    private final UidGenerator<FeatureId> fidGenerator;
    private final UidGenerator<LiveEventId> eidGenerator;
    private final ObjectMapper objectMapper;

    public LayerResource(LayerStore layerStore, UidGenerator<FeatureId> fidGenerator, UidGenerator<LiveEventId> eidGenerator, ObjectMapper objectMapper) {
        this.layerStore = layerStore;
        this.fidGenerator = fidGenerator;
        this.eidGenerator = eidGenerator;
        this.objectMapper = objectMapper;
    }

    @PUT
    @Path("/compute")
    @Produces(MediaType.APPLICATION_JSON)
    public LayerId createComputedLayer(ComputationSpec computationSpec) {
        Optional<LayerId> computedLayer = layerStore.compute(computationSpec);
        return computedLayer.orElseThrow(() -> new ProcessingException("layer computation failed"));
    }

    @DELETE
    @Path("/live/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void deleteLiveLayer(@PathParam("id") String id) {
        layerStore.deleteLayer(LayerId.of(id));
    }

    @POST
    @Path("/live/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateLiveLayer(@PathParam("id") String id, LayerUpdateRequest request) {
        final LayerId layerId = LayerId.of(id);

        layerStore.createLayer(layerId, request.metadata(), request.viewType().getLayerView());

        try {
            WebsocketLiveImporter.start(new URI(request.url()), layerId, fidGenerator, eidGenerator, layerStore, objectMapper);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    private void validateOrThrow(Stream<Feature> features, Predicate<Feature> predicate, String message) {
        if (features.anyMatch(predicate)) {
            throw new NotAcceptableException(message);
        }
    }

    @GET
    @Path("/metadata/{id}")
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
                .live(layer.live())
                .build();
    }
}
