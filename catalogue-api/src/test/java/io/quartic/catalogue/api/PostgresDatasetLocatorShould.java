package io.quartic.catalogue.api;

public class PostgresDatasetLocatorShould extends DatasetLocatorTests<PostgresDatasetLocator> {
    @Override
    protected PostgresDatasetLocator locator() {
        return PostgresDatasetLocatorImpl.of("alice", "pass", "foo", "q");
    }

    @Override
    protected String json() {
        return "{\"type\": \"postgres\", \"user\": \"alice\", \"password\": \"pass\", \"url\": \"foo\", \"query\": \"q\"}";
    }
}
