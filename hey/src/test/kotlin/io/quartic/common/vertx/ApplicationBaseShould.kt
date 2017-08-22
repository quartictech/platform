package io.quartic.common.vertx

import io.quartic.common.secrets.EncryptedSecret
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.common.vertx.ApplicationBase.Companion.DEV_MASTER_KEY_BASE64
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.EnvironmentVariables
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class ApplicationBaseShould {
    @Rule
    @JvmField
    val context = RunTestOnContext()

    @Rule
    @JvmField
    val env = EnvironmentVariables()

    @Before
    fun before() {
        // Just in case env is polluted
        env.set("DEV_MODE", "true")
        env.set("MASTER_KEY_BASE64", DEV_MASTER_KEY_BASE64.veryUnsafe)
    }

    class CompletingApp : ApplicationBase(1234) {
        override fun customise(future: Future<Void>, rawConfig: JsonObject) {
            future.complete()
        }
    }

    @Test
    fun fail_if_using_dev_key_in_prod(context: TestContext) {
        env.set("DEV_MODE", null)

        deploy(CompletingApp(), context.asyncAssertFailure())
    }

    @Test
    fun succeed_if_using_dev_key_in_dev(context: TestContext) {
        deploy(CompletingApp(), context.asyncAssertSuccess())
    }

    @Test
    fun decrypt_secrets_correctly(context: TestContext) {
        val original = UnsafeSecret("Alex loves TDD")
        env.set("CONTROVERSIAL_SECRET", SecretsCodec(DEV_MASTER_KEY_BASE64).encrypt(original).somewhatUnsafe)

        var decrypted: UnsafeSecret? = null
        deploy(
            app = object : ApplicationBase(1234) {
                override fun customise(future: Future<Void>, rawConfig: JsonObject) {
                    decrypted = EncryptedSecret(rawConfig.map["CONTROVERSIAL_SECRET"] as String).decrypt()
                    future.complete()
                }

            },
            handler = context.asyncAssertSuccess {
                context.assertEquals(original, decrypted)
            }
        )
    }

    // TODO - test mappers configured correctly

    // TODO - test request bodies are accessible

    // TODO - test getting custom config

    // TODO - test listening to requests

    private fun deploy(app: ApplicationBase, handler: Handler<AsyncResult<String>>) {
        context.vertx().deployVerticle(app, handler)
    }
}
