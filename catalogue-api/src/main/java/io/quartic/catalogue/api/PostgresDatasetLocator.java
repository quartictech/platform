package io.quartic.catalogue.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = PostgresDatasetLocatorImpl.class)
@JsonDeserialize(as = PostgresDatasetLocatorImpl.class)
public interface PostgresDatasetLocator extends DatasetLocator {
    String user();
    String password();
    String url();
    String query();
}
