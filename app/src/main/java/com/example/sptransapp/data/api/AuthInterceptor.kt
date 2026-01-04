package com.example.sptransapp.data.api

import com.example.sptransapp.BuildConfig
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (shouldAuthenticate(response)) {

            response.close()

            synchronized(this) {

                val requestRetry = request.newBuilder().build()
                val responseRetry = chain.proceed(requestRetry)

                if (responseRetry.isSuccessful) {
                    return responseRetry
                }

                responseRetry.close()

                val loginUrl = request.url.newBuilder()
                    .scheme("http")
                    .host("api.olhovivo.sptrans.com.br")
                    .encodedPath("/v2.1/Login/Autenticar")
                    .setQueryParameter("token", BuildConfig.SPTRANS_TOKEN)
                    .build()

                val loginRequest = Request.Builder()
                    .url(loginUrl)
                    .post("".toRequestBody(null))
                    .build()

                try {
                    val loginResponse = chain.proceed(loginRequest)

                    if (loginResponse.isSuccessful) {
                        loginResponse.close()
                        return chain.proceed(request)
                    } else {
                        loginResponse.close()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        return response
    }

    private fun shouldAuthenticate(response: Response): Boolean {
        return response.code == 401
    }
}
