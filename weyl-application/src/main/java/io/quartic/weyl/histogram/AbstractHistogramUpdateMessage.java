package io.quartic.weyl.histogram;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.compute.AbstractHistogram;
import io.quartic.weyl.message.SocketMessage;
import org.immutables.value.Value;

import java.util.List;

@SweetStyle
@Value.Immutable
public interface AbstractHistogramUpdateMessage extends SocketMessage {
    List<AbstractHistogram> histograms();
}
