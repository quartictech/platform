package io.quartic.mgmt;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.catalogue.api.DatasetMetadata;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = CreateStaticDatasetRequestImpl.class)
@JsonDeserialize(as = CreateStaticDatasetRequestImpl.class)
public interface CreateStaticDatasetRequest extends CreateDatasetRequest {
    DatasetMetadata metadata();
    String fileName();
    FileType fileType();

    @Override
    default <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
