package io.quartic.catalogue.api;

import io.quartic.common.uid.Uid;
import org.jetbrains.annotations.NotNull;

public class DatasetId extends Uid {
    public DatasetId(@NotNull String uid) {
        super(uid);
    }

    public static DatasetId fromString(String uid) {
        return new DatasetId(uid);
    }
}
