package io.quartic.weyl.resource;

import com.google.common.base.Preconditions;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.compute.BucketSpec;
import io.quartic.weyl.core.model.ImmutableLayerMetadata;
import io.quartic.weyl.core.model.IndexedLayer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.request.PostgisImportRequest;
import io.quartic.weyl.response.ImmutableLayerResponse;
import io.quartic.weyl.response.LayerResponse;

import javax.ws.rs.*;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("/layer")
public class LayerResource {
   private final LayerStore layerStore;

   public LayerResource(LayerStore layerStore) {
      this.layerStore = layerStore;
   }

   @PUT
   @Path("/import")
   @Produces("application/json")
   @Consumes("application/json")
   public LayerId importLayer(PostgisImportRequest request) {
      Preconditions.checkNotNull(request.name());
      Preconditions.checkNotNull(request.description());
      LayerMetadata metadata = ImmutableLayerMetadata.builder()
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
   @Produces("application/json")
   public Collection<LayerResponse> listLayers(@QueryParam("query") String query) {
      Preconditions.checkNotNull(query);
        return layerStore.listLayers()
                .stream()
                .filter(layer -> layer.layer().metadata().name().contains(query))
                .map(layer -> ImmutableLayerResponse.builder()
                        .name(layer.layer().metadata().name())
                        .description(layer.layer().metadata().description())
                        .id(layer.layerId())
                        .build())
                .collect(Collectors.toList());
   }


}
