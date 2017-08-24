package io.quartic.eval.apis

import io.quartic.quarty.model.Step


interface Database {
    sealed class BuildResult {
        data class Success(val dag: List<Step>) : BuildResult()
        data class InternalError(val throwable: Throwable) : BuildResult()
        data class UserError(val message: String) : BuildResult()
    }

    fun writeResult(result: BuildResult)
}
