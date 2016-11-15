package io.quartic.management;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreateStaticDatasetRequest.class, name = "static"),
        @JsonSubTypes.Type(value = CreateLiveDatasetRequest.class, name = "live"),
})
public interface CreateDatasetRequest {
    interface Visitor<T> {
        T visit(AbstractCreateStaticDatasetRequest request);
        T visit(AbstractCreateLiveDatasetRequest request);
    }

    <T> T accept(Visitor<T> visitor);
}
