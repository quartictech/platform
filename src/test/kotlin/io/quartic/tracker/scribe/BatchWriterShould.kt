package io.quartic.tracker.scribe

import org.junit.jupiter.api.Test
import java.time.Instant

class BatchWriterShould {
    @Test
    fun blaj() {
        BatchWriter("oliver-drivel", "tracker").write(
                listOf("love", "hate", "emo"),
                Instant.now(),
                3
        )
    }
}