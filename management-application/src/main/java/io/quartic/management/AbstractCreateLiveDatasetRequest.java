package io.quartic.management;

import io.quartic.catalogue.api.DatasetMetadata;
import io.quartic.weyl.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractCreateLiveDatasetRequest extends CreateDatasetRequest {
    DatasetMetadata metadata();

    @Override
    default <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
