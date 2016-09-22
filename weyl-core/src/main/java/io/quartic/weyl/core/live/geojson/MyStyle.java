package io.quartic.weyl.core.live.geojson;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

@JsonSerialize
@Value.Style(
        jdkOnly = true,
        typeAbstract = "Abstract*",
        typeImmutable = "*",
        visibility = ImplementationVisibility.PUBLIC)
@interface MyStyle {
}
