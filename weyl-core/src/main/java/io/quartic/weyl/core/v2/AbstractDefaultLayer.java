package io.quartic.weyl.core.v2;

import com.github.andrewoma.dexx.collection.Vector;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.github.davidmoten.rtree.internal.EntryDefault;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.SweetStyle;
import io.quartic.weyl.core.live.EnrichedFeedEvent;
import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.stream.Collectors;

@SweetStyle
@Value.Immutable
public abstract class AbstractDefaultLayer implements Layer {
    abstract Vector<Feature> features();
    abstract Vector<EnrichedFeedEvent> liveEvents();
    abstract RTree<Feature, Rectangle> spatialIndex();

    public static Layer empty() {
        return DefaultLayer.o.of(null, Vector.empty(), Vector.empty(), RTree.create());
    }

    private Rectangle jtsToRectangle(Geometry geometry) {
        Envelope envelope = geometry.getEnvelopeInternal();
        return Geometries.rectangle(envelope.getMinX(), envelope.getMinY(),
                envelope.getMaxX(), envelope.getMaxY());
    }

    @Override
    public AbstractDefaultLayer withFeatures(Collection<Feature> features) {
        RTree<Feature, Rectangle> spatialIndex = spatialIndex().add(
                features.stream().map(feature ->
                        EntryDefault.entry(feature, jtsToRectangle(feature.geometry())))
                .collect(Collectors.toList())
        );
        DefaultLayer.copyOf(this)
                .withFeatures(Vector.factory().newBuilder()
                        .
    }

    @Override
    public Iterable<Feature> query(Geometry geometry) {
        return spatialIndex().search(jtsToRectangle(geometry))
                .map(Entry::value)
                .toBlocking().toIterable();
    }
}
