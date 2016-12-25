package io.quartic.weyl.core.source;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.quartic.common.geojson.Feature;
import io.quartic.common.geojson.FeatureCollection;
import io.quartic.common.geojson.Point;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.LayerUpdateImpl;
import io.quartic.weyl.core.model.NakedFeature;
import org.junit.Rule;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Collection;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.Options.DYNAMIC_PORT;
import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GeoJsonSourceShould {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(DYNAMIC_PORT);

    @Test
    public void import_things() throws Exception {
        final FeatureCollection original = new FeatureCollection(newArrayList(new Feature("abc", new Point(newArrayList(1.0, 2.0)))));
        final Collection<NakedFeature> modelFeatures = mock(Collection.class);
        final FeatureConverter converter = mock(FeatureConverter.class);
        when(converter.toModel(any(FeatureCollection.class))).thenReturn(modelFeatures);

        stubFor(WireMock.get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(INSTANCE.getOBJECT_MAPPER().writeValueAsString(original))
                )
        );

        TestSubscriber<LayerUpdate> subscriber = TestSubscriber.create();

        GeoJsonSource.builder()
                .name("Budgie")
                .url("http://localhost:" + wireMockRule.port())
                .converter(converter)
                .build()
                .observable().subscribe(subscriber);

        verify(converter).toModel(original);
        subscriber.assertValue(LayerUpdateImpl.of(modelFeatures));
    }
}
