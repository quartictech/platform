package io.quartic.weyl.core.filter;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Predicate;
import io.quartic.weyl.core.model.Feature;

@JsonTypeInfo(use= JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BooleanFilter.class, name="boolean"),
        @JsonSubTypes.Type(value = PropertyValueFilter.class, name="propertyValue")
})
public interface Filter extends Predicate<Feature> {
}
