package io.quartic.weyl.resource;

import io.quartic.common.uid.UidGenerator;
import io.quartic.weyl.core.LayerPopulator;
import io.quartic.weyl.core.LayerSpecImpl;
import io.quartic.weyl.core.compute.ComputationResults;
import io.quartic.weyl.core.compute.ComputationResultsImpl;
import io.quartic.weyl.core.compute.ComputationSpec;
import io.quartic.weyl.core.compute.LayerComputation;
import io.quartic.weyl.core.model.AttributeSchema;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.source.LayerUpdateImpl;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.ArrayList;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.test.rx.RxUtils.all;
import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.empty;

public class ComputeResourceShould {

    private final LayerComputation.Factory computationFactory = mock(LayerComputation.Factory.class);
    @SuppressWarnings("unchecked")
    private final UidGenerator<LayerId> lidGenerator = mock(UidGenerator.class);
    private final ComputeResource resource = ComputeResourceImpl.of(lidGenerator, computationFactory);
    private final TestSubscriber<LayerPopulator> sub = TestSubscriber.create();

    @Before
    public void before() throws Exception {
        resource.layerPopulators().subscribe(sub);
    }

    @Test
    public void create_descriptor_for_computed_layer() throws Exception {
        LayerId layerId = mock(LayerId.class);
        final LayerMetadata metadata = mock(LayerMetadata.class);
        final AttributeSchema schema = mock(AttributeSchema.class);
        final ArrayList<NakedFeature> features = newArrayList(mock(NakedFeature.class), mock(NakedFeature.class));
        final ComputationResults results = ComputationResultsImpl.of(
                metadata,
                schema,
                features
        );
        final ComputationSpec spec = mock(ComputationSpec.class);

        when(lidGenerator.get()).thenReturn(layerId);
        when(computationFactory.createPopulator(any())).thenReturn(Optional.of(results));

        final LayerId computedLayerId = resource.createComputedLayer(spec);
        final LayerPopulator populator = sub.getOnNextEvents().get(0);

        verify(computationFactory).createPopulator(spec);
        assertThat(computedLayerId, equalTo(layerId));
        assertThat(populator.spec(emptyList()), equalTo(LayerSpecImpl.of(
                layerId,
                metadata,
                IDENTITY_VIEW,
                schema,
                true,
                empty() // Don't care
        )));
        assertThat(all(populator.spec(emptyList()).updates()), contains(LayerUpdateImpl.of(features)));
    }
}
