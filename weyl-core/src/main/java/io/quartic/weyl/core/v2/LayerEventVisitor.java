package io.quartic.weyl.core.v2;

public interface LayerEventVisitor<T> {
    T visit(AbstractLayerAppendEvent event);
    T visit(AbstractLayerClearEvent abstractLayerClearEvent);
}
