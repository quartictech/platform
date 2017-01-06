package io.quartic.catalogue.api;

import io.quartic.common.uid.Uid;
import org.jetbrains.annotations.NotNull;

public class TerminationId extends Uid {
    public TerminationId(@NotNull String uid) {
        super(uid);
    }

    public static TerminationId fromString(String uid) {
        return new TerminationId(uid);
    }
}
