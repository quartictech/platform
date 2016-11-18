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
        List<AbstractFeature> features = Lists.newArrayList(
                feature(ImmutableMap.of(AttributeName.of("a"), 123, AttributeName.of("b"), 456)),
                feature(ImmutableMap.of(AttributeName.of("a"), 789))              // b is missing here
        );

        assertThat(inferSchema(features),
                equalTo(ImmutableMap.of(
                        AttributeName.of("a"), Attribute.of(AttributeType.NUMERIC, Optional.empty()),
                        AttributeName.of("b"), Attribute.of(AttributeType.NUMERIC, Optional.of(ImmutableSet.of(456)))
                )));
    }

    private AbstractFeature feature(Map<AttributeName, ?> attributes) {
        return Feature.builder()
                .uid(FeatureId.of("123"))
                .entityId(EntityId.of(LayerId.of("xyz"), "abc"))
                .geometry(mock(Geometry.class))
                .attributes(attributes)
                .build();
    }
}
