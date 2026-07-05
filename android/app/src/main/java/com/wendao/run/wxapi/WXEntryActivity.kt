package com.wendao.run.wxapi

import android.app.Activity
import android.content.Intent
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import com.wendao.run.BuildConfig
import com.wendao.run.core.auth.WeChatAuthResultBus

/** 微信 SDK 回调入口（包名必须为 wxapi） */
class WXEntryActivity : Activity(), IWXAPIEventHandler {

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        val api = WXAPIFactory.createWXAPI(this, BuildConfig.WECHAT_APP_ID, false)
        api.handleIntent(intent, this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val api = WXAPIFactory.createWXAPI(this, BuildConfig.WECHAT_APP_ID, false)
        api.handleIntent(intent, this)
    }

    override fun onReq(req: BaseReq?) = Unit

    override fun onResp(resp: BaseResp?) {
        if (resp is SendAuth.Resp) {
            if (resp.errCode == BaseResp.ErrCode.ERR_OK && !resp.code.isNullOrBlank()) {
                WeChatAuthResultBus.publishSuccess(resp.code)
            } else {
                WeChatAuthResultBus.publishError("微信授权取消或失败 (${resp?.errCode})")
            }
        }
        finish()
    }
}
