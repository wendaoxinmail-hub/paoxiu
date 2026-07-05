package com.wendao.run.core.location

import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageButton
import com.baidu.mapapi.map.MapView
import com.wendao.run.R

/**
 * 百度 MapView 容器：Keep 式原生定位按钮（右下、白底十字准星）叠在地图之上。
 */
internal class MapHostView(
    context: Context,
    private val locateBottomMarginPx: Int,
    private val locateOnRight: Boolean = true,
    private val showZoomControls: Boolean = false,
) : FrameLayout(context) {

    val mapView = MapView(context)
    val overlayController = RunMapOverlayController(mapView)
    val locateButton = ImageButton(context)

    @Volatile
    var destroyed = false
        private set

    var onLocateClick: (() -> Unit)? = null

    init {
        addView(
            mapView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
        val margin = 16.dpToPx(context)
        val size = 44.dpToPx(context)
        addView(
            locateButton,
            LayoutParams(size, size).apply {
                gravity = Gravity.BOTTOM or if (locateOnRight) Gravity.END else Gravity.START
                if (locateOnRight) {
                    rightMargin = margin
                } else {
                    leftMargin = margin
                }
                bottomMargin = locateBottomMarginPx.coerceAtLeast(margin)
            },
        )
        locateButton.apply {
            setImageResource(R.drawable.ic_map_locate_crosshair)
            contentDescription = "定位到当前位置"
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            isClickable = true
            isFocusable = true
            setPadding(10.dpToPx(context), 10.dpToPx(context), 10.dpToPx(context), 10.dpToPx(context))
            setBackgroundResource(R.drawable.bg_map_locate_button)
            elevation = 8f
            stateListAnimator = null
            setOnClickListener { onLocateClick?.invoke() }
        }
        try {
            mapView.setZOrderMediaOverlay(false)
        } catch (_: Exception) {
            // ignore
        }
        mapView.showZoomControls(showZoomControls)
        locateButton.bringToFront()
    }

    fun setLocateVisible(visible: Boolean) {
        locateButton.visibility = if (visible) VISIBLE else GONE
    }

    fun resumeMap() {
        if (destroyed) return
        try {
            mapView.onResume()
        } catch (_: Exception) {
            // ignore
        }
    }

    fun pauseMap() {
        if (destroyed) return
        try {
            mapView.onPause()
        } catch (_: Exception) {
            // ignore
        }
    }

    fun destroyMap() {
        if (destroyed) return
        destroyed = true
        overlayController.dispose()
        try {
            mapView.onPause()
        } catch (_: Exception) {
            // ignore
        }
        try {
            mapView.onDestroy()
        } catch (_: Exception) {
            // ignore
        }
    }

    companion object {
        private const val VISIBLE = android.view.View.VISIBLE
        private const val GONE = android.view.View.GONE
    }
}
