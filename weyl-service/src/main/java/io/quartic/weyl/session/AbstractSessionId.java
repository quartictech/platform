package io.quartic.weyl.session;

import io.quartic.weyl.core.geojson.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractSessionId {
    String id();
}
