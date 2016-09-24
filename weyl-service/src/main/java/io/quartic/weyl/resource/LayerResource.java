package io.quartic.weyl.resource;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.compute.BucketSpec;
import io.quartic.weyl.core.geojson.*;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.live.LiveLayer;
import io.quartic.weyl.core.live.LiveLayerStore;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.request.LayerCreateRequest;
import io.quartic.weyl.request.PostgisImportRequest;
import io.quartic.weyl.response.ImmutableLayerResponse;
import io.quartic.weyl.response.LayerResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
    @Path("/import")
    @Produces("application/json")
    @Consumes("application/json")
    public LayerId importLayer(PostgisImportRequest request) {
        Preconditions.checkNotNull(request.name());
        Preconditions.checkNotNull(request.description());
        AbstractLayerMetadata metadata = LayerMetadata.builder()
                .name(request.name())
                .description(request.description())
                .build();
        Optional<IndexedLayer> layer = layerStore.importPostgis(metadata, request.query());
        return layer.orElseThrow(() -> new NotFoundException("Error importing layer"))
                .layerId();
    }

    @PUT
    @Path("/compute")
    @Produces("application/json")
    public LayerId createComputedLayer(BucketSpec bucketSpec) {
        Optional<IndexedLayer> bucketLayer = layerStore.bucket(bucketSpec);
        return bucketLayer.map(IndexedLayer::layerId)
                .orElseThrow(() -> new ProcessingException("bucket computation failed"));
    }

    @GET
    @Path("/numeric_values/{id}")
    @Produces("application/json")
    public Map<String, Double[]> numericValues(@PathParam("id") String id) {
        IndexedLayer indexedLayer = layerStore.get(LayerId.of(id))
                .orElseThrow(() -> new NotFoundException("no layer with id " + id));

        List<String> numericAttributes = indexedLayer.layer().schema()
                .attributes()
                .entrySet().stream()
                .filter( entry -> entry.getValue().type() == AttributeType.NUMERIC)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Map<String, Double[]> result = Maps.newHashMap();
        for (String attributeName : numericAttributes) {
            result.put(attributeName, new Double[indexedLayer.indexedFeatures().size()]);
        }

        int index = 0;
        for (IndexedFeature feature : indexedLayer.indexedFeatures()) {
            for (Map.Entry<String, Optional<Object>> entry : feature.feature().metadata().entrySet()) {

                if (! result.containsKey(entry.getKey())) {
                    continue;
                }

                Double assignValue = null;
                if (entry.getValue().isPresent()) {
                    Object value = entry.getValue().get();
                    if (value instanceof Double || value instanceof Float) {
                        assignValue = (double) value;
                    }
                    else if (value instanceof Long) {
                        assignValue = ((Long) value).doubleValue();
                    }
                    else if (value instanceof Integer) {
                        assignValue = ((Integer) value).doubleValue();
                    }
                }

                result.get(entry.getKey())[index] = assignValue;
            }
            index += 1;
        }

        return result;
    }

    @PUT
    @Path("/live")
    @Produces("application/json")
    public LayerId createLiveLayer(LayerCreateRequest request) {
        return liveLayerStore.createLayer(LayerMetadata.of(request.name(), request.description()));
    }

    @GET
    @Path("/live/{id}")
    @Produces("application/json")
    public FeatureCollection getLiveFeatures(@PathParam("id") String id) {
        return liveLayerStore.getFeaturesForLayer(LayerId.of(id));
    }

    @POST
    @Path("/live/{id}")
    @Consumes("application/json")
    public void updateLiveLayer(@PathParam("id") String id, FeatureCollection collection) {
        final LayerId layerId = LayerId.of(id);

        if (liveLayerStore.listLayers().stream().noneMatch(l -> l.layerId().equals(layerId))) {
            throw new NotFoundException("No live layer with id " + id);
        }

        validateOrThrow(collection,
                f -> !f.id().isPresent(),
                "Features with missing ID");
        validateOrThrow(collection,
                f -> !f.properties().containsKey("timestamp"),
                "Features with missing timestamp");
        validateOrThrow(collection,
                f -> !StringUtils.isNumeric(f.properties().get("timestamp").toString()),
                "Features with non-numeric timestamp");

        if (collection.features()
                .stream()
                .map(Feature::id)
                .distinct()
                .count() != collection.features().size()) {
            throw new NotAcceptableException("Features with duplicate IDs");
        }

        liveLayerStore.addToLayer(layerId, collection);

        log.info("Updated {} features for layerId = {}", collection.features().size(), id);
    }

    private void validateOrThrow(FeatureCollection collection, Predicate<Feature> predicate, String message) {
        if (collection.features()
                .stream()
                .anyMatch(predicate)) {
            throw new NotAcceptableException(message);
        }
    }

    @GET
    @Path("/metadata/{id}")
    @Produces("application/json")
    public LayerResponse getLayer(@PathParam("id") String id) {
        return layerStore.get(LayerId.of(id))
                .map(this::createStaticLayerResponse)
                .orElseThrow(() -> new NotFoundException("No layer with id " + id));
    }

    @GET
    @Produces("application/json")
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

    private LayerResponse createStaticLayerResponse(IndexedLayer layer) {
        return ImmutableLayerResponse.builder()
                .name(layer.layer().metadata().name())
                .description(layer.layer().metadata().description())
                .id(layer.layerId())
                .stats(layer.layerStats())
                .attributeSchema(layer.layer().schema())
                .live(false)
                .build();
    }

    private LayerResponse createLiveLayerResponse(LiveLayer layer) {
        return ImmutableLayerResponse.builder()
                .name(layer.layer().metadata().name())
                .description(layer.layer().metadata().description())
                .id(layer.layerId())
                .stats(ImmutableLayerStats.builder().featureCount(1).build())
                .attributeSchema(layer.layer().schema())
                .live(true)
                .build();
    }
}
