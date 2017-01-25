package io.quartic.weyl.core.compute;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = SpatialPredicateSpecImpl.class)
@JsonDeserialize(as = SpatialPredicateSpecImpl.class)
public interface SpatialPredicateSpec extends ComputationSpec {
    LayerId layerA();
    LayerId layerB();
    SpatialPredicate predicate();
}
