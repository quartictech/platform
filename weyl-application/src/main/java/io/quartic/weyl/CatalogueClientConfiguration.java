package io.quartic.weyl;

public class CatalogueClientConfiguration {
    private String hostName;
    private Integer port;
    private Boolean useSsl;

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setUseSsl(Boolean useSsl) {
        this.useSsl = useSsl;
    }

    public String getRestUrl() {
        return String.format("%s://%s:%d/api/", useSsl ? "https" : "http", hostName, port);
    }

    public String getWatchUrl() {
        return String.format("%s://%s:%d/api/datasets/watch", useSsl ? "wss" : "ws", hostName, port);
    }
}
