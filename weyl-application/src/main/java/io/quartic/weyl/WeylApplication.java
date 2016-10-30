package io.quartic.weyl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.websockets.WebsocketBundle;
import io.quartic.common.client.ClientBuilder;
import io.quartic.common.pingpong.PingPongResource;
import io.quartic.jester.api.DatasetSource;
import io.quartic.jester.api.GeoJsonDatasetSource;
import io.quartic.jester.api.JesterService;
import io.quartic.jester.api.PostgresDatasetSource;
import io.quartic.weyl.common.uid.RandomUidGenerator;
import io.quartic.weyl.common.uid.SequenceUidGenerator;
import io.quartic.weyl.common.uid.UidGenerator;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.alert.AlertProcessor;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.source.GeoJsonSource;
import io.quartic.weyl.core.source.PostgresSource;
import io.quartic.weyl.core.source.Source;
import io.quartic.weyl.core.live.LiveEventId;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.utils.GeometryTransformer;
import io.quartic.weyl.resource.*;

import javax.websocket.server.ServerEndpointConfig;
import java.util.Map;
import java.util.function.Function;

public class WeylApplication extends Application<WeylConfiguration> {
    private UpdateServer updateServer = null;   // TODO: deal with weird mutability
    private final GeometryTransformer transformFromFrontend = GeometryTransformer.webMercatortoWgs84();
    private final GeometryTransformer transformToFrontend = GeometryTransformer.wgs84toWebMercator();

    public static void main(String[] args) throws Exception {
        new WeylApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<WeylConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html"));
        bootstrap.addBundle(new Java8Bundle());
        bootstrap.addBundle(configureWebsockets(bootstrap.getObjectMapper()));
    }

    private WebsocketBundle configureWebsockets(ObjectMapper objectMapper) {
        updateServer = new UpdateServer(transformFromFrontend, objectMapper);
        final ServerEndpointConfig config = ServerEndpointConfig.Builder
                .create(UpdateServer.class, "/ws")
                .configurator(new ServerEndpointConfig.Configurator() {
                    @Override
                    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                        return (T) updateServer;
                    }
                })
                .build();
        return new WebsocketBundle(config);
    }

    @Override
    public void run(WeylConfiguration configuration, Environment environment) throws Exception {
        environment.jersey().register(new JsonProcessingExceptionMapper(true)); // So we get Jackson deserialization errors in the response
        environment.jersey().setUrlPattern("/api/*");

        final UidGenerator<FeatureId> fidGenerator = SequenceUidGenerator.of(FeatureId::of);
        final UidGenerator<LayerId> lidGenerator = RandomUidGenerator.of(LayerId::of);   // Use a random generator to ensure MapBox tile caching doesn't break things
        final UidGenerator<LiveEventId> eidGenerator = SequenceUidGenerator.of(LiveEventId::of);

        final FeatureStore featureStore = new FeatureStore(fidGenerator);
        final LayerStore layerStore = new LayerStore(featureStore, lidGenerator);
        final GeofenceStore geofenceStore = new GeofenceStore(layerStore, fidGenerator);
        final AlertProcessor alertProcessor = new AlertProcessor(geofenceStore);

        // TODO: deal with weird mutability
        updateServer.setLayerStore(layerStore);
        alertProcessor.addListener(updateServer);
        geofenceStore.addListener(updateServer);

        environment.jersey().register(new PingPongResource());
        environment.jersey().register(new LayerResource(layerStore, fidGenerator, eidGenerator));
        environment.jersey().register(new TileResource(layerStore));
        environment.jersey().register(new GeofenceResource(transformToFrontend, geofenceStore, layerStore));
        environment.jersey().register(new AlertResource(alertProcessor));
        environment.jersey().register(new AggregatesResource(featureStore));
        environment.jersey().register(new AttributesResource(featureStore));

        final JesterService jester = ClientBuilder.build(JesterService.class, configuration.getJesterUrl());

        environment.lifecycle().manage(new Scheduler(ImmutableList.of(
                ScheduleItem.of(2, new JesterManager(jester, layerStore, createImporterFactories(featureStore, environment.getObjectMapper())))
        )));
    }

    private Map<Class<? extends DatasetSource>, Function<DatasetSource, Source>> createImporterFactories(FeatureStore featureStore, ObjectMapper objectMapper) {
        return ImmutableMap.of(
                PostgresDatasetSource.class, source -> PostgresSource.create((PostgresDatasetSource)source, featureStore, objectMapper),
                GeoJsonDatasetSource.class, source -> GeoJsonSource.create((GeoJsonDatasetSource)source, featureStore, objectMapper)
        );
    }
}
