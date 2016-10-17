package io.quartic.weyl.core.v2;

public interface LayerEvent {
    <T> T accept(LayerEventVisitor<T> visitor);
}
