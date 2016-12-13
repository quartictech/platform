package io.quartic.howl.api;

import io.quartic.common.serdes.ObjectMappers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

public class HowlClient implements HowlService {
    private static class UserAgentInterceptor implements Interceptor {

        private final String userAgent;

        public UserAgentInterceptor(String userAgent) {
            this.userAgent = userAgent;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request requestWithUserAgent = originalRequest.newBuilder()
                    .header("User-Agent", userAgent)
                    .build();
            return chain.proceed(requestWithUserAgent);
        }
    }

    private final String baseUrl;
    private final OkHttpClient client;

    public HowlClient(String userAgent, String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient.Builder()
                .addInterceptor(new UserAgentInterceptor(userAgent))
                .build();
    }

    private HttpUrl url(String namespace, String fileName) {
       return HttpUrl.parse(baseUrl)
               .newBuilder()
               .addEncodedPathSegment(namespace)
               .addEncodedPathSegment(fileName)
               .build();
    }

    private HttpUrl url(String namespace) {
        return HttpUrl.parse(baseUrl)
                .newBuilder()
                .addEncodedPathSegment(namespace)
                .build();
    }

    private RequestBody requestBody(String contentType, Consumer<OutputStream> upload) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return MediaType.parse(contentType);
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                upload.accept(sink.outputStream());
            }
        };
    }

    @Override
    public void uploadFile(String contentType, String namespace, String fileName, Consumer<OutputStream> upload) throws IOException {
        Request request = new Request.Builder()
                .url(url(namespace, fileName))
                .put(requestBody(contentType, upload))
                .build();
        client.newCall(request).execute();
    }

    @Override
    public HowlStorageId uploadFile(String contentType, String namespace, Consumer<OutputStream> upload) throws IOException {
        Request request = new Request.Builder()
                .url(url(namespace))
                .post(requestBody(contentType, upload))
                .build();
        return ObjectMappers.decode(client.newCall(request).execute().body().string(), HowlStorageId.class);
    }

    @Override
    public InputStream downloadFile(String namespace, String fileName) throws IOException {
        HttpUrl url = url(namespace, fileName);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return client.newCall(request).execute().body().byteStream();

    }
}
