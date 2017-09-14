package io.quartic.catalogue.api.model

class CloudDatasetLocatorShould : DatasetLocatorTests<DatasetLocator.CloudDatasetLocator>() {
    override fun locator() = DatasetLocator.CloudDatasetLocator("http://wat", false, "application/noob")

    override fun json() = """
        {
            "type": "cloud",
            "path": "http://wat",
            "mime_type": "application/noob"
        }
    """
}
