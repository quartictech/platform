package io.quartic.weyl.core.export;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.catalogue.api.model.DatasetLocator;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as=LayerExportResultImpl.class)
public interface LayerExportResult {
    DatasetLocator locator();
    String message();
}
