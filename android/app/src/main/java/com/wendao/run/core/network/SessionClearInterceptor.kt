package com.wendao.run.core.network

import com.wendao.run.core.data.SessionStore
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 服务端 JWT 失效时清除本地 session，避免后续请求持续 401。
 */
class SessionClearInterceptor(
    private val sessionStore: SessionStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            val path = chain.request().url.encodedPath
            if (!path.startsWith("/api/v1/auth")) {
                sessionStore.clear()
            }
        }
        return response
    }
}
