package io.quartic.weyl.core.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;
import io.quartic.weyl.common.uid.SequenceUidGenerator;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Geometry;
import io.quartic.weyl.core.geojson.Point;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.ImmutableFeature;
import io.quartic.weyl.core.utils.GeometryTransformer;
import org.junit.Rule;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.net.URL;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.Options.DYNAMIC_PORT;
import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.core.geojson.Utils.toJts;
import static java.util.Collections.emptyList;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GeoJsonImporterShould {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new Jdk8Module());

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(DYNAMIC_PORT);

    @Test
    public void import_things() throws Exception {
        final Geometry geometry = Point.of(newArrayList(1.0, 2.0));
        final FeatureCollection original = FeatureCollection.of(newArrayList(
                Feature.of(Optional.of("abc"), Optional.of(geometry), ImmutableMap.of())
        ));

        stubFor(WireMock.get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(OBJECT_MAPPER.writeValueAsString(original))
                )
        );

        FeatureStore store = mock(FeatureStore.class);
        when(store.getFeatureIdGenerator()).thenReturn(SequenceUidGenerator.of(FeatureId::of));

        TestSubscriber<SourceUpdate> subscriber = TestSubscriber.create();

        new GeoJsonImporter(
                new URL("http://localhost:" + wireMockRule.port()),
                store, GeometryTransformer.webMercatorToWebMercator(), OBJECT_MAPPER)
                .getObservable().subscribe(subscriber);

        subscriber.assertValue(SourceUpdate.of(
                newArrayList(ImmutableFeature.of("abc", FeatureId.of("1"), toJts(geometry), ImmutableMap.of())),
                emptyList()
        ));
    }
}
