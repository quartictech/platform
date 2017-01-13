package io.quartic.weyl.core.render;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.howl.api.HowlStorageId;
import org.immutables.value.Value;

import java.util.Optional;

@SweetStyle
@Value.Immutable
@JsonSerialize(as=LayerExportResultImpl.class)
public interface LayerExportResult {
    Optional<HowlStorageId> howlStorageId();
    Optional<String> error();
    Optional<Integer> featuresExported();

    static LayerExportResult success(HowlStorageId howlStorageId, Integer featuresExported) {
        return LayerExportResultImpl.of(Optional.of(howlStorageId), Optional.empty(), Optional.of(featuresExported));
    }

    static LayerExportResult failure(String error) {
        return LayerExportResultImpl.of(Optional.empty(), Optional.of(error), Optional.empty());
    }
}
