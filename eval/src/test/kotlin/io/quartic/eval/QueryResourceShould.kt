package io.quartic.eval

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.quartic.common.model.CustomerId
import io.quartic.common.test.assertThrows
import io.quartic.eval.Database.BuildResultSuccessRow
import io.quartic.eval.api.model.*
import io.quartic.eval.model.BuildResult.Success
import io.quartic.quarty.model.Dataset
import io.quartic.quarty.model.Step
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import javax.ws.rs.NotFoundException

class QueryResourceShould {
    private val database = mock<Database> {
        on { getLatestSuccess(any()) } doReturn null as BuildResultSuccessRow?
    }
    private val resource = QueryResource(database)

    @Test
    fun throw_404_if_not_found() {
        assertThrows<NotFoundException> {
            resource.getDag(CustomerId("999"))
        }
    }

    @Test
    fun convert_dag_to_cytoscape() {
        val steps = listOf(
            Step(
                id = "123",
                name = "foo",
                description = "whatever",
                file = "whatever",
                lineRange = emptyList(),
                inputs = listOf(dataset("A"), dataset("B")),
                outputs = listOf(dataset("D"))
            ),
            Step(
                id = "456",
                name = "bar",
                description = "whatever",
                file = "whatever",
                lineRange = emptyList(),
                inputs = listOf(dataset("B"), dataset("C"), dataset("D")),
                outputs = listOf(dataset("E"))
            )
        )

        whenever(database.getLatestSuccess(CustomerId("999"))).thenReturn(BuildResultSuccessRow(Success(steps)))

        assertThat(resource.getDag(CustomerId("999")), equalTo(
            CytoscapeDag(
                setOf(
                    node("A", "raw"),
                    node("B", "raw"),
                    node("C", "raw"),
                    node("D", "derived"),
                    node("E", "derived")
                ),
                setOf(
                    edge(0, "A", "D"),
                    edge(1, "B", "D"),
                    edge(2, "B", "E"),
                    edge(3, "C", "E"),
                    edge(4, "D", "E")
                )
            )
        ))
    }

    private fun node(id: String, type: String) =
        CytoscapeNode(CytoscapeNodeData("test::${id}", "test::${id}", type))

    private fun edge(id: Long, source: String, target: String) =
        CytoscapeEdge(CytoscapeEdgeData(id, "test::${source}", "test::${target}"))

    private fun dataset(id: String) = Dataset("test", id)
}
