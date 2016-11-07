package io.quartic.management;

import io.quartic.weyl.common.SweetStyle;
import org.immutables.value.Value;

import java.io.InputStream;

@SweetStyle
@Value.Immutable
public interface AbstractInputStreamWithContentType {
    String contentType();
    InputStream inputStream();
}
