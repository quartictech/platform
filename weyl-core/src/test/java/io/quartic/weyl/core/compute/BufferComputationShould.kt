package io.quartic.weyl.core.compute

import com.google.common.collect.Lists.newArrayList
import com.google.common.collect.Lists.transform
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.GeometryFactory
import io.quartic.common.test.entry
import io.quartic.common.test.map
import io.quartic.weyl.core.feature.FeatureCollection.EMPTY_COLLECTION
import io.quartic.weyl.core.model.*
import io.quartic.weyl.core.model.Attributes.EMPTY_ATTRIBUTES
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import rx.observers.TestSubscriber
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class BufferComputationShould {
    private lateinit var computation: BufferComputation
    private val myLayerId = mock<LayerId>()
    private val sourceLayerId = mock<LayerId>()
    private val layer = layer()

    @Before
    fun before() {
        computation = BufferComputation(
                myLayerId,
                BufferSpec(sourceLayerId, 25.0),
                Clock.fixed(Instant.EPOCH, ZoneId.systemDefault())
        )
    }

    @Test
    fun produce_valid_metadata() {
        assertThat(computation.spec(newArrayList(layer)).metadata, equalTo(LayerMetadata(
                "Foo (buffered)",
                "Bar (buffered by 25.0m)",
                "Quartic",
                Instant.EPOCH
        )))
    }

    @Test
    fun not_complete() {
        val subscriber = TestSubscriber.create<LayerUpdate>()
        computation.updates(transform(computation.dependencies(), { layerMap()[it] }))
                .subscribe(subscriber)

        TimeUnit.SECONDS.sleep(2)
        subscriber.assertNotCompleted()
    }

    private fun layerMap() = map(entry(sourceLayerId, layer))

    private fun layer(): Layer {
        val myMetadata = mock<LayerMetadata> {
            on { name } doReturn "Foo"
            on { description } doReturn "Bar"
            on { attribution } doReturn "Quartic"
        }
        val mySpec = mock<LayerSpec> {
            on { metadata } doReturn myMetadata
            on { staticSchema } doReturn StaticSchema()
        }
        return mock {
            on { spec } doReturn mySpec
            on { features } doReturn EMPTY_COLLECTION.append(listOf(feature()))
        }
    }

    private fun feature() = Feature(EntityId("test"), point(), EMPTY_ATTRIBUTES)

    private fun point() = GeometryFactory().createPoint(Coordinate(0.0, 0.0))
}
