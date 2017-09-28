package io.quartic.eval

import com.nhaarman.mockito_kotlin.mock
import io.quartic.common.test.assertThrows
import io.quartic.eval.database.model.LegacyPhaseCompleted.V1.Dataset
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Node
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Node.Raw
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Node.Step
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.Assert.assertThat
import org.junit.Test

class DagShould {
    @Test
    fun detect_cyclic_dag() {
        val a = Step("111", mock(),
            listOf(dataset("B")),
            dataset("A")
        ) as Node
        val b = Step("222", mock(),
            listOf(dataset("A")),
            dataset("B")
        ) as Node
        val nodes = listOf(a, b)

        assertThrows<IllegalArgumentException> {
            Dag.fromRaw(nodes)
        }
    }

    @Test
    fun detect_multiple_steps_per_output() {
        val a = Raw("111", mock(), mock(),
            dataset("A")
        ) as Node
        val b1 = Step("222", mock(),
            listOf(dataset("A")),
            dataset("B")
        ) as Node
        val b2 = Step("333", mock(),
            listOf(dataset("A")),
            dataset("B")
        ) as Node
        val nodes = listOf(a, b1, b2)

        assertThrows<IllegalArgumentException> {
            Dag.fromRaw(nodes)
        }
    }

    @Test
    fun detect_unproduced_datasets() {
        val a = Raw("111", mock(), mock(),
            dataset("C")
        ) as Node
        val b1 = Step("222", mock(),
            listOf(dataset("A")),
            dataset("B")
        ) as Node

        val nodes = listOf(a, b1)

        assertThrows<IllegalArgumentException> {
            Dag.fromRaw(nodes)
        }
    }


    @Test
    fun generate_correct_edges() {
        val a = Raw("111", mock(), mock(),
            dataset("A")
        ) as Node
        val b = Raw("222", mock(), mock(),
            dataset("B")
        ) as Node
        val c = Raw("333", mock(), mock(),
            dataset("C")
        ) as Node
        val d = Step("444", mock(),
            listOf(dataset("A"), dataset("B")),
            dataset("D")
        ) as Node
        val e = Step("555", mock(),
            listOf(dataset("B"), dataset("C"), dataset("D")),
            dataset("E")
        ) as Node
        val nodes = listOf(a, b, c, d, e)

        assertThat(Dag.fromRaw(nodes).edges, containsInAnyOrder(
            a to d,
            b to d,
            b to e,
            c to e,
            d to e
        ))
    }

    private fun dataset(id: String) = Dataset(null, id)
}
