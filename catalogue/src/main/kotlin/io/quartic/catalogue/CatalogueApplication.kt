package io.quartic.catalogue

import io.dropwizard.setup.Environment
import io.quartic.catalogue.database.Database
import io.quartic.common.application.ApplicationBase
import io.quartic.common.db.DatabaseBuilder

class CatalogueApplication : ApplicationBase<CatalogueConfiguration>() {
    public override fun runApplication(configuration: CatalogueConfiguration, environment: Environment) {
        val database = database(configuration, environment)
        val catalogue = CatalogueResource(database)
        environment.jersey().register(catalogue)
    }

    private fun database(config: CatalogueConfiguration, environment: Environment) =
        DatabaseBuilder(
            javaClass,
            config.database,
            environment,
            config.secretsCodec
        ).dao<Database>()

    companion object {
        @JvmStatic fun main(args: Array<String>) = CatalogueApplication().run(*args)
    }
}
