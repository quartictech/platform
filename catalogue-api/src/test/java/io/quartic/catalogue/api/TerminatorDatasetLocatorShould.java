package io.quartic.catalogue.api;

public class TerminatorDatasetLocatorShould extends DatasetLocatorTests<TerminatorDatasetLocator> {
    @Override
    protected TerminatorDatasetLocator locator() {
        return TerminatorDatasetLocatorImpl.of(TerminationId.fromString("foo"));
    }

    @Override
    protected String json() {
        return "{\"type\": \"terminator\", \"id\": \"foo\"}";
    }
}