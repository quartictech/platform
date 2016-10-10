package io.quartic.weyl.session;

import io.quartic.weyl.core.filter.Filter;
import io.quartic.weyl.core.geojson.SweetStyle;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

import java.util.Map;

@SweetStyle
@Value.Immutable
public interface AbstractUserSession {
    Map<LayerId, Filter> filters();
}
