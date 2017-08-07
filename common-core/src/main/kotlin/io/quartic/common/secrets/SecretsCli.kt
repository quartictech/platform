package io.quartic.common.secrets

import io.quartic.common.secrets.SecretsCodec.EncryptedSecret

class SecretsCli {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val console = System.console()
            if (console == null) {
                System.err.println("Running in a non-console environment")
                System.exit(1)
            }

            val keyGenMode = args.asList().contains("-g")
            val decodeMode = args.asList().contains("-d")

            // Key-gen mode
            if (keyGenMode) {
                println(SecretsCodec.generateMasterKeyBase64())
                System.exit(0)
            }

            val masterKeyBase64 = getSecretFromUser("Master key (base64-encoded): ")
            val codec = SecretsCodec(masterKeyBase64)

            println("=== ${if (decodeMode) "Decode" else "Encode"} mode ===")
            println()

            while (true) {
                if (decodeMode) {
                    val encryptedSecret = getSecretFromUser("Encrypted secret: ")
                    println(codec.decrypt(EncryptedSecret(encryptedSecret)))
                } else {
                    val secret = getSecretFromUser("Secret (unencoded): ")
                    println(codec.encrypt(secret))
                }
            }
        }

        /**
         * We deliberately use this to prevent this CLI being scripted (and thus encouraging secrets to be stored
         * on disk (in a script) on in the shell history.
         */
        private fun getSecretFromUser(prompt: String) = String(System.console().readPassword(prompt))
    }
}
