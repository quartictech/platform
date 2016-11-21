package io.quartic.weyl.core.compute;

import io.quartic.weyl.core.LayerStore;

import java.util.Optional;

public interface LayerComputation {
    static Optional<ComputationResults> compute(LayerStore store, ComputationSpec computationSpec) {
        LayerComputation result;
        if (computationSpec instanceof BucketSpec) {
            result = BucketComputation.create(store, (BucketSpec) computationSpec);
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
