package io.quartic.common.client;

import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public class InputStreamEncoder implements Encoder {
    private final Encoder delegate;

    public InputStreamEncoder(Encoder delegate) {
        this.delegate = delegate;
    }

    @Override
    public void encode(Object object, Type bodyType, RequestTemplate template) throws EncodeException {
        // Adapted from: http://www.monkeypatch.io/2016/08/10/MKTD-1-feign-vs-retrofit-&-58;-2-going-further.html
        if (InputStream.class.equals(bodyType)) {
            InputStream inputStream = InputStream.class.cast(object);
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                IOUtils.copy(inputStream, bos);
                template.body(bos.toByteArray(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new EncodeException("Cannot upload file", e);
            }
        } else {
            delegate.encode(object, bodyType, template);
        }
    }
}
