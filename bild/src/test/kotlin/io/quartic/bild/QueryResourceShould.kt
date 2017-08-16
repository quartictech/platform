package io.quartic.bild

import com.nhaarman.mockito_kotlin.mock
import io.quartic.bild.resource.QueryResource
import io.quartic.bild.store.JobStore
import io.quartic.common.model.CustomerId
import org.hamcrest.Matchers
import org.junit.Assert.assertThat
import org.junit.Test


class QueryResourceShould {
    private val dag = mapOf("noob" to "yes")
    private val jobResults = mock<JobStore>()
    private val resource = QueryResource(jobResults, dag)

    @Test
    fun fetch_dag_for_customer() {
        val dag = resource.dag(CustomerId("111"))
        assertThat(dag, Matchers.equalTo(dag))
    }
}
