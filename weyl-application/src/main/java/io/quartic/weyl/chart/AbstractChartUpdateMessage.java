package io.quartic.weyl.chart;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.attributes.TimeSeriesAttribute;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.message.SocketMessage;
import org.immutables.value.Value;

import java.util.Map;

@SweetStyle
@Value.Style.Depluralize(dictionary = {"timeseries:timeseries"})
@Value.Immutable
public interface AbstractChartUpdateMessage extends SocketMessage {
    Map<AttributeName, Map<String, TimeSeriesAttribute>> timeseries();
}
