package io.quartic.weyl.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.AttributeSchema;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.model.LayerStats;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as=LayerResponseImpl.class)
@JsonDeserialize(as=LayerResponseImpl.class)
public interface LayerResponse {
   LayerId id();
   LayerMetadata metadata();
   LayerStats stats();
   AttributeSchema attributeSchema();
   boolean live();
}
