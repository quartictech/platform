package io.quartic.weyl.core.model;

import io.quartic.common.uid.Uid;
import org.jetbrains.annotations.NotNull;

public class LayerId extends Uid {
    public LayerId(@NotNull String uid) {
        super(uid);
    }

    public static LayerId fromString(String uid) {
        return new LayerId(uid);
    }
}
