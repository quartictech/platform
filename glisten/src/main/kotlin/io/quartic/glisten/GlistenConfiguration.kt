package io.quartic.glisten

import io.quartic.common.application.ConfigurationBase
import io.quartic.common.secrets.EncryptedSecret
import java.net.URI

data class GlistenConfiguration(
    val evalUrl: URI,
    val webhookSecretEncrypted: EncryptedSecret
) : ConfigurationBase()


