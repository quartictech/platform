package io.quartic.management;

import io.quartic.catalogue.api.DatasetMetadata;
import io.quartic.weyl.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractCreateStaticDatasetRequest extends CreateDatasetRequest {
    DatasetMetadata metadata();
    String fileName();

    @Override
    default <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
