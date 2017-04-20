package io.quartic.mgmt;

import io.dropwizard.Configuration;
import io.quartic.catalogue.api.DatasetNamespace;

public class MgmtConfiguration extends Configuration {
    private String catalogueUrl;
    private String bucketName;
    private String howlUrl;
    private DatasetNamespace defaultCatalogueNamespace;

    public String getCatalogueUrl() {
        return catalogueUrl;
    }

    public void setCatalogueUrl(String catalogueUrl) {
        this.catalogueUrl = catalogueUrl;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getHowlUrl() {
        return howlUrl;
    }

    public void setHowlUrl(String howlUrl) {
        this.howlUrl = howlUrl;
    }

    public DatasetNamespace getDefaultCatalogueNamespace() {
        return defaultCatalogueNamespace;
    }

    public void setHowlUrl(DatasetNamespace defaultCatalogueNamespace) {
        this.defaultCatalogueNamespace = defaultCatalogueNamespace;
    }
}
