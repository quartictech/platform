package io.quartic.weyl.core.compute;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;

public enum SpatialPredicate {
        EQUALS,
        DISJOINT,
        TOUCHES,
        CONTAINS,
        COVERS,
        INTERSECTS,
        WITHIN,
        COVERED_BY,
        CROSSES,
        OVERLAPS;


    public boolean test(PreparedGeometry left, Geometry right) {
       switch (this) {
           case EQUALS:
               return left.equals(right);
           case DISJOINT:
               return left.disjoint(right);
           case TOUCHES:
               return left.touches(right);
           case CONTAINS:
               return left.contains(right);
           case COVERS:
               return left.covers(right);
           case INTERSECTS:
               return left.intersects(right);
           case WITHIN:
               return left.within(right);
           case COVERED_BY:
               return left.coveredBy(right);
           case CROSSES:
               return left.crosses(right);
           case OVERLAPS:
               return left.overlaps(right);
           default:
               throw new IllegalArgumentException("invalid predicate: " + this);
       }
    }
}
