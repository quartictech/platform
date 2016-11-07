package io.quartic.weyl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.websockets.WebsocketBundle;
import io.quartic.catalogue.api.*;
import io.quartic.common.client.ClientBuilder;
import io.quartic.common.healthcheck.PingPongHealthCheck;
import io.quartic.common.pingpong.PingPongResource;
import io.quartic.weyl.common.uid.RandomUidGenerator;
import io.quartic.weyl.common.uid.SequenceUidGenerator;
import io.quartic.weyl.common.uid.UidGenerator;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.alert.AlertProcessor;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.live.LiveEventConverter;
import io.quartic.weyl.core.live.LiveEventId;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.source.GeoJsonSource;
import io.quartic.weyl.core.source.PostgresSource;
import io.quartic.weyl.core.source.Source;
import io.quartic.weyl.core.source.WebsocketSource;
import io.quartic.weyl.core.utils.GeometryTransformer;
import io.quartic.weyl.resource.*;
import io.quartic.weyl.scheduler.ScheduleItem;
import io.quartic.weyl.scheduler.Scheduler;
import rx.schedulers.Schedulers;

import javax.websocket.server.ServerEndpointConfig;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class WeylApplication extends Application<WeylConfiguration> {
    private final GeometryTransformer transformFromFrontend = GeometryTransformer.webMercatortoWgs84();
    private final GeometryTransformer transformToFrontend = GeometryTransformer.wgs84toWebMercator();
    private final UidGenerator<FeatureId> fidGenerator = SequenceUidGenerator.of(FeatureId::of);
    private final UidGenerator<LayerId> lidGenerator = RandomUidGenerator.of(LayerId::of);   // Use a random generator to ensure MapBox tile caching doesn't break things
    private final UidGenerator<LiveEventId> eidGenerator = SequenceUidGenerator.of(LiveEventId::of);

    private final FeatureStore featureStore = new FeatureStore(fidGenerator);
    private final LayerStore layerStore = new LayerStore(featureStore, lidGenerator);
    private final GeofenceStore geofenceStore = new GeofenceStore(layerStore, fidGenerator);
    private final AlertProcessor alertProcessor = new AlertProcessor(geofenceStore);


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
        final ServerEndpointConfig config = ServerEndpointConfig.Builder
                .create(UpdateServer.class, "/ws")
                .configurator(new ServerEndpointConfig.Configurator() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                        return (T) new UpdateServer(layerStore,
                                geofenceStore,
                                alertProcessor,
                                transformFromFrontend,
                                objectMapper);
                    }
                })
                .build();
        return new WebsocketBundle(config);
    }

    @Override
    public void run(WeylConfiguration configuration, Environment environment) throws Exception {
        environment.jersey().register(new JsonProcessingExceptionMapper(true)); // So we get Jackson deserialization errors in the response
        environment.jersey().setUrlPattern("/api/*");

        environment.healthChecks().register("catalogue", new PingPongHealthCheck(configuration.getCatalogueUrl()));

        environment.jersey().register(new PingPongResource());
        environment.jersey().register(new LayerResource(layerStore));
        environment.jersey().register(new TileResource(layerStore));
        environment.jersey().register(new GeofenceResource(transformToFrontend, geofenceStore, layerStore));
        environment.jersey().register(new AlertResource(alertProcessor));
        environment.jersey().register(new AggregatesResource(featureStore));
        environment.jersey().register(new AttributesResource(featureStore));

        final CatalogueService catalogue = ClientBuilder.build(CatalogueService.class, configuration.getCatalogueUrl());

        environment.lifecycle().manage(Scheduler.builder()
                .scheduleItem(ScheduleItem.of(2000, new CatalogueManager(
                        catalogue,
                        layerStore,
                        createSourceFactories(featureStore, environment, configuration),
                        Schedulers.from(Executors.newScheduledThreadPool(2)))))
                .build()
        );
    }

    private Map<Class<? extends DatasetLocator>, Function<DatasetConfig, Source>> createSourceFactories(FeatureStore featureStore, Environment environment, WeylConfiguration configuration) {
        return ImmutableMap.of(
                PostgresDatasetLocator.class, config -> PostgresSource.builder()
                        .name(config.metadata().name())
                        .locator((PostgresDatasetLocator)config.locator())
                        .featureStore(featureStore)
                        .objectMapper(environment.getObjectMapper())
                        .build(),
                GeoJsonDatasetLocator.class, config -> GeoJsonSource.builder()
                        .name(config.metadata().name())
                        .url(((GeoJsonDatasetLocator) config.locator()).url())
                        .featureStore(featureStore)
                        .objectMapper(environment.getObjectMapper())
                        .build(),
                WebsocketDatasetLocator.class, config -> WebsocketSource.builder()
                        .name(config.metadata().name())
                        .locator((WebsocketDatasetLocator)config.locator())
                        .converter(new LiveEventConverter(fidGenerator, eidGenerator))
                        .objectMapper(environment.getObjectMapper())
                        .metrics(environment.metrics())
                        .build(),
                CloudGeoJsonDatasetLocator.class, config -> GeoJsonSource.builder()
                        .name(config.metadata().name())
                        .url(configuration.getCloudStorageUrl() +
                                ((CloudGeoJsonDatasetLocator) config.locator()).path())
                    .featureStore(featureStore)
                    .objectMapper(environment.getObjectMapper())
                    .build()
        );
    }
}
