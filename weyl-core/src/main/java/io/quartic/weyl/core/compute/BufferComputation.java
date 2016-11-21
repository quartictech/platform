package io.quartic.weyl.core.compute;

import com.vividsolutions.jts.operation.buffer.BufferOp;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerMetadataImpl;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class BufferComputation implements LayerComputation {
    private final Layer layer;
    private final double bufferDistance;

    public BufferComputation(Layer layer, BufferSpec bufferSpec) {
        this.layer = layer;
        this.bufferDistance = bufferSpec.bufferDistance();
    }

    @Override
    public Optional<ComputationResults> compute() {
        Collection<Feature> bufferedFeatures = layer.features().parallelStream()
                .map(feature -> FeatureImpl.copyOf(feature)
                        .withGeometry(BufferOp.bufferOp(feature.geometry(), bufferDistance)))
                .collect(Collectors.toList());

        return Optional.of(ComputationResultsImpl.of(
                    LayerMetadataImpl.builder()
                            .name(layer.metadata().name() + " (buffered)")
                            .description(layer.metadata().description() + " buffered by " + bufferDistance + "m")
                            .build(),
                    bufferedFeatures,
                    layer.schema()
            ));
    }

    public static LayerComputation create(LayerStore store, BufferSpec computationSpec) {
        Optional<Layer> layer = store.getLayer(computationSpec.layerId());

        if (layer.isPresent()) {
            return new BufferComputation(layer.get(), computationSpec);
        }
        else {
            throw new RuntimeException("Layer not found: " + computationSpec.layerId());
        }
    }
}
