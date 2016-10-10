package io.quartic.weyl.core.attributes;

import io.quartic.weyl.core.geojson.SweetStyle;
import org.immutables.value.Value;

import java.util.List;

@SweetStyle
@Value.Immutable
public interface AbstractTimeSeriesAttribute extends ComplexAttribute {
    @SweetStyle
    @Value.Immutable
    interface AbstractTimeSeriesEntry {
        Long timestamp();
        Double value();
    }
    List<TimeSeriesEntry> series();
}
