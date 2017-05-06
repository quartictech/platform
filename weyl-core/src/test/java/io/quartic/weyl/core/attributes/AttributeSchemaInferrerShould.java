package io.quartic.weyl.core.attributes;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.model.Attribute;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeType;
import io.quartic.weyl.core.model.DynamicSchema;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.StaticSchema;
import kotlin.Pair;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static io.quartic.common.test.CollectionUtilsKt.entry;
import static io.quartic.common.test.CollectionUtilsKt.map;
import static io.quartic.weyl.core.attributes.AttributeSchemaInferrer.MAX_CATEGORIES;
import static io.quartic.weyl.core.attributes.AttributeSchemaInferrer.inferSchema;
import static io.quartic.weyl.core.model.AttributeType.NUMERIC;
import static io.quartic.weyl.core.model.AttributeType.STRING;
import static io.quartic.weyl.core.model.AttributeType.TIMESTAMP;
import static io.quartic.weyl.core.model.AttributeType.TIME_SERIES;
import static io.quartic.weyl.core.model.AttributeType.UNKNOWN;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AttributeSchemaInferrerShould {

    @Test
    public void infer_numeric_type() throws Exception {
        assertThatTypeInferredFromValues(NUMERIC, schema(), 123, 456);
    }

    @Test
    public void infer_numeric_type_when_stringified() throws Exception {
        assertThatTypeInferredFromValues(NUMERIC, schema(), "123", "456");
    }

    @Test
    public void infer_string_type() throws Exception {
        assertThatTypeInferredFromValues(STRING, schema(), "foo", "bar");
    }

    @Test
    public void infer_time_series_type() throws Exception {
        assertThatTypeInferredFromValues(TIME_SERIES, schema(), mock(TimeSeriesAttribute.class));
    }

    @Test
    public void infer_unknown_type_when_mixed() throws Exception {
        assertThatTypeInferredFromValues(UNKNOWN, schema(), "foo", 123);
    }

    @Test
    public void infer_known_type_when_same_as_previous() throws Exception {
        assertThatTypeInferredFromValues(NUMERIC, schema(entry(name("a"), new Attribute(NUMERIC, newHashSet(123)))), 123);
    }

    @Test
    public void infer_unknown_type_when_different_to_previous() throws Exception {
        assertThatTypeInferredFromValues(UNKNOWN, schema(entry(name("a"), new Attribute(NUMERIC, newHashSet("foo")))), "foo");
    }

    private void assertThatTypeInferredFromValues(AttributeType type, DynamicSchema previous, Object... values) {
        final List<Feature> features = features("a", values);

        final DynamicSchema schema = inferSchema(features, previous, new StaticSchema());
        assertThat(schema.getAttributes().entrySet(), hasSize(1));
        assertThat(schema.getAttributes(), hasKey(name("a")));
        assertThat(schema.getAttributes().get(name("a")).getType(), equalTo(type));
    }

    @Test
    public void infer_categories() throws Exception {
        final List<Feature> features = features("a", "foo", "bar", "foo");

        assertThat(inferSchema(features, schema(), new StaticSchema()), equalTo(schema(
                entry(name("a"), new Attribute(STRING, newHashSet("foo", "bar")))
        )));
    }

    @Test
    public void infer_categories_for_booleans() throws Exception {
        final List<Feature> features = features("a", true, false, true);

        assertThat(inferSchema(features, schema(), new StaticSchema()), equalTo(schema(
                entry(name("a"), new Attribute(STRING, newHashSet(true, false)))
        )));
    }

    @Test
    public void infer_no_categories_when_no_values() throws Exception {
        final List<Feature> features = features("a", new Object[] { null });    // Synthesise a feature with a missing attribute

        assertThat(inferSchema(features, schema(), new StaticSchema()), equalTo(schema(
                entry(name("a"), new Attribute(UNKNOWN, null))   // Specifically looking for null here
        )));
    }

    @Test
    public void infer_no_categories_when_too_many() throws Exception {
        final List<Feature> features = features("a", (Object[]) distinctValuesPlusOneRepeated(MAX_CATEGORIES + 1));

        assertThat(inferSchema(features, schema(), new StaticSchema()), equalTo(schema(
                entry(name("a"), new Attribute(STRING, null))
        )));
    }

    @Test
    public void infer_categories_even_when_too_many_if_statically_declared_as_categorical() throws Exception {
        final String[] allTheThings = distinctValuesPlusOneRepeated(MAX_CATEGORIES + 1);
        final List<Feature> features = features("a", (Object[]) allTheThings);

        final StaticSchema staticSchema = mock(StaticSchema.class);
        when(staticSchema.getCategoricalAttributes()).thenReturn(newHashSet(name("a")));

        assertThat(inferSchema(features, schema(), staticSchema), equalTo(schema(
                entry(name("a"), new Attribute(STRING, newHashSet(allTheThings)))
        )));
    }

    @Test
    public void infer_categories_as_union_with_previous() throws Exception {
        final DynamicSchema previous = schema(
                entry(name("a"), new Attribute(STRING, newHashSet("foo", "bar")))
        );
        final List<Feature> features = features("a", "foo", "baz");

        assertThat(inferSchema(features, previous, new StaticSchema()), equalTo(schema(
                entry(name("a"), new Attribute(STRING, newHashSet("foo", "bar", "baz")))
        )));
    }

    @Test
    public void infer_no_categories_when_union_has_too_many() throws Exception {
        final DynamicSchema previous = schema(
                entry(name("a"), new Attribute(STRING, newHashSet("bar")))
        );
        final List<Feature> features = features("a", (Object[]) distinctValuesPlusOneRepeated(MAX_CATEGORIES));

        assertThat(inferSchema(features, previous, new StaticSchema()), equalTo(schema(
                entry(name("a"), new Attribute(STRING, null))
        )));
    }

    @Test
    public void infer_no_categories_when_previous_had_too_many() throws Exception {
        final DynamicSchema previous = schema(
                entry(name("a"), new Attribute(STRING, null))
        );
        final List<Feature> features = features("a", "foo", "baz");

        assertThat(inferSchema(features, previous, new StaticSchema()), equalTo(schema(
                entry(name("a"), new Attribute(STRING, null))
        )));
    }

    @Test
    public void infer_no_categories_for_non_primitive_types() throws Exception {
        final List<Feature> features = features("a", "foo", new Object(), "baz");   // Second item is not a primitive type

        assertThat(inferSchema(features, schema(), new StaticSchema()), equalTo(schema(
                entry(name("a"), new Attribute(STRING, newHashSet("foo", "baz")))
        )));
    }

    @Test
    public void ignore_missing_attributes() throws Exception {
        final List<Feature> features = newArrayList(
                feature(map(entry(name("a"), 123), entry(name("b"), 456))),
                feature(map(entry(name("a"), 789), entry(name("b"), null)))
        );

        assertThat(inferSchema(features, schema(), new StaticSchema()), equalTo(schema(
                entry(name("a"), new Attribute(NUMERIC, newHashSet(123, 789))),
                entry(name("b"), new Attribute(NUMERIC, newHashSet(456)))
        )));
    }

    @Test
    public void return_empty_schema_if_no_features() throws Exception {
        assertThat(inferSchema(emptyList(), schema(), new StaticSchema()), equalTo(schema()));
    }

    @Test
    public void override_schema_with_attribute_types() {
        final List<Feature> features = newArrayList(
                feature(map(entry(name("a"), "hello"), entry(name("b"), 123)))
        );
        final StaticSchema staticSchema = mock(StaticSchema.class);
        when(staticSchema.getAttributeTypes()).thenReturn(ImmutableMap.of(name("a"), AttributeType.TIMESTAMP));

        assertThat(inferSchema(features, schema(), staticSchema), equalTo(
                schema(
                        entry(name("a"), new Attribute(TIMESTAMP, newHashSet("hello"))),
                        entry(name("b"), new Attribute(NUMERIC, newHashSet(123)))
                )
        ));
    }

    @Test
    public void not_incorrectly_override_schema_with_attribute_types() {
        final List<Feature> features = newArrayList(
                feature(map(entry(name("b"), "hello")))
        );
        final StaticSchema staticSchema = mock(StaticSchema.class);
        when(staticSchema.getAttributeTypes()).thenReturn(ImmutableMap.of(name("a"), AttributeType.TIMESTAMP));

        assertThat(inferSchema(features, schema(), staticSchema), equalTo(
                schema(
                        entry(name("b"), new Attribute(STRING, newHashSet("hello")))
                )
        ));
    }

    @SafeVarargs
    private final DynamicSchema schema(Pair<AttributeName, Attribute>... entries) {
        return new DynamicSchema(map(entries));
    }

    private List<Feature> features(String name, Object... values) {
        return stream(values)
                .map(v -> feature(map(entry(name(name), v))))
                .collect(toList());
    }

    private Feature feature(Map<AttributeName, Object> attributes) {
        return new Feature(new EntityId("xyz"), mock(Geometry.class), () -> attributes);
    }

    private AttributeName name(String name) {
        return new AttributeName(name);
    }

    private String[] distinctValuesPlusOneRepeated(int num) {
        final String[] values = new String[num + 1];
        for (int i = 0; i < num; i++) {
            values[i] = "foo" + i;
        }
        values[num] = "foo0";    // To avoid the all-distinct check
        return values;
    }
}
