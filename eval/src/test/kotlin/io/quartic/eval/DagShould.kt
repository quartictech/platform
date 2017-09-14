package io.quartic.eval

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.test.assertThrows
import io.quartic.eval.Dag.Node
import io.quartic.quarty.api.model.Dataset
import io.quartic.quarty.api.model.Step
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.Assert.assertThat
import org.junit.Test

class DagShould {
    fun readSteps(file: String) =
        OBJECT_MAPPER.readValue<List<Step>>(javaClass.getResourceAsStream(file))

    @Test
    fun build_basic_dag() {
        Dag.fromSteps(readSteps("/steps.json"))
        // No errors
    }

    @Test
    fun detect_cyclic_dag() {
        assertThrows<IllegalArgumentException> {
            Dag.fromSteps(readSteps("/steps_cyclic.json"))
        }
    }

    @Test
    fun detect_multiple_steps_per_output() {
        assertThrows<IllegalArgumentException> {
            Dag.fromSteps(readSteps("/steps_duplicate_outputs.json"))
        }
    }

    @Test
    fun generate_correct_edges() {
        val stepX = Step(
            id = "123",
            name = "foo",
            description = "whatever",
            file = "whatever",
            lineRange = emptyList(),
            inputs = listOf(dataset("A"), dataset("B")),
            outputs = listOf(dataset("D"))
        )

        val stepY = Step(
            id = "456",
            name = "bar",
            description = "whatever",
            file = "whatever",
            lineRange = emptyList(),
            inputs = listOf(dataset("B"), dataset("C"), dataset("D")),
            outputs = listOf(dataset("E"))
        )


        val steps = listOf(stepX, stepY)

        assertThat(Dag.fromSteps(steps).edges, containsInAnyOrder(
            Node(dataset("A")) to Node(dataset("D"), stepX),
            Node(dataset("B")) to Node(dataset("D"), stepX),
            Node(dataset("B")) to Node(dataset("E"), stepY),
            Node(dataset("C")) to Node(dataset("E"), stepY),
            Node(dataset("D"), stepX) to Node(dataset("E"), stepY)
        ))
    }

    private fun dataset(id: String) = Dataset(null, id)
}
