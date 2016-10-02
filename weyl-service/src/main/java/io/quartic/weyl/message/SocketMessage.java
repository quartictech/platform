package io.quartic.weyl.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LayerUpdate.class, name = "LayerUpdate"),
        @JsonSubTypes.Type(value = Notification.class, name = "Notification")
})
public interface SocketMessage {
}
