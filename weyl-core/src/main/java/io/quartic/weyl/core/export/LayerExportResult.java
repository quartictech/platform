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
    Optional<DatasetLocator> locator();
    String message();

    static LayerExportResult success(DatasetLocator locator, String message) {
        return LayerExportResultImpl.of(Optional.of(locator), message);
    }

    static LayerExportResult failure(String error) {
        return LayerExportResultImpl.of(Optional.empty(), error);
    }

    default boolean isSuccess() {
        return locator().isPresent();
    }
}
