package io.quartic.weyl.core.compute;

import io.quartic.weyl.core.compute.Histogram.Bucket;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.Feature;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import static io.quartic.weyl.core.attributes.AttributeUtils.isSimple;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

public class HistogramCalculator {
    public Collection<Histogram> calculate(Collection<? extends Feature> features) {
        final Map<AttributeName, Map<Object, Long>> counts = features.stream()
                .flatMap(f -> f.getAttributes().getAttributes().entrySet().stream())
                .filter(entry -> isSimple(entry.getValue()))
                .collect(groupingBy(Entry::getKey, groupingBy(Entry::getValue, counting())));

        return counts.entrySet().stream()
                .map(e -> new Histogram(e.getKey(), countsToBuckets(e.getValue())))
                .collect(toSet());
    }

    private Collection<Bucket> countsToBuckets(Map<Object, Long> counts) {
        return counts.entrySet().stream()
                .map(e -> new Bucket(e.getKey(), e.getValue()))
                .collect(toSet());
    }
}
