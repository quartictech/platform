package io.quartic.weyl.core.compute;

import com.google.common.collect.ImmutableList;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.model.LayerPopulator;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.NakedFeature;
import org.immutables.value.Value;
import rx.Observable;

import java.time.Clock;
import java.util.Collection;
import java.util.List;

import static io.quartic.weyl.api.LayerUpdateType.REPLACE;
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
        return ImmutableList.of(spatialPredicateSpec().getLayerA(), spatialPredicateSpec().getLayerB());
    }

    @Override
    public LayerSpec spec(List<Layer> dependencies) {
        final Layer layerA = dependencies.get(0);
        final Layer layerB = dependencies.get(1);

        return new LayerSpec(
                layerId(),
                new LayerMetadata(
                        name(layerA, layerB, spatialPredicateSpec()),
                        name(layerA, layerB, spatialPredicateSpec()),
                        layerA.getSpec().getMetadata().getAttribution(),
                        clock().instant()
                ),
                IDENTITY_VIEW,
                layerA.getSpec().getStaticSchema(),
                true
        );
    }

    private static String name(Layer layerA, Layer layerB, SpatialPredicateSpec spatialPredicateSpec) {
        return String.format("%s %s %s",
                layerA.getSpec().getMetadata().getName(),
                spatialPredicateSpec.getPredicate(),
                layerB.getSpec().getMetadata().getName());
    }

    @Override
    public Observable<LayerUpdate> updates(List<Layer> dependencies) {
        final Layer layerA = dependencies.get(0);
        final Layer layerB = dependencies.get(1);

        Collection<NakedFeature> bufferedFeatures = layerA.getIndexedFeatures().parallelStream()
                .filter(featureA -> layerB.getFeatures().stream()
                        .anyMatch(featureB -> spatialPredicateSpec().getPredicate()
                                        .test(featureA.getPreparedGeometry(), featureB.getGeometry())))
                .map(feature -> new NakedFeature(
                        feature.getFeature().getEntityId().getUid(),
                        feature.getFeature().getGeometry(),
                        feature.getFeature().getAttributes())
                )
                .collect(toList());

        return Observable.<LayerUpdate>never().startWith(new LayerUpdate(REPLACE, bufferedFeatures));
    }
}
