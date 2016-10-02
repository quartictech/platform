package io.quartic.weyl.message;

import io.quartic.weyl.core.geojson.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractNotification extends SocketMessage {
    String title();
    String body();
}
