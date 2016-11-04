package io.quartic.weyl;

import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class WeylConfiguration extends Configuration {
    @Valid
    @NotNull
    private String catalogueUrl;

    @Valid
    @NotNull
    private String cloudStorageUrl;

    public String getCatalogueUrl() {
        return catalogueUrl;
    }

    public void setCatalogueUrl(String catalogueUrl) {
        this.catalogueUrl = catalogueUrl;
    }

    public String getCloudStorageUrl() {
        return cloudStorageUrl;
    }

    public void setCloudStorageUrl(String cloudStorageUrl) {
        this.cloudStorageUrl = cloudStorageUrl;
    }

}
