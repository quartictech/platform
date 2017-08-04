package io.quartic.bild

import com.nhaarman.mockito_kotlin.mock
import io.quartic.bild.model.CustomerId
import io.quartic.bild.resource.DagResource
import org.hamcrest.Matchers
import org.junit.Assert.assertThat
import org.junit.Test


class DagResourceShould {
    private val dag = mapOf("noob" to "yes")
    private val jobResults = mock<JobResultStore>()
    private val resource = DagResource(jobResults, dag)

    @Test
    fun fetch_dag_for_customer() {
        val dag = resource.dag(CustomerId("111"))
        assertThat(dag, Matchers.equalTo(dag))
    }
}
