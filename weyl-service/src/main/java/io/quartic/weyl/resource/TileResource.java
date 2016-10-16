package io.quartic.weyl.resource;

import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.io.ParseException;
import io.dropwizard.jersey.caching.CacheControl;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.model.AbstractIndexedLayer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.render.VectorTileRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import java.io.IOException;

@Path("/")
public class TileResource {
    private static final Logger log = LoggerFactory.getLogger(TileResource.class);
    private final LayerStore layerStore;

    public TileResource(LayerStore layerStore) {
        this.layerStore = layerStore;
    }

    @GET
    @Produces("application/protobuf")
    @Path("/{layerId}/{z}/{x}/{y}.pbf")
    @CacheControl(maxAge = 60*60)
    public byte[] protobuf(@PathParam("layerId") String layerId,
                           @PathParam("z") Integer z,
                           @PathParam("x") Integer x,
                           @PathParam("y") Integer y) throws ParseException, IOException {
        AbstractIndexedLayer layer = layerStore.get(LayerId.of(layerId))
                .orElseThrow(() -> new NotFoundException("No layer with id: " + layerId));

        return new VectorTileRenderer(ImmutableList.of(layer))
                .render(z, x, y);
    }
}
