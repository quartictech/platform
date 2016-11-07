package io.quartic.terminator;

import io.dropwizard.Configuration;

public class TerminatorConfiguration extends Configuration {
    private String catalogueUrl;

    public String getCatalogueUrl() {
        return catalogueUrl;
    }

    public void setCatalogueUrl(String catalogueUrl) {
        this.catalogueUrl = catalogueUrl;
    }
}
