package io.quartic.catalogue.api.model

class PostgresDatasetLocatorShould : DatasetLocatorTests<DatasetLocator.PostgresDatasetLocator>() {
    override fun locator() = DatasetLocator.PostgresDatasetLocator("alice", "pass", "foo", "q")

    override fun json() = "{\"type\": \"postgres\", \"user\": \"alice\", \"password\": \"pass\", \"url\": \"foo\", \"query\": \"q\"}"
}
