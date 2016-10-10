package io.quartic.weyl.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as=ImmutablePostgresImportRequest.class)
public interface PostgresImportRequest {
    String name();
    String description();
    String query();
}
