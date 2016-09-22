package io.quartic.weyl.resource;

import io.quartic.weyl.core.geojson.AbstractFeatureCollection;
import io.quartic.weyl.core.live.LiveLayerStore;

import javax.ws.rs.*;

@Path("/livelayer")
public class LiveLayerResource {
    private final LiveLayerStore store;

    public LiveLayerResource(LiveLayerStore store) {
        this.store = store;
    }

    @GET
    @Path("{id}")
    @Produces("application/json")
    public AbstractFeatureCollection getLayer(@PathParam("id") String id) {
        return store.getFeaturesForLayer(id)
                .orElseThrow(() -> new NotFoundException("No layer with id " + id));
    }

}
