package io.quartic.weyl.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.quartic.common.uid.Uid;
import org.jetbrains.annotations.NotNull;

public class EntityId extends Uid {
    public EntityId(@NotNull String uid) {
        super(uid);
    }

    @JsonCreator
    public static EntityId fromString(String uid) {
        return new EntityId(uid);
    }
}
