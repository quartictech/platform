package io.quartic.management;

import io.dropwizard.Configuration;

public class ManagementConfiguration extends Configuration {
    private String catalogueUrl;
    private String bucketName;

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
}
