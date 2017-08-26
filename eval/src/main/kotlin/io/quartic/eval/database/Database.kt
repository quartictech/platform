package io.quartic.eval.database

import io.quartic.common.model.CustomerId
import io.quartic.quarty.model.Step


interface Database {
    sealed class BuildResult {
        data class Success(val dag: List<Step>) : BuildResult()
        data class InternalError(val throwable: Throwable) : BuildResult()
        data class UserError(val message: String) : BuildResult()
    }

    fun writeResult(customerId: CustomerId, result: BuildResult)

    fun getLatestDag(customerId: CustomerId): List<Step>?
}
