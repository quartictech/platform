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

    override fun uploadFile(
            targetNamespace: String,
            fileName: String,
            contentType: String,
            upload: (OutputStream) -> Unit
    ) = uploadFile(url(targetNamespace, fileName), contentType, upload)

    override fun uploadFile(
            targetNamespace: String,
            identityNamespace: String,
            fileName: String,
            contentType: String,
            upload: (OutputStream) -> Unit
    ) = uploadFile(url(targetNamespace, identityNamespace, fileName), contentType, upload)

    private fun uploadFile(url: HttpUrl, contentType: String, upload: (OutputStream) -> Unit) {
        val request = Request.Builder()
                .url(url)
                .put(requestBody(contentType, upload))
                .build()
        client.newCall(request).execute()
    }

    override fun uploadAnonymousFile(
            targetNamespace: String,
            contentType: String,
            upload: (OutputStream) -> Unit
    ) = uploadAnonymousFile(url(targetNamespace), contentType, upload)

    override fun uploadAnonymousFile(
            targetNamespace: String,
            identityNamespace: String,
            contentType: String,
            upload: (OutputStream) -> Unit
    ) = uploadAnonymousFile(url(targetNamespace, identityNamespace), contentType, upload)

    private fun uploadAnonymousFile(url: HttpUrl, contentType: String, upload: (OutputStream) -> Unit): HowlStorageId {
        val request = Request.Builder()
                .url(url)
                .post(requestBody(contentType, upload))
                .build()
        return decode(client.newCall(request).execute().body().string(), HowlStorageId::class.java)
    }

    override fun downloadFile(targetNamespace: String, fileName: String)
            = downloadFile(url(targetNamespace, fileName))

    override fun downloadFile(targetNamespace: String, identityNamespace: String, fileName: String)
            = downloadFile(url(targetNamespace, identityNamespace, fileName))

    private fun downloadFile(url: HttpUrl): InputStream? {
        val request = Request.Builder()
                .url(url)
                .get()
                .build()

        val response = client.newCall(request).execute()

        return if (response.isSuccessful) {
            response.body().byteStream()
        } else {
            null
        }
    }

    private fun url(vararg components: String) = with (HttpUrl.parse(baseUrl).newBuilder()) {
        components.forEach { addEncodedPathSegment(it) }
        build()
    }

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
