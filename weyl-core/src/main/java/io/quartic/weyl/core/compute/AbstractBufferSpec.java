package io.quartic.weyl.core.compute;

import io.quartic.weyl.core.SweetStyle;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractBufferSpec extends ComputationSpec {
    LayerId layerId();
    double bufferDistance();
}
