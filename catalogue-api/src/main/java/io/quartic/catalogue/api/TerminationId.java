package io.quartic.catalogue.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.common.uid.Uid;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = TerminationIdImpl.class)
@JsonDeserialize(as = TerminationIdImpl.class)
public abstract class TerminationId extends Uid {
    public static TerminationId fromString(String uid) {
        return TerminationIdImpl.of(uid);
    }
}
