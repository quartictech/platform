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
    private String cloudStorageUrl;

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

    public String getCloudStorageUrl() {
        return cloudStorageUrl;
    }

    public void setCloudStorageUrl(String cloudStorageUrl) {
        this.cloudStorageUrl = cloudStorageUrl;
    }
}
