package io.quartic.catalogue.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.common.uid.Uid;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = DatasetIdImpl.class)
@JsonDeserialize(as = DatasetIdImpl.class)
public abstract class DatasetId extends Uid {
    public static DatasetId fromString(String uid) {
        return DatasetIdImpl.of(uid);
    }
}
