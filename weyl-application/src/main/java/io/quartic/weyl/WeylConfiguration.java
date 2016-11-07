package io.quartic.weyl;

import io.dropwizard.Configuration;

public class WeylConfiguration extends Configuration {
    private String catalogueUrl;
    private String terminatorUrl;

    public String getCatalogueUrl() {
        return catalogueUrl;
    }

    public void setCatalogueUrl(String catalogueUrl) {
        this.catalogueUrl = catalogueUrl;
    }

    public String getTerminatorUrl() {
        return terminatorUrl;
    }

    public void setTerminatorUrl(String terminatorUrl) {
        this.terminatorUrl = terminatorUrl;
    }
}
