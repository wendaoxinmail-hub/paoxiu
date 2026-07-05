package com.wendao.run.core.location

import android.content.Context
import com.baidu.location.LocationClient
import com.baidu.mapapi.CoordType
import com.baidu.mapapi.SDKInitializer
import com.wendao.run.BuildConfig

object BaiduMapInitializer {

    fun initialize(context: Context) {
        val apiKey = BuildConfig.BAIDU_MAP_API_KEY
        // 百度 SDK 要求：用户同意隐私政策后再初始化
        SDKInitializer.setAgreePrivacy(context.applicationContext, true)
        LocationClient.setAgreePrivacy(true)
        // 定位 SDK 读 com.baidu.lbsapi.API_KEY；代码 setKey 优先级更高
        if (apiKey.isNotBlank()) {
            LocationClient.setKey(apiKey)
        }
        SDKInitializer.initialize(context.applicationContext)
        SDKInitializer.setCoordType(CoordType.GCJ02)
        PaoxiuLocationLog.appInit(context)
        PaoxiuLocationLog.i("BaiduMapInitializer done coord=GCJ02 lbsapiKeySet=${apiKey.isNotBlank()}")
    }
}
