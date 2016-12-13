package io.quartic.weyl.resource;

import com.google.common.collect.ImmutableList;
import io.dropwizard.jersey.caching.CacheControl;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.render.VectorTileRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.NoSuchElementException;

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
    public byte[] protobuf(@PathParam("layerId") LayerId layerId,
                             @PathParam("z") Integer z,
                             @PathParam("x") Integer x,
                             @PathParam("y") Integer y) {

        try {
            return layerStore.layer(layerId)
                    .first()
                    .map(layer -> {
                        final byte[] data = new VectorTileRenderer(ImmutableList.of(layer)).render(z, x, y);
                        return (data.length > 0) ? data : null;
                    })
                    .toBlocking()
                    .single();
        } catch (NoSuchElementException e) {
            throw new NotFoundException("No layer with id " + layerId);
        }
    }
}
