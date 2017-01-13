package io.quartic.weyl.resource;

import com.google.common.collect.Maps;
import io.dropwizard.jersey.caching.CacheControl;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.render.VectorTileRenderer;
import org.glassfish.jersey.server.ManagedAsync;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.schedulers.Schedulers;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.Map;

import static io.quartic.common.rx.RxUtilsKt.accumulateMap;
import static io.quartic.common.rx.RxUtilsKt.latest;
import static io.quartic.common.rx.RxUtilsKt.likeBehavior;
import static rx.Observable.error;

@Path("/")
@SweetStyle
@Value.Immutable
public abstract class TileResource {
    private static final Logger LOG = LoggerFactory.getLogger(TileResource.class);

    protected abstract Observable<LayerSnapshotSequence> snapshotSequences();

    @Value.Default
    protected VectorTileRenderer renderer() {
        return new VectorTileRenderer();
    }

    @Value.Derived
    private Scheduler scheduler(){
      return Schedulers.computation();
    }

    @Value.Derived
    protected Observable<Map<LayerId, LayerSnapshotSequence>> layers() {
        // Don't do anything smart with deleted layers
        return snapshotSequences()
                .compose(accumulateMap(snapshot -> snapshot.spec().id(), snapshot -> snapshot))
                .compose(likeBehavior());
    }

    @GET
    @Produces("application/protobuf")
    @Path("/{layerId}/{z}/{x}/{y}.pbf")
    @CacheControl(maxAge = 60*60)
    @ManagedAsync
    public void render(@PathParam("layerId") LayerId layerId,
                         @PathParam("z") Integer z,
                         @PathParam("x") Integer x,
                         @PathParam("y") Integer y,
                         @Suspended AsyncResponse asyncResponse) {

        layers().subscribeOn(scheduler())
                .map(layers -> renderer().render(latest(getOrError(layers, layerId)).absolute(), z, x, y))
                .first()
                .subscribe(data -> {
                    LOG.info("returned data: {}", data.length);
                    if (data.length > 0) {
                        asyncResponse.resume(data);
                    } else {
                        asyncResponse.resume(Response.noContent().build());
                    }
                }, asyncResponse::resume);
    }

    private Observable<Snapshot> getOrError(Map<LayerId, LayerSnapshotSequence> layers, LayerId id) {
        if (layers.containsKey(id)) {
            return layers.get(id).snapshots();
        }
        else {
            return error(new NotFoundException("No layer with id " + id));
        }
    }
}
