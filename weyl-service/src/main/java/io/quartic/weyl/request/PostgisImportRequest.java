package io.quartic.weyl.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as=ImmutablePostgisImportRequest.class)
@JsonDeserialize(as=ImmutablePostgisImportRequest.class)
public interface PostgisImportRequest {
    String name();
    String description();
    Optional<String> attribution();
    String query();
}
