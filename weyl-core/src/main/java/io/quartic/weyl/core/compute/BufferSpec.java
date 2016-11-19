package io.quartic.weyl.core.compute;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface BufferSpec extends ComputationSpec {
    LayerId layerId();
    double bufferDistance();
}
