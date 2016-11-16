package io.quartic.weyl.core.live;

import io.quartic.common.uid.UidGenerator;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.FeatureId;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.getLast;
import static java.util.stream.Collectors.groupingBy;

public class MostRecentGeometryView implements LayerView {
    @Override
    public Stream<AbstractFeature> compute(UidGenerator<FeatureId> uidGenerator, Collection<AbstractFeature> history) {
        // TODO: sorting by UID is wrong - we should sort by timestamp
        Map<String, List<AbstractFeature>> historyById = history.stream()
                .sorted((a, b) -> Long.compare(Long.valueOf(a.uid().uid()), Long.valueOf(b.uid().uid())))
                .collect(groupingBy(AbstractFeature::externalId));

        return historyById.entrySet().stream().map(entry -> getLast(entry.getValue()));
    }
}
