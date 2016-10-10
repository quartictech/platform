package io.quartic.weyl.session;

import io.quartic.weyl.core.geojson.SweetStyle;
import io.quartic.weyl.core.model.Uid;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractSessionId extends Uid {
}
