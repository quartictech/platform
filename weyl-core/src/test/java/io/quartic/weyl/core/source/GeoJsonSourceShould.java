package io.quartic.weyl.core.source;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.quartic.common.geojson.Feature;
import io.quartic.common.geojson.FeatureCollection;
import io.quartic.common.geojson.Point;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.LayerUpdateImpl;
import io.quartic.weyl.core.model.NakedFeature;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Collection;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.Options.DYNAMIC_PORT;
import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.serdes.ObjectMappersKt.objectMapper;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GeoJsonSourceShould {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(DYNAMIC_PORT);

    private final FeatureCollection original = new FeatureCollection(newArrayList(new Feature("abc", new Point(newArrayList(1.0, 2.0)))));
    private final Collection<NakedFeature> modelFeatures = mock(Collection.class);
    private final FeatureConverter converter = mock(FeatureConverter.class);


    @Before
    public void before() throws Exception {
        when(converter.toModel(any(FeatureCollection.class))).thenReturn(modelFeatures);

        stubFor(WireMock.get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(objectMapper().writeValueAsString(original))
                )
        );
    }

    @Test
    public void import_things() throws Exception {
        TestSubscriber<LayerUpdate> subscriber = TestSubscriber.create();

        GeoJsonSource.builder()
                .name("Budgie")
                .url("http://localhost:" + wireMockRule.port())
                .userAgent("MyUserAgent")
                .converter(converter)
                .build()
                .observable().subscribe(subscriber);

        verify(converter).toModel(original);
        subscriber.assertValue(LayerUpdateImpl.of(modelFeatures));
    }

    @Test
    public void set_the_correct_user_agent_header() throws Exception {
        GeoJsonSource.builder()
                .name("Budgie")
                .url("http://localhost:" + wireMockRule.port())
                .userAgent("MyUserAgent")
                .converter(converter)
                .build()
                .observable().subscribe();

        final LoggedRequest request = findAll(getRequestedFor(urlMatching("/.*"))).get(0);

        assertThat(request.header("User-Agent").firstValue(), equalTo("MyUserAgent"));
    }
}
