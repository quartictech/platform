package io.quartic.weyl.resource;

import io.quartic.common.uid.UidGenerator;
import io.quartic.weyl.core.compute.BucketComputation;
import io.quartic.weyl.core.compute.BucketSpec;
import io.quartic.weyl.core.compute.BufferComputation;
import io.quartic.weyl.core.compute.BufferSpec;
import io.quartic.weyl.core.compute.ComputationSpec;
import io.quartic.weyl.core.compute.SpatialPredicateComputation;
import io.quartic.weyl.core.compute.SpatialPredicateSpec;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerPopulator;
import rx.Observable;
import rx.subjects.PublishSubject;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/compute")
public class ComputeResource {
    private final PublishSubject<LayerPopulator> populators = PublishSubject.create();
    private final UidGenerator<LayerId> lidGenerator;

    public ComputeResource(UidGenerator<LayerId> lidGenerator) {
        this.lidGenerator = lidGenerator;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public LayerId createComputedLayer(ComputationSpec spec) {
        final LayerId layerId = lidGenerator.get();
        populators.onNext(createPopulator(layerId, spec));
        return layerId;
    }

    private LayerPopulator createPopulator(LayerId layerId, ComputationSpec spec) {
        if (spec instanceof BucketSpec) {
            return new BucketComputation(layerId, (BucketSpec) spec);
        } else if (spec instanceof BufferSpec) {
            return new BufferComputation(layerId, (BufferSpec) spec);
        } else if (spec instanceof SpatialPredicateSpec) {
            return new SpatialPredicateComputation(layerId, (SpatialPredicateSpec) spec);
        }

        else {
            throw new RuntimeException("Invalid computation spec: " + spec);
        }
    }

    public Observable<LayerPopulator> layerPopulators() {
        return populators;
    }
}
