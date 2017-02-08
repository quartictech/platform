package io.quartic.weyl;

import io.dropwizard.Configuration;
import io.quartic.catalogue.CatalogueClientConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class WeylConfiguration extends Configuration {
    @Valid
    @NotNull
    private CatalogueClientConfiguration catalogue;

    @Valid
    @NotNull
    private String howlStorageUrl;

    public CatalogueClientConfiguration getCatalogue() {
        return this.catalogue;
    }

    public void setCatalogue(CatalogueClientConfiguration catalogue) {
        this.catalogue = catalogue;
    }

    public String getHowlStorageUrl() {
        return howlStorageUrl;
    }

    public void setHowlStorageUrl(String howlStorageUrl) {
        this.howlStorageUrl = howlStorageUrl;
    }
}
