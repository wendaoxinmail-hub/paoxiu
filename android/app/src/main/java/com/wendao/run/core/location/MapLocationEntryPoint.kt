package com.wendao.run.core.location

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MapLocationEntryPoint {
    fun locationTracker(): BaiduRunLocationTracker
}

fun Context.mapLocationTracker(): BaiduRunLocationTracker =
    EntryPointAccessors.fromApplication(applicationContext, MapLocationEntryPoint::class.java)
        .locationTracker()
