package com.wendao.run.core.location

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.baidu.mapapi.map.BitmapDescriptor
import com.baidu.mapapi.map.BitmapDescriptorFactory
import com.wendao.run.R

internal object BaiduMapIconUtils {

    private var runnerIcon: BitmapDescriptor? = null
    private var startIcon: BitmapDescriptor? = null
    private var endIcon: BitmapDescriptor? = null

    fun runnerIcon(context: Context): BitmapDescriptor =
        runnerIcon ?: descriptorFromDrawable(context, R.drawable.ic_map_runner_dot).also { runnerIcon = it }

    fun startIcon(context: Context): BitmapDescriptor =
        startIcon ?: descriptorFromDrawable(context, R.drawable.ic_map_marker_start).also { startIcon = it }

    fun endIcon(context: Context): BitmapDescriptor =
        endIcon ?: descriptorFromDrawable(context, R.drawable.ic_map_marker_end).also { endIcon = it }

    private fun descriptorFromDrawable(context: Context, @DrawableRes resId: Int): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(context, resId)
            ?: return BitmapDescriptorFactory.fromResource(resId)
        val density = context.resources.displayMetrics.density
        val width = (drawable.intrinsicWidth.takeIf { it > 0 } ?: (20 * density).toInt())
            .coerceAtLeast(1)
        val height = (drawable.intrinsicHeight.takeIf { it > 0 } ?: (20 * density).toInt())
            .coerceAtLeast(1)
        drawable.setBounds(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply { drawable.draw(this) }
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
