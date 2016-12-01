package io.quartic.howl;

import io.dropwizard.Configuration;

import java.nio.file.Path;

public class HowlConfiguration extends Configuration {
    private String bucketName;
    private String dataDir;

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }
}
