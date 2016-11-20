package io.quartic.weyl.core.attributes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.List;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = TimeSeriesAttributeImpl.class)
@JsonDeserialize(as = TimeSeriesAttributeImpl.class)
public interface TimeSeriesAttribute extends ComplexAttribute {
    @SweetStyle
    @Value.Immutable
    @JsonSerialize(as = TimeSeriesEntryImpl.class)
    @JsonDeserialize(as = TimeSeriesEntryImpl.class)
    interface TimeSeriesEntry {
        Long timestamp();
        Double value();
    }
    List<TimeSeriesEntry> series();
}
