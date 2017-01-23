package io.quartic.tracker.scribe

import com.google.cloud.pubsub.TopicInfo
import com.google.cloud.pubsub.testing.LocalPubSubHelper
import org.joda.time.Duration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class Balls {
    private val helper = LocalPubSubHelper.create()

    @BeforeEach
    fun before() {
        helper.start()
    }

    @AfterEach
    fun after() {
        helper.stop(Duration.millis(3000))
    }

    @Test
    fun name() {

        val service = helper.options.service

        val topic = service.create(TopicInfo.of("Balls"))

        service.getSubscription("Sub")
        service.getSubscription("Sub")
    }
}