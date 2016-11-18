package io.quartic.weyl.core.live;

import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.EntityId;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.getLast;
import static java.util.stream.Collectors.groupingBy;

public class MostRecentGeometryView implements LayerView {
    @Override
    public Stream<AbstractFeature> compute(Collection<AbstractFeature> history) {
        Map<EntityId, List<AbstractFeature>> historyById = history.stream()
                .collect(groupingBy(AbstractFeature::entityId));

        return historyById.entrySet().stream().map(entry -> getLast(entry.getValue()));
    }
}
