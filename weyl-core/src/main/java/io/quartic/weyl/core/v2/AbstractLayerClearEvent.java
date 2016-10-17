package io.quartic.weyl.core.v2;

public class AbstractLayerClearEvent implements LayerEvent {
    @Override
    public <T> T accept(LayerEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
