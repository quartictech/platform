package io.quartic.weyl.core.alert;

import io.quartic.weyl.core.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractAlert {
    String title();
    String body();
}
