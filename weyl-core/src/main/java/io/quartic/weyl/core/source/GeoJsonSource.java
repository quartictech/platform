package io.quartic.weyl.core.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.geojson.Feature;
import io.quartic.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.model.AbstractNakedFeature;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.utils.GeometryTransformer;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import static io.quartic.weyl.core.source.ConversionUtils.convertToModelAttributes;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Value.Immutable
public abstract class GeoJsonSource implements Source {
    public static ImmutableGeoJsonSource.Builder builder() {
        return ImmutableGeoJsonSource.builder();
    }

    private static final Logger LOG = LoggerFactory.getLogger(GeoJsonSource.class);

    protected abstract String name();
    protected abstract String url();
    protected abstract ObjectMapper objectMapper();
    @Value.Default
    protected GeometryTransformer geometryTransformer() {
        return GeometryTransformer.wgs84toWebMercator();
    }

    @Override
    public Observable<SourceUpdate> observable() {
        return Observable.create(sub -> {
            try {
                sub.onNext(SourceUpdate.of(importAllFeatures(), emptyList()));
                sub.onCompleted();
            } catch (IOException e) {
                sub.onError(e);
            }
        });
    }

    @Override
    public boolean indexable() {
        return true;
    }

    private Collection<AbstractNakedFeature> importAllFeatures() throws IOException {
        final FeatureCollection featureCollection = objectMapper().readValue(parseURL(url()), FeatureCollection.class);

        return featureCollection.features().stream().map(this::toJts)
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))
                .collect(toList());
    }

    private URL parseURL(String url) throws MalformedURLException {
        return new URL(url);
    }

    private Optional<NakedFeature> toJts(Feature f) {
        // TODO: We are ignoring null geometries here (as well as in the live pipeline). We should figure out something better.
        return f.geometry().map(rawGeometry -> {
            Geometry transformedGeometry = geometryTransformer().transform(Utils.toJts(rawGeometry));
            return NakedFeature.of(
                    f.id().orElse(null),
                    transformedGeometry,
                    convertToModelAttributes(objectMapper(), f.properties())
            );
        });
    }




}
