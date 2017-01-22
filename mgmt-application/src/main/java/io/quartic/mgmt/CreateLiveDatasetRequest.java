package io.quartic.mgmt;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.catalogue.api.DatasetMetadata;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = CreateLiveDatasetRequestImpl.class)
@JsonDeserialize(as = CreateLiveDatasetRequestImpl.class)
public interface CreateLiveDatasetRequest extends CreateDatasetRequest {
    DatasetMetadata metadata();

    @Override
    default <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
