package io.quartic.catalogue.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = GooglePubSubDatasetLocatorImpl.class)
@JsonDeserialize(as = GooglePubSubDatasetLocatorImpl.class)
public interface GooglePubSubDatasetLocator extends DatasetLocator {
    String topic();
}