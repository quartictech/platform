package io.quartic.catalogue.api;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quartic.weyl.common.SweetStyle;
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
