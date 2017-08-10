package io.quartic.glisten

import io.quartic.common.application.ConfigurationBase
import io.quartic.common.secrets.EncryptedSecret
import java.net.URL

data class GlistenConfiguration(
    val bildUrl: URL,
    val webhookSecretEncrypted: EncryptedSecret
) : ConfigurationBase()


