package io.quartic.weyl.core.live;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.ImmutableFeature;
import io.quartic.weyl.core.utils.UidGenerator;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.getLast;
import static java.util.stream.Collectors.groupingBy;

public class LastKnownLocationAndTrackView implements LiveLayerView {

    private static Stream<Feature> makeTrack(UidGenerator<FeatureId> uidGenerator, List<Feature> history) {
        final Feature last = getLast(history);
        final GeometryFactory factory = last.geometry().getFactory();
        if (history.size() == 1) {
            return Stream.of(last);
        }
        return Stream.of(
                last,
                ImmutableFeature.builder()
                        .externalId(last.externalId())
                        .uid(uidGenerator.get())
                        .geometry(factory.createLineString(history.stream()
                                        .map(f -> f.geometry().getCoordinate())
                                        .collect(Collectors.toList())
                                        .toArray(new Coordinate[0])
                        ))
                        .metadata(last.metadata())
                        .build()
        );
    }

    @Override
    public Stream<Feature> compute(UidGenerator<FeatureId> uidGenerator, Collection<Feature> history) {
        Map<String, List<Feature>> historyById = history.stream()
                .sorted((a, b) -> Long.compare(a.uid().id(), b.uid().id()))
                .collect(groupingBy(Feature::externalId));

        return historyById.values()
                .stream()
                .flatMap(features -> makeTrack(uidGenerator, features));
    }
}
