package io.quartic.weyl.core.compute;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.compute.Histogram.Bucket;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class HistogramCalculatorShould {
    private static final AttributeName SPECIES = new AttributeName("species");
    private static final AttributeName NAME = new AttributeName("name");
    private final HistogramCalculator calculator = new HistogramCalculator();

    @Test
    public void count_distinct_values_for_attribute() throws Exception {
        List<Feature> features = newArrayList(
                feature(ImmutableMap.of(NAME, "Alice")),
                feature(ImmutableMap.of(NAME, "Bob")),
                feature(ImmutableMap.of(NAME, "Alice"))
        );

        assertThat(calculator.calculate(features),
                equalTo(ImmutableSet.of(
                    new Histogram(NAME, ImmutableSet.of(new Bucket("Alice", 2L), new Bucket("Bob", 1L)))
                )));
    }

    @Test
    public void count_distinct_values_for_multiple_attributes() throws Exception {
        List<Feature> features = newArrayList(
                feature(ImmutableMap.of(NAME, "Alice", SPECIES, "dog")),
                feature(ImmutableMap.of(NAME, "Bob", SPECIES, "dog")),
                feature(ImmutableMap.of(NAME, "Alice", SPECIES, "cat"))
        );

        assertThat(calculator.calculate(features),
                equalTo(ImmutableSet.of(
                        new Histogram(NAME, ImmutableSet.of(new Bucket("Alice", 2L), new Bucket("Bob", 1L))),
                        new Histogram(SPECIES, ImmutableSet.of(new Bucket("dog", 2L), new Bucket("cat", 1L)))
                )));
    }

    @Test
    public void handle_missing_attributes() throws Exception {
        List<Feature> features = newArrayList(
                feature(ImmutableMap.of(NAME, "Alice", SPECIES, "dog")),
                feature(ImmutableMap.of(NAME, "Bob")),
                feature(ImmutableMap.of(NAME, "Alice", SPECIES, "cat"))
        );

        assertThat(calculator.calculate(features),
                equalTo(ImmutableSet.of(
                        new Histogram(NAME, ImmutableSet.of(new Bucket("Alice", 2L), new Bucket("Bob", 1L))),
                        new Histogram(SPECIES, ImmutableSet.of(new Bucket("dog", 1L), new Bucket("cat", 1L)))
                )));
    }

    private Feature feature(Map<AttributeName, Object> attributes) {
        return new Feature(new EntityId("def"), mock(Geometry.class), () -> attributes);
    }
}
