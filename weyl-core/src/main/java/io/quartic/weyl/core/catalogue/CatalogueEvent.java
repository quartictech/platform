package io.quartic.weyl.core.catalogue;

import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface CatalogueEvent {
    enum Type {
        CREATE,
        DELETE
    }

    Type type();
    DatasetId id();
    DatasetConfig config();
}
