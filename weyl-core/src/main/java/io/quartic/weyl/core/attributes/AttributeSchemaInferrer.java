package io.quartic.weyl.core.attributes;

import io.quartic.weyl.core.model.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AttributeSchemaInferrer {
    public static Map<AttributeName, AbstractAttribute> inferSchema(Collection<Feature> features) {
        Set<AttributeName> attributes = features.parallelStream()
                .flatMap(feature -> feature.metadata().keySet().stream())
                .collect(Collectors.toSet());

        return attributes.parallelStream()
                .collect(Collectors.toConcurrentMap(
                        Function.identity(),
                        attribute -> inferAttribute(attribute, features)));

    }

    private static AbstractAttribute inferAttribute(AttributeName attribute, Collection<Feature> features) {
        Optional<Set<Object>> categories = inferCategories(attribute, features);
        return Attribute.builder()
                .type(inferAttributeType(attribute, features))
                .categories(categories)
                .build();
    }

    private static Optional<Set<Object>> inferCategories(AttributeName attribute, Collection<Feature> features) {
        Set<Object> values = features.stream()
                .map(feature -> feature.metadata().get(attribute))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (values.size() > 0 && values.size() < 20 && values.size() < features.size()) {
            return Optional.of(values);
        }
        else {
            return Optional.empty();
        }
    }

    private static AttributeType inferAttributeType(AttributeName attribute,
                                                    Collection<Feature> features) {
        Set<AttributeType> attributeTypes = features.stream()
                .map(feature -> feature.metadata().get(attribute))
                .filter(Objects::nonNull)
                .map(AttributeSchemaInferrer::inferValueType)
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
        else if (value instanceof AbstractTimeSeriesAttribute) {
            return AttributeType.TIME_SERIES;
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
