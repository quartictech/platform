package io.quartic.weyl.core.compute;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.IndexedFeature;
import io.quartic.weyl.core.model.Layer;
import org.immutables.value.Value;

import java.util.stream.Stream;

public class SpatialJoiner {
    @SweetStyle
    @Value.Immutable
    public interface Tuple {
        Feature left();
        Feature right();
    }

    @SuppressWarnings("unchecked")
    public Stream<Tuple> innerJoin(Layer leftLayer, Layer rightLayer, SpatialPredicate predicate) {
       return leftLayer.indexedFeatures().parallelStream()
                .flatMap(left -> rightLayer.spatialIndex()
                        .query(left.feature().geometry().getEnvelopeInternal())
                        .stream()
                        .filter(o -> predicate.test(left.preparedGeometry(), ((IndexedFeature) o).feature().geometry()))
                        .map(o -> TupleImpl.of(left.feature(), ((IndexedFeature) o).feature())));
    }
}
