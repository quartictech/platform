package io.quartic.terminator;

import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.weyl.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractDatasetAction {
    enum ActionType {
        ADDED,
        MODIFIED,
        REMOVED
    }

    ActionType actionType();
    DatasetId id();
    DatasetConfig config();
}
