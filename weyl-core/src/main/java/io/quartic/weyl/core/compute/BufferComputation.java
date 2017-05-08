package io.quartic.weyl.core.compute;

import com.vividsolutions.jts.operation.buffer.BufferOp;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.model.LayerPopulator;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.NakedFeature;
import org.immutables.value.Value;
import rx.Observable;

import java.time.Clock;
import java.util.Collection;
import java.util.List;

import static io.quartic.weyl.api.LayerUpdateType.REPLACE;
import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@SweetStyle
@Value.Immutable
public abstract class BufferComputation implements LayerPopulator {
    protected abstract LayerId layerId();
    protected abstract BufferSpec bufferSpec();

    @Value.Default
    protected Clock clock() {
        return Clock.systemUTC();
    }

    @Override
    public List<LayerId> dependencies() {
        return singletonList(bufferSpec().getLayerId());
    }

    @Override
    public LayerSpec spec(List<Layer> dependencies) {
        final Layer layer = dependencies.get(0);

        return new LayerSpec(
                layerId(),
                new LayerMetadata(
                        layer.getSpec().getMetadata().getName() + " (buffered)",
                        layer.getSpec().getMetadata().getDescription() + " (buffered by " + bufferSpec().getBufferDistance() + "m)",
                        layer.getSpec().getMetadata().getAttribution(),
                        clock().instant()
                ),
                IDENTITY_VIEW,
                layer.getSpec().getStaticSchema(),
                true
        );
    }

    @Override
    public Observable<LayerUpdate> updates(List<Layer> dependencies) {
        final Layer layer = dependencies.get(0);

        Collection<NakedFeature> bufferedFeatures = layer.getFeatures().parallelStream()
                .map(feature -> new NakedFeature(
                        feature.getEntityId().getUid(),
                        BufferOp.bufferOp(feature.getGeometry(), bufferSpec().getBufferDistance()),
                        feature.getAttributes())
                )
                .collect(toList());

        return Observable.<LayerUpdate>never().startWith(new LayerUpdate(REPLACE, bufferedFeatures));
    }
}
