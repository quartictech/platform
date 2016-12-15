package io.quartic.weyl.resource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import io.dropwizard.jersey.caching.CacheControl;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.render.VectorTileRenderer;
import rx.Observable;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.Map;

import static io.quartic.common.rx.RxUtils.latest;
import static rx.Observable.error;

@Path("/")
public class TileResource {
    private final Map<LayerId, Observable<Snapshot>> sequences = Maps.newConcurrentMap();

    public TileResource(Observable<LayerSnapshotSequence> snapshotSequences) {
        snapshotSequences.subscribe(s -> sequences.put(s.id(), s.snapshots()));
    }

    @GET
    @Produces("application/protobuf")
    @Path("/{layerId}/{z}/{x}/{y}.pbf")
    @CacheControl(maxAge = 60*60)
    public byte[] protobuf(@PathParam("layerId") LayerId layerId,
                             @PathParam("z") Integer z,
                             @PathParam("x") Integer x,
                             @PathParam("y") Integer y) {

        final Layer layer = latest(getOrError(layerId)).absolute();
        final byte[] data = new VectorTileRenderer(ImmutableList.of(layer)).render(z, x, y);
        return (data.length > 0) ? data : null;
    }

    private Observable<Snapshot> getOrError(LayerId id) {
        return sequences.getOrDefault(id, error(new NotFoundException("No layer with id " + id)));
    }
}
