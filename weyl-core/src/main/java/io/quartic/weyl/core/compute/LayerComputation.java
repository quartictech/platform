package io.quartic.weyl.core.compute;

import java.util.Optional;

public interface LayerComputation {
    Optional<ComputationResults> compute();
}
