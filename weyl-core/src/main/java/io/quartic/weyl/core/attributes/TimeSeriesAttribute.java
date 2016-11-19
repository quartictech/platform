package io.quartic.weyl.core.attributes;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.List;

@SweetStyle
@Value.Immutable
public interface TimeSeriesAttribute extends ComplexAttribute {
    @SweetStyle
    @Value.Immutable
    interface TimeSeriesEntry {
        Long timestamp();
        Double value();
    }
    List<TimeSeriesEntry> series();
}
