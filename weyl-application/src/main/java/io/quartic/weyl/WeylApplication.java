package io.quartic.weyl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.websockets.WebsocketBundle;
import io.quartic.catalogue.api.*;
import io.quartic.common.application.ApplicationBase;
import io.quartic.common.client.WebsocketClientSessionFactory;
import io.quartic.common.client.WebsocketListener;
import io.quartic.common.pingpong.PingPongResource;
import io.quartic.common.uid.RandomUidGenerator;
import io.quartic.common.uid.UidGenerator;
import io.quartic.weyl.attributes.AttributesUpdateGenerator;
import io.quartic.weyl.catalogue.CatalogueWatcher;
import io.quartic.weyl.chart.ChartUpdateGenerator;
import io.quartic.weyl.core.EntityStore;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.alert.AlertProcessor;
import io.quartic.weyl.core.compute.HistogramCalculator;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.live.LiveEventConverter;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.source.*;
import io.quartic.weyl.core.utils.GeometryTransformer;
import io.quartic.weyl.histogram.HistogramUpdateGenerator;
import io.quartic.weyl.resource.*;
import rx.schedulers.Schedulers;

import javax.websocket.server.ServerEndpointConfig;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;

public class WeylApplication extends ApplicationBase<WeylConfiguration> {
    private final GeometryTransformer transformFromFrontend = GeometryTransformer.webMercatortoWgs84();
    private final GeometryTransformer transformToFrontend = GeometryTransformer.wgs84toWebMercator();
    private final UidGenerator<LayerId> lidGenerator = RandomUidGenerator.of(LayerId::of);   // Use a random generator to ensure MapBox tile caching doesn't break things

    private final EntityStore entityStore = new EntityStore();
    private final LayerStore layerStore = new LayerStore(entityStore, lidGenerator);
    private final GeofenceStore geofenceStore = new GeofenceStore(layerStore);
    private final AlertProcessor alertProcessor = new AlertProcessor(geofenceStore);

    public static void main(String[] args) throws Exception {
        new WeylApplication().run(args);
    }

    @Override
    public void initializeApplication(Bootstrap<WeylConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html"));
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
                                Multiplexer.create(entityStore::getObservable),
                                newArrayList(
                                        new ChartUpdateGenerator(),
                                        new HistogramUpdateGenerator(new HistogramCalculator()),
                                        new AttributesUpdateGenerator()
                                ),
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
    public void runApplication(WeylConfiguration configuration, Environment environment) throws Exception {
        environment.jersey().register(new JsonProcessingExceptionMapper(true)); // So we get Jackson deserialization errors in the response
        environment.jersey().setUrlPattern("/api/*");

        environment.jersey().register(new PingPongResource());
        environment.jersey().register(new LayerResource(layerStore));
        environment.jersey().register(new TileResource(layerStore));
        environment.jersey().register(new GeofenceResource(transformToFrontend, geofenceStore, layerStore));
        environment.jersey().register(new AlertResource(alertProcessor));

        final WebsocketClientSessionFactory websocketFactory = new WebsocketClientSessionFactory(getClass());

        final CatalogueWatcher catalogueWatcher = CatalogueWatcher.builder()
                .listenerFactory(WebsocketListener.Factory.of(configuration.getCatalogueWatchUrl(), websocketFactory))
                .sourceFactories(createSourceFactories(configuration, environment, websocketFactory))
                .layerStore(layerStore)
                .scheduler(Schedulers.from(Executors.newScheduledThreadPool(2)))
                .build();

        catalogueWatcher.start();
    }

    private Map<Class<? extends DatasetLocator>, Function<DatasetConfig, Source>> createSourceFactories(
            WeylConfiguration configuration,
            Environment environment,
            WebsocketClientSessionFactory websocketFactory) {
        final LiveEventConverter converter = new LiveEventConverter();

        final TerminatorSourceFactory terminatorSourceFactory = TerminatorSourceFactory.builder()
                .listenerFactory(WebsocketListener.Factory.of(configuration.getTerminatorUrl(), websocketFactory))
                .converter(converter)
                .metrics(environment.metrics())
                .build();

        return ImmutableMap.of(
                PostgresDatasetLocator.class, config -> PostgresSource.builder()
                        .name(config.metadata().name())
                        .locator((PostgresDatasetLocator) config.locator())
                        .objectMapper(environment.getObjectMapper())
                        .build(),
                GeoJsonDatasetLocator.class, config -> GeoJsonSource.builder()
                        .name(config.metadata().name())
                        .url(((GeoJsonDatasetLocator) config.locator()).url())
                        .objectMapper(environment.getObjectMapper())
                        .build(),
                WebsocketDatasetLocator.class, config -> WebsocketSource.builder()
                        .name(config.metadata().name())
                        .listenerFactory(WebsocketListener.Factory.of(((WebsocketDatasetLocator) config.locator()).url(), websocketFactory))
                        .converter(converter)
                        .metrics(environment.metrics())
                        .build(),
                TerminatorDatasetLocator.class, config -> terminatorSourceFactory.sourceFor((TerminatorDatasetLocator) config.locator()),
                CloudGeoJsonDatasetLocator.class, config -> GeoJsonSource.builder()
                        .name(config.metadata().name())
                        .url(configuration.getCloudStorageUrl() + ((CloudGeoJsonDatasetLocator) config.locator()).path())
                        .objectMapper(environment.getObjectMapper())
                        .build()
        );
    }
}
