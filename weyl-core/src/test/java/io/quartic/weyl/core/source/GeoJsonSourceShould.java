package io.quartic.weyl.core.source;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;
import io.quartic.geojson.*;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.model.NakedFeature;
import org.junit.Rule;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Collection;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.Options.DYNAMIC_PORT;
import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

public class GeoJsonSourceShould {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(DYNAMIC_PORT);

    @Test
    public void import_things() throws Exception {
        final FeatureCollection original = FeatureCollectionImpl.of(newArrayList(
                FeatureImpl.of(
                    Optional.of("abc"),
                    Optional.of((Geometry) PointImpl.of(newArrayList(1.0, 2.0))),
                    ImmutableMap.of())
        ));
        final Collection<NakedFeature> modelFeatures = mock(Collection.class);
        final FeatureConverter converter = mock(FeatureConverter.class);
        when(converter.toModel(any(Collection.class))).thenReturn(modelFeatures);

        stubFor(WireMock.get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(OBJECT_MAPPER.writeValueAsString(original))
                )
        );

        TestSubscriber<SourceUpdate> subscriber = TestSubscriber.create();

        GeoJsonSource.builder()
                .name("Budgie")
                .url("http://localhost:" + wireMockRule.port())
                .converter(converter)
                .build()
                .observable().subscribe(subscriber);

        verify(converter).toModel(original.features());
        subscriber.assertValue(SourceUpdateImpl.of(modelFeatures));
    }
}
