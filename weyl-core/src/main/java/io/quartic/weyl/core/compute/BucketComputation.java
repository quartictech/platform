package io.quartic.weyl.core.compute;

import com.google.common.collect.ImmutableList;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.LayerSpec;
import io.quartic.weyl.core.LayerSpecImpl;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.compute.SpatialJoiner.Tuple;
import io.quartic.weyl.core.model.Attribute;
import io.quartic.weyl.core.model.AttributeImpl;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.AttributeSchema;
import io.quartic.weyl.core.model.AttributeSchemaImpl;
import io.quartic.weyl.core.model.AttributeType;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadataImpl;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.NakedFeatureImpl;
import io.quartic.weyl.core.source.LayerUpdateImpl;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Maps.newHashMap;
import static io.quartic.weyl.core.compute.SpatialJoiner.SpatialPredicate.CONTAINS;
import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static rx.Observable.just;

@SweetStyle
@Value.Immutable
public abstract class BucketComputation implements LayerComputation {
    protected abstract LayerId layerId();
    protected abstract BucketSpec bucketSpec();

    @Value.Default
    protected SpatialJoiner joiner() {
        return new SpatialJoiner();
    }

    private final AttributesFactory attributesFactory = new AttributesFactory();

    @Override
    public List<LayerId> dependencies() {
        return ImmutableList.of(bucketSpec().features(), bucketSpec().buckets());
    }

    @Override
    public LayerSpec spec(List<Layer> dependencies) {
        return new BucketImplementation(dependencies.get(0), dependencies.get(1)).compute();
    }

    private class BucketImplementation {
        private final Layer featureLayer;
        private final Layer bucketLayer;

        private BucketImplementation(Layer featureLayer, Layer bucketLayer) {
            this.featureLayer = featureLayer;
            this.bucketLayer = bucketLayer;
        }

        private LayerSpec compute() {
            final ForkJoinPool forkJoinPool = new ForkJoinPool(4);

            try {
                return results(forkJoinPool.submit(this::bucketData).get(), schema());
            } catch (InterruptedException | ExecutionException e) {
                // TODO: what do we do?
                return null;
            }
        }

        private LayerSpec results(Collection<NakedFeature> features, AttributeSchema schema) {
            final String name = String.format("%s (bucketed)",
                    rawAttributeName());
            final String description = String.format("%s bucketed by %s aggregating by %s",
                    rawAttributeName(),
                    bucketLayer.metadata().name(),
                    bucketSpec().aggregation().toString());
            return LayerSpecImpl.of(
                    layerId(),
                    LayerMetadataImpl.builder()
                            .name(name)
                            .description(description)
                            .build(),
                    IDENTITY_VIEW,
                    schema,
                    true,
                    just(LayerUpdateImpl.of(features))
            );
        }

        private AttributeSchema schema() {
            final AttributeSchema originalSchema = bucketLayer.schema();

            Map<AttributeName, Attribute> attributeMap = newHashMap(originalSchema.attributes());
            Attribute newAttribute = AttributeImpl.builder()
                    .type(AttributeType.NUMERIC)
                    .build();
            attributeMap.put(attributeName(), newAttribute);

            return AttributeSchemaImpl
                    .copyOf(originalSchema)
                    .withAttributes(attributeMap)
                    .withBlessedAttributes(concat(singletonList(attributeName()), originalSchema.blessedAttributes()))
                    .withPrimaryAttribute(attributeName());
        }

        private Collection<NakedFeature> bucketData() {
            // The order here is that CONTAINS applies from left -> right and
            // the spatial index on the right layer is the one that is queried
            Map<Feature, List<Tuple>> groups = joiner().innerJoin(bucketLayer, featureLayer, CONTAINS)
                    .collect(groupingBy(Tuple::left));

            return groups.entrySet().parallelStream()
                    .map(this::featureForBucket)
                    .collect(toList());
        }

        private NakedFeature featureForBucket(Entry<Feature, List<Tuple>> entry) {
            Feature bucket = entry.getKey();
            Double value = bucketSpec().aggregation().aggregate(
                    bucket,
                    entry.getValue().stream().map(Tuple::right).collect(toList()));

            if (bucketSpec().normalizeToArea()) {
                if (bucket.geometry().getArea() > 0) {
                    value /= bucket.geometry().getArea();
                }
            }

            final AttributesFactory.AttributesBuilder builder = attributesFactory.builder(bucket.attributes());
            builder.put(rawAttributeName(), value);
            return NakedFeatureImpl.of(
                    Optional.of(bucket.entityId().uid()),
                    bucket.geometry(),
                    builder.build()
            );
        }

        private AttributeName attributeName() {
            return AttributeNameImpl.of(rawAttributeName());
        }

        private String rawAttributeName() {
            return featureLayer.metadata().name();
        }
    }
}
