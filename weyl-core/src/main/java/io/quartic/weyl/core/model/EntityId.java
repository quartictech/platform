package io.quartic.weyl.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.common.uid.Uid;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = EntityIdImpl.class)
@JsonDeserialize(as = EntityIdImpl.class)
public abstract class EntityId extends Uid {
    public static EntityId fromString(String uid) {
        return EntityIdImpl.of(uid);
    }
}
