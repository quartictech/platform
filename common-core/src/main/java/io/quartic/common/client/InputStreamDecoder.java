package io.quartic.common.client;

import feign.FeignException;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

public class InputStreamDecoder implements Decoder {
    private final Decoder delegate;

    public InputStreamDecoder(Decoder delegate) {
       this.delegate = delegate;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException, FeignException {
        // Adapted from: http://www.monkeypatch.io/2016/08/10/MKTD-1-feign-vs-retrofit-&-58;-2-going-further.html
        if (InputStream.class.equals(type)) {
            return response.body().asInputStream();
        }
        return delegate.decode(response, type);
    }
}
