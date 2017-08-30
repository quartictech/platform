package io.quartic.eval.model

import io.quartic.eval.api.model.TriggerDetails
import io.quartic.quarty.model.Step
import java.util.*

// TODO - Jackson polymorphism

sealed class BuildEvent {
    data class TriggerReceived(val details: TriggerDetails) : BuildEvent()

    class BuildCancelled : BuildEvent()

    class BuildSucceeded : BuildEvent()

    class BuildFailed : BuildEvent()

    data class ContainerAcquired(val hostname: String) : BuildEvent()

    data class PhaseStarted(val phaseId: UUID, val description: String) : BuildEvent()

    data class PhaseCompleted(val phaseId: UUID, val result: Result) : BuildEvent() {

        sealed class Result {
            data class Success(val artifact: Artifact) : Result() {
                sealed class Artifact {
                    data class EvaluationOutput(val steps: List<Step>) : Artifact()
                }
            }

            data class InternalError(val throwable: Throwable) : Result()

            data class UserError(val detail: Any?) : Result()
        }
    }

    data class LogMessageReceived(val phaseId: UUID, val stream: String, val message: String) : BuildEvent()

    companion object {
        // To make testing easier given one can't have zero-arg data classes
        val BUILD_CANCELLED = BuildCancelled()
        val BUILD_SUCCEEDED = BuildSucceeded()
        val BUILD_FAILED = BuildFailed()
    }
}
