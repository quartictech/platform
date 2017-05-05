package io.quartic.weyl.websocket.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LayerListUpdateMessage.class, name = "LayerListUpdate"),
        @JsonSubTypes.Type(value = LayerUpdateMessage.class, name = "LayerUpdate"),
        @JsonSubTypes.Type(value = GeofenceGeometryUpdateMessage.class, name = "GeofenceGeometryUpdate"),
        @JsonSubTypes.Type(value = GeofenceViolationsUpdateMessage.class, name = "GeofenceViolationsUpdate"),
        @JsonSubTypes.Type(value = SelectionDrivenUpdateMessage.class, name = "SelectionDrivenUpdate"),
        @JsonSubTypes.Type(value = AlertMessage.class, name = "Alert"),
        @JsonSubTypes.Type(value = ClientStatusMessage.class, name = "ClientStatus"),
        @JsonSubTypes.Type(value = PingMessage.class, name = "Ping"),
})
public interface SocketMessage {
}
