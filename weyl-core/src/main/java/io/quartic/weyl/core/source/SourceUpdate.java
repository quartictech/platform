package io.quartic.weyl.core.source;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.NakedFeature;
import org.immutables.value.Value;

import java.util.Collection;

@SweetStyle
@Value.Immutable
public interface SourceUpdate {
    // TODO: not Collections
    Collection<NakedFeature> features();
}
