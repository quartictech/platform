package io.quartic.weyl.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.quartic.weyl.chart.ChartUpdateMessage;
import io.quartic.weyl.histogram.HistogramUpdateMessage;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LayerUpdateMessage.class, name = "LayerUpdate"),
        @JsonSubTypes.Type(value = GeofenceGeometryUpdateMessage.class, name = "GeofenceGeometryUpdate"),
        @JsonSubTypes.Type(value = GeofenceViolationsUpdateMessage.class, name = "GeofenceViolationsUpdate"),
        @JsonSubTypes.Type(value = ChartUpdateMessage.class, name = "ChartUpdate"),
        @JsonSubTypes.Type(value = HistogramUpdateMessage.class, name = "HistogramUpdate"),
        @JsonSubTypes.Type(value = AlertMessage.class, name = "Alert"),
        @JsonSubTypes.Type(value = ClientStatusMessage.class, name = "ClientStatus"),
        @JsonSubTypes.Type(value = PingMessage.class, name = "Ping"),
})
public interface SocketMessage {
}
