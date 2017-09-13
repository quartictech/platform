package io.quartic.eval.model.noob

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import io.quartic.eval.model.noob.LegacyTriggerReceived.V2


@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonSubTypes(
    Type(TriggerReceived::class, name = "trigger_received_a"),
    Type(PhaseCompleted::class, name = "phase_completed_b")
)
sealed class Event {

    // Change the definition of a field type
    data class CurrentTriggerReceived(
        val foo: V2.Foo,
        val bar: Bar
    ) : Event() {
        data class Bar(val z: String)
    }

    data class CurrentPhaseCompleted(val alice: Alice, val bob: Bob) : Event() {
        data class Alice(val u: Int)
        data class Bob(val v: Int)
    }
}

typealias TriggerReceived = Event.CurrentTriggerReceived
typealias PhaseCompleted = Event.CurrentPhaseCompleted
