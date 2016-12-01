package io.quartic.weyl;

import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class WeylConfiguration extends Configuration {
    @Valid
    @NotNull
    private String catalogueWatchUrl;

    @Valid
    @NotNull
    private String terminatorUrl;

    @Valid
    @NotNull
    private String howlStorageUrl;

    public String getCatalogueWatchUrl() {
        return catalogueWatchUrl;
    }

    public void setCatalogueWatchUrl(String catalogueWatchUrl) {
        this.catalogueWatchUrl = catalogueWatchUrl;
    }

    public String getTerminatorUrl() {
        return terminatorUrl;
    }

    public void setTerminatorUrl(String terminatorUrl) {
        this.terminatorUrl = terminatorUrl;
    }

    public String getHowlStorageUrl() {
        return howlStorageUrl;
    }

    public void setHowlStorageUrl(String howlStorageUrl) {
        this.howlStorageUrl = howlStorageUrl;
    }
}
