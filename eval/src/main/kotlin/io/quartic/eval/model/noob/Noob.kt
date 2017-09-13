package io.quartic.eval.model.noob

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import io.quartic.eval.model.noob.LegacyEventA.V2
import io.quartic.eval.model.noob.Noob.EventA
import io.quartic.eval.model.noob.Noob.EventB


// Latest model

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonSubTypes(
    Type(EventA::class, name = "event_a"),
    Type(EventB::class, name = "event_b")
)
sealed class Noob {

    // Change the definition of a field type
    data class EventA(
        val foo: V2.Foo,
        val bar: Bar
    ) : Noob() {
        data class Bar(val z: String)
    }

    data class EventB(val alice: Alice, val bob: Bob) : Noob() {
        data class Alice(val u: Int)
        data class Bob(val v: Int)
    }
}
