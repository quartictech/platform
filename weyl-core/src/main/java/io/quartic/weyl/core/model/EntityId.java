package io.quartic.weyl.core.model;

import io.quartic.common.uid.Uid;
import org.jetbrains.annotations.NotNull;

public class EntityId extends Uid {
    public EntityId(@NotNull String uid) {
        super(uid);
    }

    public static EntityId fromString(String uid) {
        return new EntityId(uid);
    }
}
