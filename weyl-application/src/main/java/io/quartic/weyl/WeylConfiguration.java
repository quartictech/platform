package io.quartic.weyl;

import io.dropwizard.Configuration;

public class WeylConfiguration extends Configuration {
    private String catalogueUrl;

    public String getCatalogueUrl() {
        return catalogueUrl;
    }

    public void setCatalogueUrl(String catalogueUrl) {
        this.catalogueUrl = catalogueUrl;
    }
}
