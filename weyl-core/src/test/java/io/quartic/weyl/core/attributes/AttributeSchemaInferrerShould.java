package io.quartic.weyl.core.attributes;

import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.model.*;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static io.quartic.common.CollectionUtils.entry;
import static io.quartic.common.CollectionUtils.map;
import static io.quartic.weyl.core.attributes.AttributeSchemaInferrer.MAX_CATEGORIES;
import static io.quartic.weyl.core.attributes.AttributeSchemaInferrer.inferSchema;
import static io.quartic.weyl.core.model.AttributeType.*;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class AttributeSchemaInferrerShould {

    @Test
    public void infer_numeric_type() throws Exception {
        assertThatTypeInferredFromValues(NUMERIC, 123, 456);
    }

    @Test
    public void infer_numeric_type_when_stringified() throws Exception {
        assertThatTypeInferredFromValues(NUMERIC, "123", "456");
    }

    @Test
    public void infer_string_type() throws Exception {
        assertThatTypeInferredFromValues(STRING, "foo", "bar");
    }

    @Test
    public void infer_timeseries_type() throws Exception {
        assertThatTypeInferredFromValues(TIME_SERIES, mock(TimeSeriesAttribute.class));
    }

    @Test
    public void infer_unknown_type_when_mixed() throws Exception {
        assertThatTypeInferredFromValues(UNKNOWN, "foo", 123);
    }

    private void assertThatTypeInferredFromValues(AttributeType type, Object... values) {
        List<Feature> features = features("a", values);

        assertThat(inferSchema(features), equalTo(map(entry(name("a"), AttributeImpl.of(type, Optional.empty())))));
    }

    @Test
    public void infer_categories() throws Exception {
        List<Feature> features = features("a", "foo", "bar", "foo");

        assertThat(inferSchema(features), equalTo(map(
                entry(name("a"), AttributeImpl.of(STRING, Optional.of(newHashSet("foo", "bar"))))
        )));
    }

    @Test
    public void infer_no_categories_when_all_distinct() throws Exception {
        List<Feature> features = features("a", "foo", "bar", "baz");

        assertThat(inferSchema(features), equalTo(map(
                entry(name("a"), AttributeImpl.of(STRING, Optional.empty()))
        )));
    }

    @Test
    public void infer_no_categories_when_too_many() throws Exception {
        String[] values = new String[MAX_CATEGORIES + 2];
        for (int i = 0; i < MAX_CATEGORIES + 1; i++) {
            values[i] = "foo" + i;
        }
        values[MAX_CATEGORIES + 1] = "foo0";    // To avoid the all-distinct check

        List<Feature> features = features("a", values);

        assertThat(inferSchema(features), equalTo(map(
                entry(name("a"), AttributeImpl.of(STRING, Optional.empty()))
        )));
    }

    @Test
    public void ignore_missing_attributes() throws Exception {
        List<Feature> features = newArrayList(
                feature(map(entry(name("a"), 123), entry(name("b"), 456))),
                feature(map(entry(name("a"), 789), entry(name("b"), null)))
        );

        assertThat(inferSchema(features), equalTo(map(
                entry(name("a"), AttributeImpl.of(NUMERIC, Optional.empty())),
                entry(name("b"), AttributeImpl.of(NUMERIC, Optional.of(newHashSet(456))))
        )));
    }

    @Test
    public void return_empty_schema_if_no_features() throws Exception {
        assertThat(inferSchema(emptyList()).entrySet(), empty());
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
}
