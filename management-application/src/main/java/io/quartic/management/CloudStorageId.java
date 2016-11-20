package io.quartic.management;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.common.uid.Uid;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = CloudStorageIdImpl.class)
@JsonDeserialize(as = CloudStorageIdImpl.class)
public abstract class CloudStorageId extends Uid {
    public static CloudStorageId fromString(String uid) {
        return CloudStorageIdImpl.of(uid);
    }
}
