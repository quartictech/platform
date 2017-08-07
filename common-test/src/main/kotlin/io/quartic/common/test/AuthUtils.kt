package io.quartic.common.test

import io.quartic.common.secrets.SecretsCodec

val MASTER_KEY = SecretsCodec.generateMasterKey()
val TOKEN_KEY_BASE64 = "BffwOJzi7ejTe9yC1IpQ4+P6fYpyGz+GvVyrfhamNisNqa96CF8wGSp3uATaITUP7r9n6zn9tDN8k4424zwZ2Q=="   // 512-bit key
