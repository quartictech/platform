package io.quartic.weyl.core.live;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.common.uid.UidGenerator;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.getLast;
import static java.util.stream.Collectors.groupingBy;

public class LastKnownLocationAndTrackView implements LayerView {

    private static Stream<AbstractFeature> makeTrack(UidGenerator<FeatureId> uidGenerator, List<AbstractFeature> history) {
        final AbstractFeature last = getLast(history);
        final GeometryFactory factory = last.geometry().getFactory();
        if (history.size() == 1) {
            return Stream.of(last);
        }
        return Stream.of(
                last,
                Feature.builder()
                        .entityId(last.entityId())  // TODO: this is wrong
                        .uid(uidGenerator.get())
                        .geometry(factory.createLineString(history.stream()
                                        .map(f -> f.geometry().getCoordinate())
                                        .collect(Collectors.toList())
                                        .toArray(new Coordinate[0])
                        ))
                        .attributes(last.attributes())
                        .build()
        );
    }

    @Override
    public Stream<AbstractFeature> compute(UidGenerator<FeatureId> uidGenerator, Collection<AbstractFeature> history) {
        // TODO: sorting by UID is wrong - we should sort by timestamp
        Map<EntityId, List<AbstractFeature>> historyById = history.stream()
                .sorted((a, b) -> Long.compare(Long.valueOf(a.uid().uid()), Long.valueOf(b.uid().uid())))
                .collect(groupingBy(AbstractFeature::entityId));

        return historyById.values()
                .stream()
                .flatMap(features -> makeTrack(uidGenerator, features));
    }
}
