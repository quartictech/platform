package io.quartic.weyl.core.source;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;
import io.quartic.geojson.*;
import io.quartic.weyl.core.model.NakedFeatureImpl;
import io.quartic.weyl.core.utils.GeometryTransformer;
import org.junit.Rule;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.Options.DYNAMIC_PORT;
import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static io.quartic.weyl.core.geojson.Utils.toJts;
import static io.quartic.weyl.core.model.Attributes.EMPTY_ATTRIBUTES;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class GeoJsonSourceShould {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(DYNAMIC_PORT);

    @Test
    public void import_things() throws Exception {
        final Geometry geometry = PointImpl.of(newArrayList(1.0, 2.0));
        final FeatureCollection original = FeatureCollectionImpl.of(newArrayList(
                FeatureImpl.of(Optional.of("abc"), Optional.of(geometry), ImmutableMap.of())
        ));

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
                .objectMapper(OBJECT_MAPPER)
                .geometryTransformer(GeometryTransformer.webMercatorToWebMercator())
                .build()
                .observable().subscribe(subscriber);

        subscriber.assertValue(SourceUpdateImpl.of(
                newArrayList(NakedFeatureImpl.of("abc", toJts(geometry), EMPTY_ATTRIBUTES))
        ));
    }
}
