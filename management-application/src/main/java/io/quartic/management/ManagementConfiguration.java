package io.quartic.management;

import io.dropwizard.Configuration;

public class ManagementConfiguration extends Configuration {
    private String catalogueUrl;
    private String bucketName;
    private String howlUrl;

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
}
