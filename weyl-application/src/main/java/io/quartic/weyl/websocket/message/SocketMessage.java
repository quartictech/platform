package io.quartic.weyl.websocket.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LayerUpdateMessageImpl.class, name = "LayerUpdate"),
        @JsonSubTypes.Type(value = GeofenceGeometryUpdateMessageImpl.class, name = "GeofenceGeometryUpdate"),
        @JsonSubTypes.Type(value = GeofenceViolationsUpdateMessageImpl.class, name = "GeofenceViolationsUpdate"),
        @JsonSubTypes.Type(value = SelectionDrivenUpdateMessageImpl.class, name = "SelectionDrivenUpdate"),
        @JsonSubTypes.Type(value = AlertMessageImpl.class, name = "Alert"),
        @JsonSubTypes.Type(value = ClientStatusMessageImpl.class, name = "ClientStatus"),
        @JsonSubTypes.Type(value = PingMessageImpl.class, name = "Ping"),
})
public interface SocketMessage {
}
