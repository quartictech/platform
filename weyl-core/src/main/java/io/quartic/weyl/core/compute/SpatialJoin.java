package io.quartic.weyl.core.compute;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.AbstractLayer;
import io.quartic.weyl.core.model.IndexedFeature;
import org.immutables.value.Value;

import java.util.stream.Stream;

public class SpatialJoin {
    @SweetStyle
    @Value.Immutable
    public interface AbstractTuple {
        AbstractFeature left();
        AbstractFeature right();
    }

    public enum SpatialPredicate {
        CONTAINS,
        COVERS
    }

    private static boolean applySpatialPredicate(PreparedGeometry left, Geometry right, SpatialPredicate predicate) {
       switch (predicate) {
           case CONTAINS:
               return left.contains(right);
           case COVERS:
               return left.covers(right);
           default:
               throw new IllegalArgumentException("invalid predicate: " + predicate);
       }
    }


    @SuppressWarnings("unchecked")
    public static Stream<Tuple> innerJoin(AbstractLayer leftLayer, AbstractLayer rightLayer, SpatialPredicate predicate) {
       return leftLayer.indexedFeatures().parallelStream()
                .flatMap(left -> rightLayer.spatialIndex()
                        .query(left.feature().geometry().getEnvelopeInternal())
                        .stream()
                        .filter(o -> applySpatialPredicate(left.preparedGeometry(), ((IndexedFeature) o).feature().geometry(), predicate))
                        .map(o -> Tuple.of(left.feature(), ((IndexedFeature) o).feature())));
    }
}
