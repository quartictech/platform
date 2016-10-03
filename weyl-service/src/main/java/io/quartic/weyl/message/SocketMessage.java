package io.quartic.weyl.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LayerUpdateMessage.class, name = "LayerUpdate"),
        @JsonSubTypes.Type(value = AlertMessage.class, name = "Alert"),
        @JsonSubTypes.Type(value = ClientStatusMessage.class, name = "ClientStatus"),
        @JsonSubTypes.Type(value = PingMessage.class, name = "Ping"),
})
public interface SocketMessage {
}
