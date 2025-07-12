package org.ganquan.musictimer.tools

import android.annotation.SuppressLint
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

private var client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS) //设置连接超时
    .readTimeout(10, TimeUnit.SECONDS) //设置读超时
    .writeTimeout(10, TimeUnit.SECONDS) //设置写超时
    .retryOnConnectionFailure(true) //是否自动重连
    // 设置https配置，此处忽略了所有证书
    .sslSocketFactory(createEasySSLContext().socketFactory, EasyX509TrustManager(null))
    .build()

class Http {
    companion object {
        const val BACK_FORMAT_ORIGIN = "origin"
        const val BACK_FORMAT_JSON = "json"
        const val BACK_FORMAT_DOWN = "down"
        const val METHOD_GET = "GET"
        const val METHOD_POST = "POST"

        fun get(url: String,
                callback: (Any?) -> Unit = {},
                params: Map<String, Any>? = null
        ): Any? {
            return request(url, callback, METHOD_GET, params)
        }

        fun post(url: String,
                 callback: (Any?) -> Unit = {},
                 body: Map<String, Any>? = null,
                 params: Map<String, Any>? = null
        ): Any? {
            val formBody = FormBody.Builder()
            if(body != null) {
                for ((key, value) in body) {
                    formBody.add(key, Json.toJson(value))
                }
            }
            return request(url, callback, METHOD_POST, params, formBody.build())
        }

        fun down(
            url: String,
            savePath: String,
            callback: (String) -> Unit = {},
            progress: (List<Long>) -> Unit = {}
        ) {
            request(url, { result ->
                if(result == null) callback("")
                val inputStream: InputStream? = (result as ResponseBody).byteStream()
                if (inputStream == null) callback("")
                else {
                    var outputStream: FileOutputStream? = null
                    try {
                        outputStream = FileOutputStream(savePath)
                        val buffer = ByteArray(2048)
                        val total: Long = result.contentLength()
                        var len = 0
                        var sum = 0L
                        while ((inputStream.read(buffer).also { len = it }) != -1) {
                            sum += len
                            outputStream.write(buffer, 0, len)
                            progress(listOf(sum, total))
                        }
                        outputStream.flush()
                        callback(savePath)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        callback("")
                    }
                }
            }, backFormat = BACK_FORMAT_DOWN, isAsync = true)
        }

        fun request(url: String,
                    callback: (Any?) -> Unit = {},
                    method: String? = null,
                    params: Map<String, Any>? = null,
                    body: RequestBody? = null,
                    headers: Map<String, String?>? = null,
                    backFormat: String = BACK_FORMAT_JSON,
                    isAsync: Boolean = false
        ): Any? {
            var uri = url
            if(params != null) {
                uri += if(uri.contains("?")) ":" else "?"
                for ((k, v) in params) {
                    uri += "$k=$v"
                }
            }

            var req = getBuilder(uri)
            if(method != null) req = req.method(method,body)
            if(headers != null) {
                for ((k, v) in headers) {
//                .addHeader(k, v) // 添加不覆盖
                    if (v != null) req.header(k, v) else req.removeHeader(k)
                }
            }

            if(isAsync) {
                client.newCall(req.build()).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        e.printStackTrace()
                        callback(null)
                    }

                    override fun onResponse(call: Call, res: Response) {
                        try {
                            if (res.isSuccessful) {
                                val resBody = res.body
                                if(resBody == null) {
                                    callback(null)
                                } else {
                                    callback(when (backFormat) {
                                        BACK_FORMAT_DOWN -> resBody
                                        BACK_FORMAT_ORIGIN -> resBody.string()
                                        else -> Json.parseGJson(resBody.string().toString())
                                    })
                                }
                            } else {
                                callback(null)
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            callback(null)
                        }
                    }
                })
                return null
            } else {
                val result = try {
                    val call = client.newCall(req.build())
                    val res: Response = call.execute()
                    return if(res.isSuccessful) {
                        val data = res.body?.string()
                        return when(backFormat) {
                            BACK_FORMAT_ORIGIN -> data
                            else -> Json.parseGJson(data.toString())
                        }
                    } else null
                } catch (e: IOException) {
                    e.printStackTrace()
                    return null
                } catch (e: JSONException) {
                    e.printStackTrace()
                    return null
                }
                callback(result)
            }
        }

        private fun getBuilder(uri: String): Request.Builder {
            return Request.Builder().url(uri)
        }
    }
}

private fun createEasySSLContext(): SSLContext {
    try {
        val context: SSLContext = SSLContext.getInstance("TLS")
        context.init(null, null, null)
        return context
    } catch (e: Exception) {
        throw IOException(e.message)
    }
}

@SuppressLint("CustomX509TrustManager")
class EasyX509TrustManager(keystore: KeyStore?) : X509TrustManager {
    private var standardTrustManager: X509TrustManager? = null

    /**
     * Constructor for EasyX509TrustManager.
     */
    init {
        val factory: TrustManagerFactory =
            TrustManagerFactory.getInstance(
                TrustManagerFactory
                    .getDefaultAlgorithm()
            )
        factory.init(keystore)
        val trustManagers: Array<TrustManager?> = factory.trustManagers
        if (trustManagers.isEmpty()) {
            throw java.security.NoSuchAlgorithmException("no trust manager found")
        }
        this.standardTrustManager = trustManagers[0] as X509TrustManager?
    }

    /**
     * @see X509TrustManager.checkClientTrusted
     */
    override fun checkClientTrusted(
        certificates: Array<X509Certificate?>?,
        authType: String?
    ) {
        standardTrustManager?.checkClientTrusted(certificates, authType)
    }

    /**
     * @see X509TrustManager.checkServerTrusted
     */
    override fun checkServerTrusted(
        certificates: Array<X509Certificate?>?,
        authType: String?
    ) {
        if ((certificates != null) && (certificates.size == 1)) {
            certificates[0]?.checkValidity()
        } else {
            standardTrustManager?.checkServerTrusted(certificates, authType)
        }
    }

    /**
     * @see X509TrustManager.getAcceptedIssuers
     */
    override fun getAcceptedIssuers(): Array<X509Certificate?>? {
        return this.standardTrustManager?.acceptedIssuers
    }
}