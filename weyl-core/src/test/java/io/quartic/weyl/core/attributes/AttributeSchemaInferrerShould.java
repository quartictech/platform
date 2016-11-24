package io.quartic.weyl.core.attributes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.model.*;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import static io.quartic.weyl.core.attributes.AttributeSchemaInferrer.inferSchema;
import static io.quartic.weyl.core.model.AttributeType.NUMERIC;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class AttributeSchemaInferrerShould {
    @Test
    public void ignore_missing_attributes() throws Exception {
        List<Feature> features = Lists.newArrayList(
                feature(ImmutableMap.of("a", 123, "b", 456)),
                feature(ImmutableMap.of("a", 789))              // b is missing here
        );

        assertThat(inferSchema(features),
                equalTo(ImmutableMap.of(
                        name("a"), AttributeImpl.of(NUMERIC, Optional.empty()),
                        name("b"), AttributeImpl.of(NUMERIC, Optional.of(ImmutableSet.of(456)))
                )));
    }

    @Test
    public void return_empty_schema_if_no_features() throws Exception {
        assertThat(inferSchema(emptyList()).entrySet(), empty());
    }

    private Feature feature(Map<String, ?> attributes) {
        return FeatureImpl.builder()
                .entityId(EntityIdImpl.of("xyz"))
                .geometry(mock(Geometry.class))
                .attributes(() -> attributes.entrySet().stream().collect(toMap(e -> name(e.getKey()), Entry::getValue)))
                .build();
    }

    private AttributeNameImpl name(String name) {
        return AttributeNameImpl.of(name);
    }
}
