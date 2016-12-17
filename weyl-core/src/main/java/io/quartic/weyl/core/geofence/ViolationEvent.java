package io.quartic.weyl.core.geofence;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

interface ViolationEvent {

    @SweetStyle
    @Value.Immutable
    interface ViolationBeginEvent extends ViolationEvent {
        Geofence geofence();
        Feature feature();

        @Override
        default <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    @SweetStyle
    @Value.Immutable
    interface ViolationEndEvent extends ViolationEvent {
        Geofence geofence();
        Feature feature();

        @Override
        default <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    @SweetStyle
    @Value.Immutable
    interface ViolationClearEvent extends ViolationEvent {
        @Override
        default <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    <T> T accept(Visitor<T> visitor);

    interface Visitor<T> {
        T visit(ViolationBeginEvent event);
        T visit(ViolationEndEvent event);
        T visit(ViolationClearEvent event);
    }
}
