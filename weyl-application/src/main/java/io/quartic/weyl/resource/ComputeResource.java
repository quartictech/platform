package io.quartic.weyl.resource;

import io.quartic.common.SweetStyle;
import io.quartic.common.uid.UidGenerator;
import io.quartic.weyl.core.LayerPopulator;
import io.quartic.weyl.core.compute.ComputationSpec;
import io.quartic.weyl.core.compute.LayerComputation;
import io.quartic.weyl.core.model.LayerId;
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
    protected abstract LayerComputation.Factory computationFactory();
    protected abstract UidGenerator<LayerId> lidGenerator();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public LayerId createComputedLayer(ComputationSpec spec) {
        final LayerId layerId = lidGenerator().get();
        populators.onNext(computationFactory().createPopulator(layerId, spec));
        return layerId;
        // TODO: error handling
    }

    public Observable<LayerPopulator> layerPopulators() {
        return populators;
    }
}
