package io.quartic.catalogue;

import io.dropwizard.Configuration;

public class ManagementConfiguration extends Configuration {
    private String catalogueUrl;

    public String getCatalogueUrl() {
        return catalogueUrl;
    }

    public void setCatalogueUrl(String catalogueUrl) {
        this.catalogueUrl = catalogueUrl;
    }
}
