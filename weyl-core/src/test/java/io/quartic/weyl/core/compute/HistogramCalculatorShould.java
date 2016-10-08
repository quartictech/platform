package io.quartic.weyl.core.compute;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.ImmutableFeature;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class HistogramCalculatorShould {
    private final HistogramCalculator calculator = new HistogramCalculator();

    @Test
    public void count_distinct_values_for_property() throws Exception {
        List<Feature> features = newArrayList(
                feature(ImmutableMap.of("name", "Alice")),
                feature(ImmutableMap.of("name", "Bob")),
                feature(ImmutableMap.of("name", "Alice"))
        );

        assertThat(calculator.calculate(features),
                equalTo(ImmutableSet.of(
                    Histogram.of("name", ImmutableSet.of(Bucket.of("Alice", 2L), Bucket.of("Bob", 1L)))
                )));
    }

    @Test
    public void count_distinct_values_for_multiple_properties() throws Exception {
        List<Feature> features = newArrayList(
                feature(ImmutableMap.of("name", "Alice", "species", "dog")),
                feature(ImmutableMap.of("name", "Bob", "species", "dog")),
                feature(ImmutableMap.of("name", "Alice", "species", "cat"))
        );

        assertThat(calculator.calculate(features),
                equalTo(ImmutableSet.of(
                        Histogram.of("name", ImmutableSet.of(Bucket.of("Alice", 2L), Bucket.of("Bob", 1L))),
                        Histogram.of("species", ImmutableSet.of(Bucket.of("dog", 2L), Bucket.of("cat", 1L)))
                )));
    }

    @Test
    public void handle_missing_properties() throws Exception {
        List<Feature> features = newArrayList(
                feature(ImmutableMap.of("name", "Alice", "species", "dog")),
                feature(ImmutableMap.of("name", "Bob")),
                feature(ImmutableMap.of("name", "Alice", "species", "cat"))
        );

        assertThat(calculator.calculate(features),
                equalTo(ImmutableSet.of(
                        Histogram.of("name", ImmutableSet.of(Bucket.of("Alice", 2L), Bucket.of("Bob", 1L))),
                        Histogram.of("species", ImmutableSet.of(Bucket.of("dog", 1L), Bucket.of("cat", 1L)))
                )));
    }


    private Feature feature(Map<String, ?> properties) {
        return ImmutableFeature.builder()
                .uid(FeatureId.of("abc"))
                .externalId("def")
                .geometry(mock(Geometry.class))
                .putAllMetadata(properties)
                .build();
    }
}
