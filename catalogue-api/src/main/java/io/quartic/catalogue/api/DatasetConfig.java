package io.quartic.catalogue.api;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.Map;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = DatasetConfigImpl.class)
@JsonDeserialize(as = DatasetConfigImpl.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface DatasetConfig {
    DatasetMetadata metadata();
    DatasetLocator locator();

    @JsonAnyGetter
    Map<String, Object> extensions();
}
