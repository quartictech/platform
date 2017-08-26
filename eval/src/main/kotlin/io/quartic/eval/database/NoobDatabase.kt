package io.quartic.eval.database

import io.quartic.common.model.CustomerId
import io.quartic.eval.database.Database.BuildResult
import io.quartic.quarty.model.Step

class NoobDatabase : Database {
    private val dags = mutableMapOf<CustomerId, List<Step>>()

    @Synchronized
    override fun writeResult(customerId: CustomerId, result: BuildResult) {
        if (result is BuildResult.Success) {
            dags[customerId] = result.dag
        }
    }

    @Synchronized
    override fun getLatestDag(customerId: CustomerId) = dags[customerId]
}
