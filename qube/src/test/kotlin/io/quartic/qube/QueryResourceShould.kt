package io.quartic.qube

import io.quartic.common.model.CustomerId
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.qube.api.model.Dag
import io.quartic.qube.resource.QueryResource
import org.hamcrest.Matchers
import org.junit.Assert.assertThat
import org.junit.Test

class QueryResourceShould {
    private val dag = OBJECT_MAPPER.readValue(javaClass.getResourceAsStream("/pipeline.json"), Dag::class.java)
    private val resource = QueryResource(dag)

    @Test
    fun fetch_dag_for_customer() {
        val dag = resource.dag(CustomerId("111"))
        assertThat(dag, Matchers.equalTo(dag))
    }
}
