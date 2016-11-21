package io.quartic.common.application;

import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.quartic.common.ApplicationDetails;
import io.quartic.common.ApplicationDetailsImpl;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.slf4j.LoggerFactory.getLogger;

public abstract class ApplicationBase<T extends Configuration> extends Application<T> {
    private static final Logger LOG = getLogger(ApplicationBase.class);

    private final ApplicationDetails details = ApplicationDetailsImpl.of(getClass());

    @Override
    public final void initialize(Bootstrap<T> bootstrap) {
        bootstrap.setConfigurationSourceProvider((path) -> new SequenceInputStream(
                new FileInputStream(path),
                toInputStream("\n" + getBaseConfig(), StandardCharsets.UTF_8)
        ));

        bootstrap.addBundle(new Java8Bundle());
        bootstrap.addBundle(new TemplateConfigBundle());
        initializeApplication(bootstrap);
    }

    @Override
    public final void run(T configuration, Environment environment) throws Exception {
        LOG.info("Running " + details.name() + " " + details.version() + " (Java " + details.javaVersion() + ")");
        runApplication(configuration, environment);
    }

    protected void initializeApplication(Bootstrap<T> bootstrap) {}
    protected abstract void runApplication(T configuration, Environment environment) throws Exception;

    private String getBaseConfig() {
        try {
            // TODO: this string substitution is gross, should come up with something better
            return IOUtils.toString(getClass().getResourceAsStream("/application.yml"), StandardCharsets.UTF_8)
                    .replaceAll("\\$\\{APPLICATION_NAME\\}", details.name().toLowerCase());
        } catch (IOException e) {
            throw new RuntimeException("Couldn't read base config", e);
        }
    }
}
