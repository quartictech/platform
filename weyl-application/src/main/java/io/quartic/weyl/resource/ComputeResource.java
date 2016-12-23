package io.quartic.weyl.resource;

import io.quartic.common.SweetStyle;
import io.quartic.common.uid.UidGenerator;
import io.quartic.weyl.core.compute.BucketComputationImpl;
import io.quartic.weyl.core.compute.BucketSpec;
import io.quartic.weyl.core.compute.BufferComputationImpl;
import io.quartic.weyl.core.compute.BufferSpec;
import io.quartic.weyl.core.compute.ComputationSpec;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerPopulator;
import org.immutables.value.Value;
import rx.Observable;
import rx.subjects.PublishSubject;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@SweetStyle
@Value.Immutable
@Path("/compute")
public abstract class ComputeResource {
    private final PublishSubject<LayerPopulator> populators = PublishSubject.create();
    protected abstract UidGenerator<LayerId> lidGenerator();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public LayerId createComputedLayer(ComputationSpec spec) {
        final LayerId layerId = lidGenerator().get();
        populators.onNext(createPopulator(layerId, spec));
        return layerId;
    }

    private LayerPopulator createPopulator(LayerId layerId, ComputationSpec spec) {
        if (spec instanceof BucketSpec) {
            return BucketComputationImpl.builder()
                    .layerId(layerId)
                    .bucketSpec((BucketSpec) spec)
                    .build();
        } else if (spec instanceof BufferSpec) {
            return BufferComputationImpl.builder()
                    .layerId(layerId)
                    .bufferSpec((BufferSpec) spec)
                    .build();
        } else {
            throw new RuntimeException("Invalid computation spec: " + spec);
        }
    }

    public Observable<LayerPopulator> layerPopulators() {
        return populators;
    }
}
