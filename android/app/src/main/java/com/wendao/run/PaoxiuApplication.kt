package com.wendao.run

import android.app.Application
import com.wendao.run.core.location.BaiduMapInitializer
import com.wendao.run.core.sync.RunSyncWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PaoxiuApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BaiduMapInitializer.initialize(this)
        RunSyncWorker.schedule(this)
    }
}
