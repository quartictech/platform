package io.quartic.jester.api;

import io.quartic.weyl.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractWebsocketDatasetLocator extends DatasetLocator {
    String url();
}
