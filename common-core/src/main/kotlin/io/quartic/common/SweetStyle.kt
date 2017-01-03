package io.quartic.common

import org.immutables.value.Value
import org.immutables.value.Value.Style.ImplementationVisibility

@Value.Style(
        add = "*",
        addAll = "all*",
        put = "*",
        putAll = "all*",
        depluralize = true,
        allParameters = true,
        jdkOnly = true,
        typeImmutable = "*Impl",
        visibility = ImplementationVisibility.PUBLIC
)
annotation class SweetStyle
