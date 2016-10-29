package io.quartic.weyl;

import io.dropwizard.Configuration;

public class WeylConfiguration extends Configuration {
    private String jesterUrl;

    public String getJesterUrl() {
        return jesterUrl;
    }

    public void setJesterUrl(String jesterUrl) {
        this.jesterUrl = jesterUrl;
    }
}
