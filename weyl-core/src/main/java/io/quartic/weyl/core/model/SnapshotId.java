package io.quartic.weyl.core.model;

import io.quartic.common.uid.Uid;
import org.jetbrains.annotations.NotNull;

public class SnapshotId extends Uid {
    public SnapshotId(@NotNull String uid) {
        super(uid);
    }

    public static SnapshotId fromString(String uid) {
        return new SnapshotId(uid);
    }
}
