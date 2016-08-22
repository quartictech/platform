package io.quartic.weyl.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as=ImmutablePostgisImportRequest.class)
@JsonDeserialize(as=ImmutablePostgisImportRequest.class)
public interface PostgisImportRequest {
    String query();
}
