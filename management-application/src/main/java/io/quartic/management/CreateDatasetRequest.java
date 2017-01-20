package io.quartic.management;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreateStaticDatasetRequestImpl.class, name = "static"),
})
public interface CreateDatasetRequest {
    interface Visitor<T> {
        T visit(CreateStaticDatasetRequest request);
    }

    <T> T accept(Visitor<T> visitor);
}
