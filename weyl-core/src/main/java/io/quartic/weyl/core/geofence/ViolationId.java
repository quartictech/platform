package io.quartic.weyl.core.geofence;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.common.uid.Uid;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = ViolationIdImpl.class)
@JsonDeserialize(as = ViolationIdImpl.class)
public abstract class ViolationId extends Uid {
    public static ViolationId fromString(String uid) {
        return ViolationIdImpl.of(uid);
    }
}
