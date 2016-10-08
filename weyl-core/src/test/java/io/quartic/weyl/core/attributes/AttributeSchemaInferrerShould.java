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
    public void ignore_missing_properties() throws Exception {
        List<Feature> features = Lists.newArrayList(
                feature(ImmutableMap.of("a", 123, "b", 456)),
                feature(ImmutableMap.of("a", 789))              // b is missing here
        );

        assertThat(inferSchema(features),
                equalTo(ImmutableMap.of(
                        "a", Attribute.of(AttributeType.NUMERIC, Optional.empty()),
                        "b", Attribute.of(AttributeType.NUMERIC, Optional.of(ImmutableSet.of(456)))
                )));
    }

    private Feature feature(Map<String, ?> properties) {
        return ImmutableFeature.builder()
                .uid(FeatureId.of("123"))
                .externalId("abc")
                .geometry(mock(Geometry.class))
                .metadata(properties)
                .build();
    }
}
