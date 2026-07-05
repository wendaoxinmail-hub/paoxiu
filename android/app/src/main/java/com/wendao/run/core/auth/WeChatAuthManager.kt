package com.wendao.run.core.auth

import android.content.Context
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import com.wendao.run.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeChatAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var wxApi: IWXAPI? = null

    fun isConfigured(): Boolean = BuildConfig.WECHAT_APP_ID.isNotBlank()

    fun getApi(): IWXAPI? {
        if (!isConfigured()) return null
        if (wxApi == null) {
            wxApi = WXAPIFactory.createWXAPI(context, BuildConfig.WECHAT_APP_ID, true).also {
                it.registerApp(BuildConfig.WECHAT_APP_ID)
            }
        }
        return wxApi
    }

    fun isWeChatInstalled(): Boolean = getApi()?.isWXAppInstalled == true

    fun sendAuthRequest(): Boolean {
        val api = getApi() ?: return false
        val req = SendAuth.Req().apply {
            scope = "snsapi_userinfo"
            state = "paoxiu_${System.currentTimeMillis()}"
        }
        return api.sendReq(req)
    }
}
