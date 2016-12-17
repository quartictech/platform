package io.quartic.weyl.resource;

import com.google.common.collect.Maps;
import io.dropwizard.jersey.caching.CacheControl;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.render.VectorTileRenderer;
import org.immutables.value.Value;
import rx.Observable;
import rx.Subscription;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.Map;
import java.util.NoSuchElementException;

import static io.quartic.common.rx.RxUtils.latest;
import static rx.Observable.error;

@Path("/")
@SweetStyle
@Value.Immutable
public abstract class TileResource {
    private final Map<LayerId, Observable<Snapshot>> sequences = Maps.newConcurrentMap();

    protected abstract Observable<LayerSnapshotSequence> snapshotSequences();

    @Value.Default
    protected VectorTileRenderer renderer() {
        return new VectorTileRenderer();
    }

    @Value.Derived
    protected Subscription sequenceSubscription() {
        return snapshotSequences().subscribe(s -> sequences.put(s.spec().id(), s.snapshots()));
    }

    @GET
    @Produces("application/protobuf")
    @Path("/{layerId}/{z}/{x}/{y}.pbf")
    @CacheControl(maxAge = 60*60)
    public byte[] render(@PathParam("layerId") LayerId layerId,
                         @PathParam("z") Integer z,
                         @PathParam("x") Integer x,
                         @PathParam("y") Integer y) {

        final byte[] data = renderer().render(getLayer(layerId), z, x, y);
        return (data.length > 0) ? data : null;
    }

    private Layer getLayer(LayerId id) {
        try {
            return latest(getOrError(id)).absolute();
        } catch (NoSuchElementException e) {
            throw new NotFoundException("Layer with id " + id + " was deleted");    // TODO: it would be more graceful to return empty in the case of deletion
        }
    }

    private Observable<Snapshot> getOrError(LayerId id) {
        return sequences.getOrDefault(id, error(new NotFoundException("No layer with id " + id)));
    }
}
