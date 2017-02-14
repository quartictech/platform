package io.quartic.weyl.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.api.LayerUpdateType;
import org.immutables.value.Value;

import java.util.Collection;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = LayerUpdateImpl.class)
@JsonDeserialize(as = LayerUpdateImpl.class)
public interface LayerUpdate {

    LayerUpdateType type();

    // TODO: not Collections
    Collection<NakedFeature> features();
}
