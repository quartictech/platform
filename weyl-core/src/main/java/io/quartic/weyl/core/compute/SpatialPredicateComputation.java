package io.quartic.weyl.core.compute;

import com.google.common.collect.ImmutableList;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.api.LayerUpdateType;
import io.quartic.weyl.core.model.*;
import org.immutables.value.Value;
import rx.Observable;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static java.util.stream.Collectors.toList;

@SweetStyle
@Value.Immutable
public abstract class SpatialPredicateComputation implements LayerPopulator {
    protected abstract LayerId layerId();
    protected abstract SpatialPredicateSpec spatialPredicateSpec();

    @Value.Default
    protected Clock clock() {
        return Clock.systemUTC();
    }

    @Override
    public List<LayerId> dependencies() {
        return ImmutableList.of(spatialPredicateSpec().layerA(), spatialPredicateSpec().layerB());
    }

    @Override
    public LayerSpec spec(List<Layer> dependencies) {
        final Layer layerA = dependencies.get(0);
        final Layer layerB = dependencies.get(1);

        return LayerSpecImpl.of(
                layerId(),
                LayerMetadataImpl.builder()
                        .name(name(layerA, layerB, spatialPredicateSpec()))
                        .description(name(layerA, layerB, spatialPredicateSpec()))
                        .attribution(layerA.spec().metadata().attribution())
                        .registered(clock().instant())
                        .build(),
                IDENTITY_VIEW,
                layerA.spec().staticSchema(),
                true
        );
    }

    private static String name(Layer layerA, Layer layerB, SpatialPredicateSpec spatialPredicateSpec) {
        return String.format("%s %s %s",
                layerA.spec().metadata().name(),
                spatialPredicateSpec.predicate(),
                layerB.spec().metadata().name());
    }

    @Override
    public Observable<LayerUpdate> updates(List<Layer> dependencies) {
        final Layer layerA = dependencies.get(0);
        final Layer layerB = dependencies.get(1);

        Collection<NakedFeature> bufferedFeatures = layerA.indexedFeatures().parallelStream()
                .filter(featureA -> layerB.features().stream()
                        .anyMatch(featureB -> spatialPredicateSpec().predicate()
                                        .test(featureA.preparedGeometry(), featureB.geometry())))
                .map(feature -> NakedFeatureImpl.of(
                        Optional.of(feature.feature().entityId().getUid()),
                        feature.feature().geometry(),
                        feature.feature().attributes())
                )
                .collect(toList());

        return Observable.<LayerUpdate>never().startWith(LayerUpdateImpl.of(LayerUpdateType.REPLACE, bufferedFeatures));
    }
}
