package io.quartic.weyl.core.attributes;

import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.model.Attribute;
import io.quartic.weyl.core.model.AttributeImpl;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.AttributeType;
import io.quartic.weyl.core.model.EntityIdImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static io.quartic.common.test.CollectionUtils.entry;
import static io.quartic.common.test.CollectionUtils.map;
import static io.quartic.weyl.core.attributes.AttributeSchemaInferrer.MAX_CATEGORIES;
import static io.quartic.weyl.core.attributes.AttributeSchemaInferrer.inferSchema;
import static io.quartic.weyl.core.model.AttributeType.NUMERIC;
import static io.quartic.weyl.core.model.AttributeType.STRING;
import static io.quartic.weyl.core.model.AttributeType.TIME_SERIES;
import static io.quartic.weyl.core.model.AttributeType.UNKNOWN;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class AttributeSchemaInferrerShould {

    @Test
    public void infer_numeric_type() throws Exception {
        assertThatTypeInferredFromValues(NUMERIC, emptyMap(), 123, 456);
    }

    @Test
    public void infer_numeric_type_when_stringified() throws Exception {
        assertThatTypeInferredFromValues(NUMERIC, emptyMap(), "123", "456");
    }

    @Test
    public void infer_string_type() throws Exception {
        assertThatTypeInferredFromValues(STRING, emptyMap(), "foo", "bar");
    }

    @Test
    public void infer_time_series_type() throws Exception {
        assertThatTypeInferredFromValues(TIME_SERIES, emptyMap(), mock(TimeSeriesAttribute.class));
    }

    @Test
    public void infer_unknown_type_when_mixed() throws Exception {
        assertThatTypeInferredFromValues(UNKNOWN, emptyMap(), "foo", 123);
    }

    @Test
    public void infer_known_type_when_same_as_previous() throws Exception {
        assertThatTypeInferredFromValues(NUMERIC, map(entry(name("a"), AttributeImpl.of(NUMERIC, Optional.of(newHashSet(123))))), 123);
    }

    @Test
    public void infer_unknown_type_when_different_to_previous() throws Exception {
        assertThatTypeInferredFromValues(UNKNOWN, map(entry(name("a"), AttributeImpl.of(NUMERIC, Optional.of(newHashSet("foo"))))), "foo");
    }

    private void assertThatTypeInferredFromValues(AttributeType type, Map<AttributeName, Attribute> previous, Object... values) {
        final List<Feature> features = features("a", values);

        final Map<AttributeName, Attribute> schema = inferSchema(features, previous);
        assertThat(schema.entrySet(), hasSize(1));
        assertThat(schema, hasKey(name("a")));
        assertThat(schema.get(name("a")).type(), equalTo(type));
    }

    @Test
    public void infer_categories() throws Exception {
        final List<Feature> features = features("a", "foo", "bar", "foo");

        assertThat(inferSchema(features, emptyMap()), equalTo(map(
                entry(name("a"), AttributeImpl.of(STRING, Optional.of(newHashSet("foo", "bar"))))
        )));
    }

    @Test
    public void infer_no_categories_when_no_values() throws Exception {
        final List<Feature> features = features("a", new Object[] { null });    // Synthesise a feature with a missing attribute

        assertThat(inferSchema(features, emptyMap()), equalTo(map(
                entry(name("a"), AttributeImpl.of(UNKNOWN, Optional.empty()))   // Specifically looking for Optional.empty() here
        )));
    }

    @Test
    public void infer_no_categories_when_too_many() throws Exception {
        final List<Feature> features = features("a", distinctValuesPlusOneRepeated(MAX_CATEGORIES + 1));

        assertThat(inferSchema(features, emptyMap()), equalTo(map(
                entry(name("a"), AttributeImpl.of(STRING, Optional.empty()))
        )));
    }

    @Test
    public void infer_categories_as_union_with_previous() throws Exception {
        final Map<AttributeName, Attribute> previous = map(
                entry(name("a"), AttributeImpl.of(STRING, Optional.of(newHashSet("foo", "bar"))))
        );
        final List<Feature> features = features("a", "foo", "baz");

        assertThat(inferSchema(features, previous), equalTo(map(
                entry(name("a"), AttributeImpl.of(STRING, Optional.of(newHashSet("foo", "bar", "baz"))))
        )));
    }

    @Test
    public void infer_no_categories_when_union_has_too_many() throws Exception {
        final Map<AttributeName, Attribute> previous = map(
                entry(name("a"), AttributeImpl.of(STRING, Optional.of(newHashSet("bar"))))
        );
        final List<Feature> features = features("a", distinctValuesPlusOneRepeated(MAX_CATEGORIES));

        assertThat(inferSchema(features, previous), equalTo(map(
                entry(name("a"), AttributeImpl.of(STRING, Optional.empty()))
        )));
    }

    @Test
    public void infer_no_categories_when_previous_had_too_many() throws Exception {
        final Map<AttributeName, Attribute> previous = map(
                entry(name("a"), AttributeImpl.of(STRING, Optional.empty()))
        );
        final List<Feature> features = features("a", "foo", "baz");

        assertThat(inferSchema(features, previous), equalTo(map(
                entry(name("a"), AttributeImpl.of(STRING, Optional.empty()))
        )));
    }

    @Test
    public void infer_no_categories_for_non_primitive_types() throws Exception {
        final List<Feature> features = features("a", "foo", new Object(), "baz");   // Second item is not a primitive type

        assertThat(inferSchema(features, emptyMap()), equalTo(map(
                entry(name("a"), AttributeImpl.of(STRING, Optional.of(newHashSet("foo", "baz"))))
        )));
    }

    @Test
    public void ignore_missing_attributes() throws Exception {
        final List<Feature> features = newArrayList(
                feature(map(entry(name("a"), 123), entry(name("b"), 456))),
                feature(map(entry(name("a"), 789), entry(name("b"), null)))
        );

        assertThat(inferSchema(features, emptyMap()), equalTo(map(
                entry(name("a"), AttributeImpl.of(NUMERIC, Optional.of(newHashSet(123, 789)))),
                entry(name("b"), AttributeImpl.of(NUMERIC, Optional.of(newHashSet(456))))
        )));
    }

    @Test
    public void return_empty_schema_if_no_features() throws Exception {
        assertThat(inferSchema(emptyList(), emptyMap()).entrySet(), empty());
    }

    private List<Feature> features(String name, Object... values) {
        return stream(values)
                .map(v -> feature(map(entry(name(name), v))))
                .collect(toList());
    }

    private Feature feature(Map<AttributeName, Object> attributes) {
        return FeatureImpl.builder()
                .entityId(EntityIdImpl.of("xyz"))
                .geometry(mock(Geometry.class))
                .attributes(() -> attributes)
                .build();
    }

    private AttributeNameImpl name(String name) {
        return AttributeNameImpl.of(name);
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
