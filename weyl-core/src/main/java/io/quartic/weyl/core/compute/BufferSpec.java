package io.quartic.weyl.core.compute;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = BufferSpecImpl.class)
@JsonDeserialize(as = BufferSpecImpl.class)
public interface BufferSpec extends ComputationSpec {
    LayerId layerId();
    double bufferDistance();
}
