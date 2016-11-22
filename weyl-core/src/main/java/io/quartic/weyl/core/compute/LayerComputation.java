package io.quartic.weyl.core.compute;

import io.quartic.weyl.core.LayerStore;

import java.util.Optional;

public interface LayerComputation {
    static Optional<ComputationResults> compute(LayerStore store, ComputationSpec computationSpec) {
        LayerComputation result;
        if (computationSpec instanceof BucketSpec) {
            result = BucketComputationImpl.builder()
                    .store(store)
                    .bucketSpec((BucketSpec) computationSpec)
                    .build();
        }
        else if (computationSpec instanceof BufferSpec) {
            result = BufferComputation.create(store, (BufferSpec) computationSpec);
        }
        else {
            throw new RuntimeException("Invalid computation spec: " + computationSpec);
        }
        return result.compute();
    }

    Optional<ComputationResults> compute();
}
