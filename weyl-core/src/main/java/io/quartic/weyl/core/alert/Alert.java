package io.quartic.weyl.core.alert;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = AlertImpl.class)
@JsonDeserialize(as = AlertImpl.class)
public interface Alert {
    String title();
    String body();
}
