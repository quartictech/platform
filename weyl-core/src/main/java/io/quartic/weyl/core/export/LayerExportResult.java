package io.quartic.weyl.core.export;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.catalogue.api.DatasetLocator;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.Optional;

@SweetStyle
@Value.Immutable
@JsonSerialize(as=LayerExportResultImpl.class)
public interface LayerExportResult {
    DatasetLocator locator();
    String message();
}
