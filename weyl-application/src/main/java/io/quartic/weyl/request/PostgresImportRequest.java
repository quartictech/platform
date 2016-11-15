package io.quartic.weyl.request;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quartic.weyl.core.model.LayerMetadata;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as=ImmutablePostgresImportRequest.class)
public interface PostgresImportRequest {
    @JsonUnwrapped
    LayerMetadata metadata();
    String query();
}
