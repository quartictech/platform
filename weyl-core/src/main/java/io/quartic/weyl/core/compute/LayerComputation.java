package io.quartic.weyl.core.compute;

import io.quartic.weyl.core.LayerPopulator;
import io.quartic.weyl.core.model.LayerId;

public interface LayerComputation extends LayerPopulator {
    class Factory {
        public LayerPopulator createPopulator(LayerId layerId, ComputationSpec spec) {
            if (spec instanceof BucketSpec) {
                return BucketComputationImpl.builder()
                        .layerId(layerId)
                        .bucketSpec((BucketSpec) spec)
                        .build();
            } else if (spec instanceof BufferSpec) {
                return new BufferComputation(layerId, (BufferSpec) spec);
            } else {
                throw new RuntimeException("Invalid computation spec: " + spec);
            }
        }
    }
}
