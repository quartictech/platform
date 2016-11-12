package io.quartic.terminator;

import io.dropwizard.Configuration;

public class TerminatorConfiguration extends Configuration {
    private String catalogueWatchUrl;

    public String getCatalogueWatchUrl() {
        return catalogueWatchUrl;
    }

    public void setCatalogueWatchUrl(String catalogueWatchUrl) {
        this.catalogueWatchUrl = catalogueWatchUrl;
    }
}
