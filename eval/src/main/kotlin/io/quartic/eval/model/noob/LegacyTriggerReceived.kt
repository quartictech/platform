package io.quartic.eval.model.noob

class LegacyTriggerReceived private constructor() {

    // Add a field and associated type
    data class V2(
        val foo: Foo,
        val bar: V1.Bar
    ) {
        data class Foo(val x: Int)
    }

    // Original version
    data class V1(val bar: Bar) {
        data class Bar(val y: Int)
    }
}

