package io.quartic.bild

import com.nhaarman.mockito_kotlin.mock
import io.quartic.bild.api.model.Dag
import io.quartic.bild.resource.QueryResource
import io.quartic.bild.store.BuildStore
import io.quartic.common.model.CustomerId
import io.quartic.common.serdes.OBJECT_MAPPER
import org.hamcrest.Matchers
import org.junit.Assert.assertThat
import org.junit.Test


class QueryResourceShould {
    private val dag = OBJECT_MAPPER.readValue(javaClass.getResourceAsStream("/pipeline.json"), Dag::class.java)
    private val buildStore = mock<BuildStore>()
    private val resource = QueryResource(buildStore, dag)

    @Test
    fun fetch_dag_for_customer() {
        val dag = resource.dag(CustomerId("111"))
        assertThat(dag, Matchers.equalTo(dag))
    }
}
