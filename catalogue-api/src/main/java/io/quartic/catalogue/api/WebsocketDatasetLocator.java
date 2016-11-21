package io.quartic.catalogue.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = WebsocketDatasetLocatorImpl.class)
@JsonDeserialize(as = WebsocketDatasetLocatorImpl.class)
public interface WebsocketDatasetLocator extends DatasetLocator {
    String url();
}
