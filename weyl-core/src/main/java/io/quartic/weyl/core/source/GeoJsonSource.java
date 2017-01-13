package io.quartic.weyl.core.source;

import com.google.common.net.HttpHeaders;
import io.quartic.common.geojson.FeatureCollection;
import io.quartic.common.geojson.GeoJsonParser;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.LayerUpdateImpl;
import io.quartic.weyl.core.model.NakedFeature;
import org.immutables.value.Value;
import rx.Observable;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;

import static com.google.common.collect.Lists.newArrayList;

@Value.Immutable
public abstract class GeoJsonSource implements Source {
    public static ImmutableGeoJsonSource.Builder builder() {
        return ImmutableGeoJsonSource.builder();
    }

    protected abstract String name();
    protected abstract String url();
    protected abstract String userAgent();
    protected abstract FeatureConverter converter();

    @Override
    public Observable<LayerUpdate> observable() {
        return Observable.create(sub -> {
            try {
                sub.onNext(LayerUpdateImpl.of(importAllFeatures()));
            } catch (IOException e) {
                sub.onError(e);
            }
        });
    }

    @Override
    public boolean indexable() {
        return true;
    }

    private Collection<NakedFeature> importAllFeatures() throws IOException {
        final URLConnection conn = parseURL(url()).openConnection();
        conn.setRequestProperty(HttpHeaders.USER_AGENT, userAgent());
        try (final InputStream inputStream = conn.getInputStream()) {
            FeatureCollection featureCollection = new FeatureCollection(newArrayList(new GeoJsonParser(inputStream)));
            return converter().toModel(featureCollection);
        }
    }

    private URL parseURL(String url) throws MalformedURLException {
        return new URL(url);
    }
}
