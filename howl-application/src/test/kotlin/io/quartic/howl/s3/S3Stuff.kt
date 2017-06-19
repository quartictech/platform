package io.quartic.howl.s3

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.junit.Test

class S3Stuff {
    @Test
    fun name() {
        val s3 = AmazonS3ClientBuilder.defaultClient()

        println("Exists = ${s3.doesBucketExist(BUCKET_NAME)}")

//        s3.listBuckets().forEach { println("Bucket: $it") }
    }

    companion object {
        val BUCKET_NAME = "howl-test"
    }
}