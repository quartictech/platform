package io.quartic.weyl;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Map;

public class WeylConfiguration extends Configuration {
    @Valid
    @NotNull
    @JsonProperty
    private DataSourceFactory database = new DataSourceFactory();

    @Valid
    @NotNull
    @JsonProperty
    private Map<String, GeoQueryConfig> queries;

    public DataSourceFactory getDataSourceFactory() {
        return database;
    }

    public Map<String, GeoQueryConfig> getQueries() {
        return queries;
    }
}
