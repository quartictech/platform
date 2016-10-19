package io.quartic.weyl.core.compute;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.quartic.weyl.core.model.AbstractLayer;

import java.util.Optional;

public interface LayerComputation {
    Optional<ComputationResults> compute();
}
