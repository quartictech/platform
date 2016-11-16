package io.quartic.weyl.core.compute;

import io.quartic.weyl.core.LayerStore;

import java.util.Optional;

public final class BufferComputationUtils {
    private BufferComputationUtils() {}

    public static Optional<ComputationResults> compute(LayerStore store, ComputationSpec computationSpec) {
        return createComputation(store, computationSpec).compute();
    }

    private static LayerComputation createComputation(LayerStore store, ComputationSpec computationSpec) {
        if (computationSpec instanceof BucketSpec) {
            return BucketComputation.create(store, (BucketSpec) computationSpec);
        }
        else if (computationSpec instanceof BufferSpec) {
            return BufferComputation.create(store, (BufferSpec) computationSpec);
        }
        else {
            throw new RuntimeException("Invalid computation spec: " + computationSpec);
        }
    }
}
