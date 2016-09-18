package io.quartic.weyl.core.attributes;

import io.quartic.weyl.core.model.Attribute;
import io.quartic.weyl.core.model.AttributeType;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.ImmutableAttribute;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InferAttributeSchema {
    public static Map<String, Attribute> inferSchema(Collection<Feature> features) {
        Set<String> attributes = features.parallelStream()
                .flatMap(feature -> feature.metadata().entrySet().stream())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        return attributes.parallelStream()
                .collect(Collectors.toConcurrentMap(
                        Function.identity(),
                        attribute -> inferAttribute(attribute, features)));

    }

    public static Attribute inferAttribute(String attribute, Collection<Feature> features) {
        Optional<Set<Object>> categories = inferCategories(attribute, features);
        return ImmutableAttribute.builder()
                .type(inferAttributeType(attribute, features))
                .categories(categories)
                .build();
    }

    private static Optional<Set<Object>> inferCategories(String attribute, Collection<Feature> features) {
        Set<Object> values = features.stream()
                .map(feature -> feature.metadata().get(attribute))
                .filter(Optional::isPresent)
                .collect(Collectors.toSet());

        if (values.size() > 0 && values.size() < 10 && values.size() < features.size()) {
            return Optional.of(values);
        }
        else {
            return Optional.empty();
        }
    }


    private static AttributeType inferAttributeType(String attribute,
                                                    Collection<Feature> features) {
        Set<AttributeType> attributeTypes = features.stream()
                .map(feature -> feature.metadata().get(attribute))
                .filter(Optional::isPresent)
                .map(value -> InferAttributeSchema.inferValueType(value.get()))
                .collect(Collectors.toSet());

        if (attributeTypes.size() == 1) {
            return attributeTypes.iterator().next();
        }
        else return AttributeType.UNKNOWN;
    }

     private static AttributeType inferValueType(Object value) {
        if (value instanceof Integer || value instanceof Double || value instanceof Float) {
            return AttributeType.NUMERIC;
        }
        else {
            String stringValue = value.toString();
            try {
                Double.parseDouble(stringValue);
                return AttributeType.NUMERIC;
            }
            catch(NumberFormatException e) {
                return AttributeType.STRING;
            }
        }
    }
}
