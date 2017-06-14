package io.quartic.howl.api

import io.quartic.common.serdes.decode
import okhttp3.*
import okio.BufferedSink
import java.io.InputStream
import java.io.OutputStream

class HowlClient(userAgent: String, private val baseUrl: String) : HowlService {
    private class UserAgentInterceptor(private val userAgent: String) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val requestWithUserAgent = originalRequest.newBuilder()
                    .header("User-Agent", userAgent)
                    .build()
            return chain.proceed(requestWithUserAgent)
        }
    }

    private val client = OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor(userAgent))
            .build()

    override fun uploadFile(contentType: String, namespace: String, fileName: String, upload: (OutputStream) -> Unit) {
        val request = Request.Builder()
                .url(url(namespace, fileName))
                .put(requestBody(contentType, upload))
                .build()
        client.newCall(request).execute()
    }

    override fun uploadFile(contentType: String, namespace: String, upload: (OutputStream) -> Unit): HowlStorageId {
        val request = Request.Builder()
                .url(url(namespace))
                .post(requestBody(contentType, upload))
                .build()
        return decode(client.newCall(request).execute().body().string(), HowlStorageId::class.java)
    }

    override fun downloadFile(namespace: String, fileName: String): InputStream? {
        val request = Request.Builder()
                .url(url(namespace, fileName))
                .get()
                .build()

        val response = client.newCall(request).execute()

        return if (response.isSuccessful) {
            response.body().byteStream()
        } else {
            null
        }
    }

    private fun url(namespace: String, fileName: String) = HttpUrl.parse(baseUrl)
            .newBuilder()
            .addEncodedPathSegment(namespace)
            .addEncodedPathSegment(fileName)
            .build()

    private fun url(namespace: String) = HttpUrl.parse(baseUrl)
            .newBuilder()
            .addEncodedPathSegment(namespace)
            .build()

    private fun requestBody(contentType: String, upload: (OutputStream) -> Unit): RequestBody {
        return object : RequestBody() {
            override fun contentType(): MediaType {
                return MediaType.parse(contentType)
            }

            override fun writeTo(sink: BufferedSink) {
                upload(sink.outputStream())
            }
        }
    }
}
