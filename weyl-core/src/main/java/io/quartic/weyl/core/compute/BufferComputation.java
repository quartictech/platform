package io.quartic.weyl.core.compute;

import com.vividsolutions.jts.operation.buffer.BufferOp;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.AbstractLayer;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.LayerMetadata;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class BufferComputation implements LayerComputation {
    private final AbstractLayer layer;
    private final double bufferDistance;

    public BufferComputation(AbstractLayer layer, BufferSpec bufferSpec) {
        this.layer = layer;
        this.bufferDistance = bufferSpec.bufferDistance();
    }

    public static Optional<ComputationResults> compute(LayerStore store, ComputationSpec computationSpec) {
        return createComputation(store, computationSpec).compute();
    }

    private static LayerComputation createComputation(LayerStore store, ComputationSpec computationSpec) {
        if (computationSpec instanceof BucketSpec) {
            return BucketComputation.create(store, (BucketSpec) computationSpec);
        }
        else if (computationSpec instanceof BufferSpec) {
            return create(store, (BufferSpec) computationSpec);
        }
        else {
            throw new RuntimeException("Invalid computation spec: " + computationSpec);
        }
    }

    @Override
    public Optional<ComputationResults> compute() {
        Collection<AbstractFeature> bufferedFeatures = layer.features().parallelStream()
                .map(feature -> Feature.copyOf(feature)
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
            return new BufferComputation(layer.get(), computationSpec);
        }
        else {
            throw new RuntimeException("Layer not found: " + computationSpec.layerId());
        }
    }
}
