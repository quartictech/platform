package io.quartic.weyl.core.attributes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.model.*;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.quartic.weyl.core.attributes.AttributeSchemaInferrer.inferSchema;
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
                        AttributeNameImpl.of("a"), AttributeImpl.of(AttributeType.NUMERIC, Optional.empty()),
                        AttributeNameImpl.of("b"), AttributeImpl.of(AttributeType.NUMERIC, Optional.of(ImmutableSet.of(456)))
                )));
    }

    private Feature feature(Map<String, ?> attributes) {
        final AttributesImpl.Builder builder = AttributesImpl.builder();
        attributes.forEach((k, v) -> builder.attribute(AttributeNameImpl.of(k), v));

        return FeatureImpl.builder()
                .entityId(EntityIdImpl.of("xyz"))
                .geometry(mock(Geometry.class))
                .attributes(builder.build())
                .build();
    }
}
