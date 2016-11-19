package io.quartic.weyl.attributes;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.message.SocketMessage;
import org.immutables.value.Value;

import java.util.Map;

@SweetStyle
@Value.Immutable
public interface AbstractAttributesUpdateMessage extends SocketMessage {
    Map<EntityId, Attributes> attributes();
}
