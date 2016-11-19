package io.quartic.catalogue.api;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface WebsocketDatasetLocator extends DatasetLocator {
    String url();
}
