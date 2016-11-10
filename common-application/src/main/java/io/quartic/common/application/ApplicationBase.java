package io.quartic.common.application;

import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundleConfiguration;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.setup.Bootstrap;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;

import static org.apache.commons.io.IOUtils.toInputStream;

public abstract class ApplicationBase<T extends Configuration> extends Application<T> {
    private final String name;

    protected ApplicationBase(String name) {
        this.name = name;
    }

    @Override
    public void initialize(Bootstrap<T> bootstrap) {
        bootstrap.setConfigurationSourceProvider((path) -> new SequenceInputStream(
                new FileInputStream(path),
                toInputStream("\n" + getBaseConfig(), StandardCharsets.UTF_8)
        ));

        bootstrap.addBundle(new Java8Bundle());
        bootstrap.addBundle(new TemplateConfigBundle(new TemplateConfigBundleConfiguration()
        ));
    }

    private String getBaseConfig() {
        try {
            // TODO: this string substitution is gross, should come up with something better
            return IOUtils.toString(getClass().getResourceAsStream("/application.yml"), StandardCharsets.UTF_8)
                    .replaceAll("\\$\\{APPLICATION_NAME\\}", name);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't read base config", e);
        }
    }
}
