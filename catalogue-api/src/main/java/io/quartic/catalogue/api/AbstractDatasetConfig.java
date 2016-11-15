package io.quartic.catalogue.api;

import io.quartic.common.SweetStyle;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.immutables.value.Value;

import java.util.Map;

@SweetStyle
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractDatasetConfig {
    DatasetMetadata metadata();
    DatasetLocator locator();

    @JsonAnyGetter
    Map<String, Object> extensions();
}
