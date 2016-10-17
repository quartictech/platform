package io.quartic.weyl.resource;

import com.google.common.base.Preconditions;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.compute.BucketSpec;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.live.LiveEventId;
import io.quartic.weyl.core.live.LiveImporter;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.core.utils.UidGenerator;
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
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Path("/layer")
public class LayerResource {
    private static final Logger log = LoggerFactory.getLogger(LayerResource.class);
    private final LayerStore layerStore;
    private final UidGenerator<FeatureId> fidGenerator;
    private final UidGenerator<LiveEventId> eidGenerator;

    public LayerResource(LayerStore layerStore, UidGenerator<FeatureId> fidGenerator, UidGenerator<LiveEventId> eidGenerator) {
        this.layerStore = layerStore;
        this.fidGenerator = fidGenerator;
        this.eidGenerator = eidGenerator;
    }

    @PUT
    @Path("/compute")
    @Produces(MediaType.APPLICATION_JSON)
    public LayerId createComputedLayer(BucketSpec bucketSpec) {
        Optional<LayerId> bucketLayer = layerStore.bucket(bucketSpec);
        return bucketLayer.orElseThrow(() -> new ProcessingException("bucket computation failed"));
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

        request.events().forEach( event -> {
                    validateOrThrow(event.featureCollection().isPresent() ? event.featureCollection().get().features().stream() : Stream.empty(),
                            f -> !f.id().isPresent(),
                            "Features with missing ID");
                });

        final LiveImporter importer = new LiveImporter(request.events(), fidGenerator, eidGenerator);

        final int numFeatures = layerStore.addToLayer(layerId, importer);

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
