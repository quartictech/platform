package io.quartic.catalogue.api

class PostgresDatasetLocatorShould : DatasetLocatorTests<PostgresDatasetLocator>() {
    override fun locator(): PostgresDatasetLocator = PostgresDatasetLocatorImpl.of("alice", "pass", "foo", "q")

    override fun json() = "{\"type\": \"postgres\", \"user\": \"alice\", \"password\": \"pass\", \"url\": \"foo\", \"query\": \"q\"}"
}
