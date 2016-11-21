package io.quartic.weyl.core.source;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.NakedFeature;
import org.immutables.value.Value;

import java.util.Collection;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = SourceUpdateImpl.class)
@JsonDeserialize(as = SourceUpdateImpl.class)
public interface SourceUpdate {
    // TODO: not Collections
    Collection<NakedFeature> features();
}
