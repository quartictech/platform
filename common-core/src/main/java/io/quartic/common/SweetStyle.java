package io.quartic.common;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

@JsonSerialize
@Value.Style(
        add = "*",
        addAll = "all*",
        depluralize = true,
        allParameters = true,
        jdkOnly = true,
        typeAbstract = "Abstract*",
        typeImmutable = "*",
        visibility = ImplementationVisibility.PUBLIC)
public @interface SweetStyle {
}
