package io.quartic.weyl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GeoQueryConfig {
    private final String sql;

    @JsonCreator
    public GeoQueryConfig(@JsonProperty("sql") String sql) {
       this.sql = sql;
    }

    public String getSql() {
        return sql;
    }
}
