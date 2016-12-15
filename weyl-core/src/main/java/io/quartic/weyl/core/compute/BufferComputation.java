package io.quartic.weyl.core.compute;

import com.vividsolutions.jts.operation.buffer.BufferOp;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadataImpl;
import io.quartic.weyl.core.model.LayerPopulator;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerSpecImpl;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.LayerUpdateImpl;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.NakedFeatureImpl;
import rx.Observable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static rx.Observable.just;

public class BufferComputation implements LayerPopulator {
    private final LayerId layerId;
    private final BufferSpec spec;

    public BufferComputation(LayerId layerId, BufferSpec spec) {
        this.layerId = layerId;
        this.spec = spec;
    }

    @Override
    public List<LayerId> dependencies() {
        return singletonList(spec.layerId());
    }

    @Override
    public LayerSpec spec(List<Layer> dependencies) {
        final Layer layer = dependencies.get(0);

        return LayerSpecImpl.of(
                layerId,
                LayerMetadataImpl.builder()
                        .name(layer.spec().metadata().name() + " (buffered)")
                        .description(layer.spec().metadata().description() + " buffered by " + spec.bufferDistance() + "m")
                        .build(),
                IDENTITY_VIEW,
                layer.spec().schema(),
                true
        );
    }

    @Override
    public Observable<LayerUpdate> updates(List<Layer> dependencies) {
        final Layer layer = dependencies.get(0);

        Collection<NakedFeature> bufferedFeatures = layer.features().parallelStream()
                .map(feature -> NakedFeatureImpl.of(
                        Optional.of(feature.entityId().uid()),
                        BufferOp.bufferOp(feature.geometry(), spec.bufferDistance()),
                        feature.attributes())
                )
                .collect(toList());

        return just(LayerUpdateImpl.of(bufferedFeatures));
    }
}
