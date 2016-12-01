package io.quartic.howl.storage;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.io.InputStream;

@SweetStyle
@Value.Immutable
public interface InputStreamWithContentType extends AutoCloseable {
    String contentType();
    InputStream inputStream();

    @Override
    default void close() throws Exception {
        inputStream().close();
    }
}
