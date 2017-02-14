package io.quartic.weyl.core.attributes;

import io.quartic.weyl.core.model.Attribute;
import io.quartic.weyl.core.model.AttributeImpl;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeType;
import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.core.model.DynamicSchema;
import io.quartic.weyl.core.model.DynamicSchemaImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.StaticSchema;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static io.quartic.weyl.core.model.AttributeType.NUMERIC;
import static io.quartic.weyl.core.model.AttributeType.STRING;
import static io.quartic.weyl.core.model.AttributeType.TIME_SERIES;
import static io.quartic.weyl.core.model.AttributeType.UNKNOWN;
import static java.util.Collections.emptyMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toConcurrentMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class AttributeSchemaInferrer {

    public static final int MAX_CATEGORIES = 19;

    public static DynamicSchema inferSchema(Collection<Feature> newFeatures, DynamicSchema previousInference, StaticSchema staticSchema) {
        final List<Attributes> attributes = newFeatures.stream().map(Feature::attributes).collect(toList());
        if (attributes.isEmpty()) {
            return DynamicSchemaImpl.of(emptyMap());
        }

        final Collection<AttributeName> names = attributes.iterator().next().attributes().keySet();   // They should all be the same
        return DynamicSchemaImpl.of(names.parallelStream()
                .collect(toConcurrentMap(identity(),
                        attribute -> inferAttribute(
                                attribute,
                                attributes,
                                previousInference.attributes(),
                                staticSchema
                        ))
        ));
    }

    private static Attribute inferAttribute(
            AttributeName name,
            Collection<Attributes> attributes,
            Map<AttributeName, Attribute> previousInference,
            StaticSchema staticSchema
    ) {
        final Attribute previous = previousInference.get(name);
        return AttributeImpl.builder()
                .type(staticSchema.attributeTypes().getOrDefault(name, inferAttributeType(name, attributes, previous)))
                .categories(inferCategories(name, attributes, previous, staticSchema.categoricalAttributes().contains(name)))
                .build();
    }

    private static Optional<Set<Object>> inferCategories(
            AttributeName name,
            Collection<Attributes> attributes,
            Attribute previous,
            boolean forceToBeCategorical
    ) {
        if (previous != null && !previous.categories().isPresent()) {
            return Optional.empty();    // Assume this means there were already too many
        }

        Set<Object> union = (previous != null) ? newHashSet(previous.categories().get()) : newHashSet();

        Set<Object> values = attributes.stream()
                .map(a -> a.attributes().get(name))
                .filter(v -> (v != null) && isPrimitive(v))
                .collect(toCollection(() -> union));

        return (forceToBeCategorical || (values.size() > 0 && values.size() <= MAX_CATEGORIES))
                ? Optional.of(values)
                : Optional.empty();
    }

    private static AttributeType inferAttributeType(AttributeName name, Collection<Attributes> attributes, Attribute previous) {
        Set<AttributeType> types = attributes.stream()
                .map(a -> a.attributes().get(name))
                .filter(Objects::nonNull)
                .map(AttributeSchemaInferrer::inferValueType)
                .collect(toSet());

        if (types.size() != 1) {
            return UNKNOWN;
        }
        final AttributeType type = types.iterator().next();
        return (previous == null || previous.type() == type) ? type : UNKNOWN;
    }

     private static AttributeType inferValueType(Object value) {
        if (value instanceof Integer || value instanceof Double || value instanceof Float) {
            return NUMERIC;
        } else if (value instanceof TimeSeriesAttribute) {
            return TIME_SERIES;
        } else {
            String stringValue = value.toString();
            try {
                Double.parseDouble(stringValue);
                return NUMERIC;
            } catch(NumberFormatException e) {
                return STRING;
            }
        }
    }

    private static boolean isPrimitive(Object v) {
        return (v instanceof String) || (v instanceof Number);
    }
}
