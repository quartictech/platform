package io.quartic.weyl.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.common.uid.Uid;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = LayerIdImpl.class)
@JsonDeserialize(as = LayerIdImpl.class)
public abstract class LayerId extends Uid {
    public static LayerId fromString(String uid) {
        return LayerIdImpl.of(uid);
    }
}
