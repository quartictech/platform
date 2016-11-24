package io.quartic.weyl.core.attributes;

import io.quartic.weyl.core.model.*;

import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

public class AttributeSchemaInferrer {

    public static final int MAX_CATEGORIES = 19;

    public static Map<AttributeName, Attribute> inferSchema(Collection<Feature> features) {
        final List<Attributes> attributes = features.stream().map(Feature::attributes).collect(toList());
        if (attributes.isEmpty()) {
            return emptyMap();
        }

        final Collection<AttributeName> names = attributes.iterator().next().attributes().keySet();   // They should all be the same
        return names.parallelStream()
                .collect(toConcurrentMap(identity(), attribute -> inferAttribute(attribute, attributes)));
    }

    private static Attribute inferAttribute(AttributeName name, Collection<Attributes> attributes) {
        return AttributeImpl.builder()
                .type(inferAttributeType(name, attributes))
                .categories(inferCategories(name, attributes))
                .build();
    }

    private static Optional<Set<Object>> inferCategories(AttributeName name, Collection<Attributes> attributes) {
        Set<Object> values = attributes.stream()
                .map(a -> a.attributes().get(name))
                .filter(Objects::nonNull)
                .collect(toSet());

        if (values.size() <= MAX_CATEGORIES && values.size() < attributes.size()) {
            return Optional.of(values);
        } else {
            return Optional.empty();
        }
    }

    private static AttributeType inferAttributeType(AttributeName attribute, Collection<Attributes> attributes) {
        Set<AttributeType> attributeTypes = attributes.stream()
                .map(a -> a.attributes().get(attribute))
                .filter(Objects::nonNull)
                .map(AttributeSchemaInferrer::inferValueType)
                .collect(toSet());

        if (attributeTypes.size() == 1) {
            return attributeTypes.iterator().next();
        }
        else return AttributeType.UNKNOWN;
    }

     private static AttributeType inferValueType(Object value) {
        if (value instanceof Integer || value instanceof Double || value instanceof Float) {
            return AttributeType.NUMERIC;
        }
        else if (value instanceof TimeSeriesAttribute) {
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
