package io.quartic.tracker.scribe.healthcheck

import com.google.cloud.pubsub.SubscriptionInfo
import com.google.cloud.pubsub.Topic
import com.google.cloud.pubsub.testing.LocalPubSubHelper
import org.joda.time.Duration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PubSubSubscriptionHealthCheckShould {
    private val helper = LocalPubSubHelper.create()
    private val pubsub = helper.options.service
    private val healthcheck = PubSubSubscriptionHealthCheck(pubsub, "mySubscription")

    @AfterEach
    fun after() {
        try {
            helper.stop(Duration.millis(3000))
        } catch (e: Exception) { }
    }

    @Test
    fun report_healthy_if_pubsub_alive_and_subscription_exists() {
        helper.start()
        pubsub.create(Topic.of("myTopic"))
        pubsub.create(SubscriptionInfo.of("myTopic", "mySubscription"))

        assertTrue(healthcheck.execute().isHealthy)
    }

    @Test
    fun report_unhealthy_if_pubsub_alive_but_subscription_doesnt_exist() {
        helper.start()
        pubsub.create(Topic.of("myTopic"))

        assertFalse(healthcheck.execute().isHealthy)
    }

    @Test
    fun report_unhealthy_if_pubsub_not_alive() {
        assertFalse(healthcheck.execute().isHealthy)
    }
}