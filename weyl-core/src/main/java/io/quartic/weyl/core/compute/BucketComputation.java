package io.quartic.weyl.core.compute;

import com.google.common.collect.ImmutableList;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.compute.SpatialJoiner.Tuple;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.model.LayerPopulator;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.StaticSchema;
import rx.Observable;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.api.LayerUpdateType.REPLACE;
import static io.quartic.weyl.core.compute.SpatialPredicate.CONTAINS;
import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class BucketComputation implements LayerPopulator {
    private final LayerId layerId;
    private final BucketSpec bucketSpec;
    private final SpatialJoiner joiner;
    private final Clock clock;
    private final AttributesFactory attributesFactory = new AttributesFactory();

    public BucketComputation(LayerId layerId, BucketSpec bucketSpec) {
        this(layerId, bucketSpec, new SpatialJoiner(), Clock.systemUTC());
    }

    public BucketComputation(LayerId layerId, BucketSpec bucketSpec, SpatialJoiner joiner, Clock clock) {
        this.layerId = layerId;
        this.bucketSpec = bucketSpec;
        this.joiner = joiner;
        this.clock = clock;
    }

    @Override
    public List<LayerId> dependencies() {
        return ImmutableList.of(bucketSpec.getFeatures(), bucketSpec.getBuckets());
    }

    @Override
    public LayerSpec spec(List<Layer> dependencies) {
        final Layer featureLayer = dependencies.get(0);
        final Layer bucketLayer = dependencies.get(1);

        final String featureName = featureLayer.getSpec().getMetadata().getName();
        final String bucketName = bucketLayer.getSpec().getMetadata().getName();

        return new LayerSpec(
                layerId,
                new LayerMetadata(
                        String.format("%s (bucketed)", featureName),
                        String.format("%s bucketed by %s aggregating by %s", featureName, bucketName, bucketSpec.getAggregation().describe()),
                        String.format("%s / %s", featureLayer.getSpec().getMetadata().getAttribution(), bucketLayer.getSpec().getMetadata().getAttribution()),
                        clock.instant()
                ),
                IDENTITY_VIEW,
                schemaFrom(bucketLayer.getSpec().getStaticSchema(), featureName),
                true
        );
    }

    private StaticSchema schemaFrom(StaticSchema original, String rawAttributeName) {
        final AttributeName attributeName = new AttributeName(rawAttributeName);

        // TODO: Kotlin-ify this
        return new StaticSchema(
                original.getTitleAttribute(),
                attributeName,
                original.getImageAttribute(),
                newArrayList(concat(singletonList(attributeName), original.getBlessedAttributes())),
                original.getCategoricalAttributes(),
                original.getAttributeTypes()
        );
    }

    @Override
    public Observable<LayerUpdate> updates(List<Layer> dependencies) {
        final Layer featureLayer = dependencies.get(0);
        final Layer bucketLayer = dependencies.get(1);

        final ForkJoinPool forkJoinPool = new ForkJoinPool(4);
        try {
            return Observable.<LayerUpdate>never().startWith(new LayerUpdate(
                    REPLACE,
                    forkJoinPool.submit(() -> {
                        // The order here is that CONTAINS applies from left -> right and
                        // the spatial index on the right layer is the one that is queried
                        Map<Feature, List<Tuple>> groups = joiner
                                .innerJoin(bucketLayer, featureLayer, CONTAINS)
                                .collect(groupingBy(Tuple::getLeft));

                        return groups.entrySet().parallelStream()
                                .map(entry -> featureForBucket(featureLayer.getSpec().getMetadata().getName(), entry))
                                .collect(toList());
                    }).get()
            ));
        } catch (ExecutionException | InterruptedException e) {
            // TODO: this is naughty, we shouldn't be wrapping an IE in a RE
            throw new RuntimeException("Error during computation", e);
        }
    }

    private NakedFeature featureForBucket(String attributeName, Entry<Feature, List<Tuple>> entry) {
        Feature bucket = entry.getKey();
        Double value = bucketSpec.getAggregation().aggregate(
                bucket,
                entry.getValue().stream().map(Tuple::getRight).collect(toList()));

        if (bucketSpec.getNormalizeToArea()) {
            if (bucket.getGeometry().getArea() > 0) {
                value /= bucket.getGeometry().getArea();
            }
        }

        final AttributesFactory.AttributesBuilder builder = attributesFactory.builder(bucket.getAttributes());
        builder.put(attributeName, value);
        return new NakedFeature(
                bucket.getEntityId().getUid(),
                bucket.getGeometry(),
                builder.build()
        );
    }
}
