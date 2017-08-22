package io.quartic.eval.apis

import io.quartic.qube.api.model.Dag

interface Database {
    sealed class BuildResult {
        data class Success(val dag: Dag) : BuildResult()
        data class InternalError(val throwable: Throwable) : BuildResult()
        data class UserError(val message: String) : BuildResult()
    }

    fun writeResult(result: BuildResult)
}
