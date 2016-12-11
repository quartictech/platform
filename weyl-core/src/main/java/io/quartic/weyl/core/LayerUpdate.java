package io.quartic.weyl.core;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.NakedFeature;
import org.immutables.value.Value;

import java.util.Collection;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = LayerUpdateImpl.class)
@JsonDeserialize(as = LayerUpdateImpl.class)
public interface LayerUpdate {
    // TODO: not Collections
    Collection<NakedFeature> features();
}
