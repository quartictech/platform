package io.quartic.weyl.resource;

import com.google.common.base.Preconditions;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.compute.BucketSpec;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.live.LiveLayerStore;
import io.quartic.weyl.core.model.AbstractIndexedLayer;
import io.quartic.weyl.core.model.ImmutableLayerStats;
import io.quartic.weyl.core.model.IndexedLayer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.request.LayerUpdateRequest;
import io.quartic.weyl.response.ImmutableLayerResponse;
import io.quartic.weyl.response.LayerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/layer")
public class LayerResource {
    private static final Logger log = LoggerFactory.getLogger(LayerResource.class);
    private final LayerStore layerStore;
    private final LiveLayerStore liveLayerStore;

    public LayerResource(LayerStore layerStore, LiveLayerStore liveLayerStore) {
        this.layerStore = layerStore;
        this.liveLayerStore = liveLayerStore;
    }

    @PUT
    @Path("/compute")
    @Produces(MediaType.APPLICATION_JSON)
    public LayerId createComputedLayer(BucketSpec bucketSpec) {
        Optional<IndexedLayer> bucketLayer = layerStore.bucket(bucketSpec);
        return bucketLayer.map(IndexedLayer::layerId)
                .orElseThrow(() -> new ProcessingException("bucket computation failed"));
    }

    @DELETE
    @Path("/live/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void deleteLiveLayer(@PathParam("id") String id) {
        liveLayerStore.deleteLayer(LayerId.of(id));
    }

    @POST
    @Path("/live/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateLiveLayer(@PathParam("id") String id, LayerUpdateRequest request) {
        final LayerId layerId = LayerId.of(id);

        liveLayerStore.createLayer(layerId, request.metadata(), request.viewType().getLiveLayerView());

        request.events().forEach( event -> {
                    validateOrThrow(event.featureCollection().isPresent() ? event.featureCollection().get().features().stream() : Stream.empty(),
                            f -> !f.id().isPresent(),
                            "Features with missing ID");
                });

        final int numFeatures = liveLayerStore.addToLayer(layerId, request.events());

        log.info("Updated {} features for layerId = {}", numFeatures, id);
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
        return layerStore.get(LayerId.of(id))
                .map(this::createStaticLayerResponse)
                .orElseThrow(() -> new NotFoundException("No layer with id " + id));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<LayerResponse> listLayers(@QueryParam("query") String query) {
        Preconditions.checkNotNull(query);
        return Stream.concat(listStaticLayers(query), listLiveLayers(query)).collect(Collectors.toList());
    }

    private Stream<LayerResponse> listStaticLayers(String query) {
        return layerStore.listLayers()
                .stream()
                .filter(layer -> layer.layer().metadata().name().toLowerCase().contains(query.toLowerCase()))
                .map(this::createStaticLayerResponse);
    }

    private Stream<LayerResponse> listLiveLayers(String query) {
        return liveLayerStore.listLayers()
                .stream()
                .filter(layer -> layer.layer().metadata().name().toLowerCase().contains(query.toLowerCase()))
                .map(this::createLiveLayerResponse);
    }

    private LayerResponse createStaticLayerResponse(AbstractIndexedLayer layer) {
        return ImmutableLayerResponse.builder()
                .id(layer.layerId())
                .metadata(layer.layer().metadata())
                .stats(layer.layerStats())
                .attributeSchema(layer.layer().schema())
                .live(false)
                .build();
    }

    private LayerResponse createLiveLayerResponse(AbstractIndexedLayer layer) {
        return ImmutableLayerResponse.builder()
                .id(layer.layerId())
                .metadata(layer.layer().metadata())
                .stats(ImmutableLayerStats.builder().featureCount(1).build())
                .attributeSchema(layer.layer().schema())
                .live(true)
                .build();
    }
}
