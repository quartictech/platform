package io.quartic.howl.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.common.uid.Uid;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = CloudStorageIdImpl.class)
@JsonDeserialize(as = CloudStorageIdImpl.class)
public abstract class HowlStorageId extends Uid {
    public static HowlStorageId fromString(String uid) {
        return HowlStorageIdImpl.of(uid);
    }
}
