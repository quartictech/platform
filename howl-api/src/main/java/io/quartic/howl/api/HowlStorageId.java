package io.quartic.howl.api;

import io.quartic.common.uid.Uid;
import org.jetbrains.annotations.NotNull;

public class HowlStorageId extends Uid {
    public HowlStorageId(@NotNull String uid) {
        super(uid);
    }

    public static HowlStorageId fromString(String uid) {
        return new HowlStorageId(uid);
    }
}
