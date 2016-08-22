package io.quartic.weyl.resource;

import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.compute.BucketSpec;
import io.quartic.weyl.core.model.IndexedLayer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.request.PostgisImportRequest;

import javax.ws.rs.*;
import java.util.Optional;

@Path("/layer")
public class LayerResource {
   private final LayerStore layerStore;

   public LayerResource(LayerStore layerStore) {
      this.layerStore = layerStore;
   }

   @PUT
   @Path("/import/{name}")
   @Produces("application/json")
   public LayerId importLayer(@PathParam("name") String name, PostgisImportRequest request) {
      Optional<IndexedLayer> layer = layerStore.importPostgis(name, request.query());
      return layer.orElseThrow(() -> new NotFoundException("Error importing layer"))
              .layerId();
   }

   @PUT
   @Path("/compute")
   @Produces("application/json")
   public void createComputedLayer(BucketSpec bucketSpec) {
      Optional<IndexedLayer> bucketLayer = layerStore.bucket(bucketSpec);
   }
}
