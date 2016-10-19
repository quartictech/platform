package io.quartic.weyl.core.compute;

import com.vividsolutions.jts.operation.buffer.BufferOp;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.model.AbstractLayer;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.ImmutableFeature;
import io.quartic.weyl.core.model.LayerMetadata;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class BufferComputation implements LayerComputation {
    private final double bufferDistance;
    private final AbstractLayer layer;
    private final FeatureStore featureStore;

    public BufferComputation(FeatureStore featureStore, AbstractLayer layer, BufferSpec bufferSpec) {
        this.featureStore = featureStore;
        this.layer = layer;
        this.bufferDistance = bufferSpec.bufferDistance();
    }

    @Override
    public Optional<ComputationResults> compute() {
        Collection<Feature> bufferedFeatures = layer.features().parallelStream()
                .map(feature -> ImmutableFeature.copyOf(feature)
                        .withUid(featureStore.getFeatureIdGenerator().get())
                        .withGeometry(BufferOp.bufferOp(feature.geometry(), bufferDistance)))
                .collect(Collectors.toList());

        return Optional.of(ComputationResults.of(
                    LayerMetadata.builder()
                            .name(layer.metadata().name() + " (buffered)")
                            .description(layer.metadata().description() + " buffered by " + bufferDistance + "m")
                            .build(),
                    bufferedFeatures,
                    layer.schema()
            ));
    }

    public static LayerComputation create(LayerStore store, BufferSpec computationSpec) {
        Optional<AbstractLayer> layer = store.getLayer(computationSpec.layerId());

        if (layer.isPresent()) {
            return new BufferComputation(store.getFeatureStore(), layer.get(), computationSpec);
        }
        else {
            throw new RuntimeException("layer not found: " + computationSpec.layerId());
        }
    }
}
