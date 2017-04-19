package io.quartic.catalogue.api.model

import io.quartic.catalogue.api.model.PostgresDatasetLocator

class PostgresDatasetLocatorShould : DatasetLocatorTests<PostgresDatasetLocator>() {
    override fun locator() = PostgresDatasetLocator("alice", "pass", "foo", "q")

    override fun json() = "{\"type\": \"postgres\", \"user\": \"alice\", \"password\": \"pass\", \"url\": \"foo\", \"query\": \"q\"}"
}
