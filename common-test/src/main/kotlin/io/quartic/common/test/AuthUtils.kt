package io.quartic.common.test

import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.secrets.UnsafeSecret

val MASTER_KEY_BASE64 = SecretsCodec.generateMasterKeyBase64()
val TOKEN_KEY_BASE64 = UnsafeSecret("BffwOJzi7ejTe9yC1IpQ4+P6fYpyGz+GvVyrfhamNisNqa96CF8wGSp3uATaITUP7r9n6zn9tDN8k4424zwZ2Q==")   // 512-bit key
