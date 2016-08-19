package io.quartic.weyl.resource;

import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.compute.BucketSpec;
import io.quartic.weyl.core.model.IndexedLayer;
import io.quartic.weyl.core.model.Layer;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import java.util.Optional;

@Path("/computation")
public class ComputationResource {
   private final LayerStore layerStore;

   public ComputationResource(LayerStore layerStore) {
      this.layerStore = layerStore;
   }

   @PUT
   void createComputedLayer(BucketSpec bucketSpec) {
      Optional<IndexedLayer> bucketLayer = layerStore.bucket(bucketSpec);
   }
}
