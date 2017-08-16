package io.quartic.common.secrets

import java.io.File

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
            val fileMode = args.asList().contains("-f")

            // Key-gen mode
            if (keyGenMode) {
                println(SecretsCodec.generateMasterKeyBase64().veryUnsafe)
                System.exit(0)
            }

            val codec = SecretsCodec(getUnsafeSecretFromUser("Master key (base64-encoded): "))

            println("=== ${if (decodeMode) "Decode" else "Encode"} mode ===")
            println()

            while (true) {
                if (decodeMode) {
                    println(codec.decrypt(getEncryptedSecretFromUser("Encrypted secret: ")).veryUnsafe)
                } else {
                    if (fileMode) {
                        println(codec.encrypt(getUnsafeSecretFromFile("Secret file (unencoded): ")).somewhatUnsafe)
                    } else {
                        println(codec.encrypt(getUnsafeSecretFromUser("Secret (unencoded): ")).somewhatUnsafe)
                    }
                }
            }
        }

        // We deliberately use this to discourage this CLI being scripted (because that would inevitably lead to
        // unencrypted secrets being stored on disk (in a script) on in the shell history).
        private fun getEncryptedSecretFromUser(prompt: String) = EncryptedSecret(getSecretFromUser(prompt))
        private fun getUnsafeSecretFromUser(prompt: String) = UnsafeSecret(getSecretFromUser(prompt))
        private fun getUnsafeSecretFromFile(prompt: String) = UnsafeSecret(getSecretFromFile(prompt))
        private fun getSecretFromUser(prompt: String) = String(System.console().readPassword(prompt))
        private fun getSecretFromFile(prompt: String) = File(System.console().readLine(prompt)).readText()
    }
}
