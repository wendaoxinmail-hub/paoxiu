package com.wendao.run.core.location

import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.MapStatusUpdateFactory
import com.baidu.mapapi.map.MapView
import com.baidu.mapapi.map.Marker
import com.baidu.mapapi.map.MarkerOptions
import com.baidu.mapapi.map.Overlay
import com.baidu.mapapi.map.PolylineOptions
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.model.LatLngBounds
import com.wendao.run.core.run.RunGeoUtils
import com.wendao.run.core.run.RunTrackPoint
import com.wendao.run.core.run.TrackVisualEffect

/**
 * 增量更新百度地图 overlay，避免每帧 clear() 引发蓝点漂移与 native 闪退。
 */
internal class RunMapOverlayController(private val mapView: MapView) {

    private val context get() = mapView.context
    private var runnerMarker: Marker? = null
    private val trackOverlays = mutableListOf<Overlay>()
    private var lastTrackKey: String? = null
    private var lastRunnerKey: String? = null
    private var lastFitKey: String? = null

    fun prepareMap(baiduMap: BaiduMap) {
        baiduMap.isMyLocationEnabled = false
        baiduMap.uiSettings.apply {
            isScrollGesturesEnabled = true
            isZoomGesturesEnabled = true
        }
    }

    fun updateRunner(baiduMap: BaiduMap, lat: Double, lng: Double) {
        val key = "%.6f,%.6f".format(lat, lng)
        if (key == lastRunnerKey && runnerMarker != null) return
        lastRunnerKey = key
        baiduMap.isMyLocationEnabled = false
        val pos = LatLng(lat, lng)
        val existing = runnerMarker
        if (existing == null) {
            runnerMarker = baiduMap.addOverlay(
                MarkerOptions()
                    .position(pos)
                    .icon(BaiduMapIconUtils.runnerIcon(context))
                    .anchor(0.5f, 0.5f)
                    .zIndex(10),
            ) as Marker
        } else {
            existing.position = pos
        }
    }

    fun updateTrack(
        baiduMap: BaiduMap,
        trackPoints: List<RunTrackPoint>,
        trackEffect: TrackVisualEffect,
        isLiveRun: Boolean,
        showStartEndMarkers: Boolean = false,
    ) {
        if (trackPoints.isEmpty()) {
            clearTrackOverlays()
            lastTrackKey = null
            return
        }
        val key = buildString {
            append(trackPoints.size)
            trackPoints.lastOrNull()?.let { append("|${it.lat}|${it.lng}") }
            append("|${trackEffect.label}|$isLiveRun|$showStartEndMarkers")
        }
        if (key == lastTrackKey) return
        lastTrackKey = key
        clearTrackOverlays()

        val segments = RunGeoUtils.splitTrackByGap(trackPoints)
        if (segments.isEmpty()) return

        val pulse = if (isLiveRun && trackEffect.pulseGlow) {
            0.92f + 0.08f * kotlin.math.sin(System.currentTimeMillis() / 800.0).toFloat()
        } else {
            1f
        }
        val auraW = (trackEffect.auraWidth * pulse).toInt().coerceAtLeast(trackEffect.lineWidth + 4)
        val lineW = (trackEffect.lineWidth * pulse).toInt().coerceAtLeast(8)

        for (segment in segments) {
            if (segment.size < 2) continue
            val latLngs = segment.map { LatLng(it.lat, it.lng) }
            trackOverlays += baiduMap.addOverlay(
                PolylineOptions().width(auraW).color(trackEffect.auraColor).points(latLngs),
            )
            trackOverlays += baiduMap.addOverlay(
                PolylineOptions().width(lineW).color(trackEffect.primaryColor).points(latLngs),
            )
            if (segment.size >= 4) {
                trackOverlays += baiduMap.addOverlay(
                    PolylineOptions()
                        .width((lineW * 0.4f).toInt().coerceAtLeast(3))
                        .color(trackEffect.accentColor)
                        .points(latLngs),
                )
            }
        }
        if (showStartEndMarkers && trackPoints.size >= 2) {
            val all = trackPoints.map { LatLng(it.lat, it.lng) }
            trackOverlays += baiduMap.addOverlay(
                MarkerOptions()
                    .position(all.first())
                    .icon(BaiduMapIconUtils.startIcon(context))
                    .title("起"),
            )
            trackOverlays += baiduMap.addOverlay(
                MarkerOptions()
                    .position(all.last())
                    .icon(BaiduMapIconUtils.endIcon(context))
                    .title("终"),
            )
        }
    }

    fun recenter(baiduMap: BaiduMap, lat: Double, lng: Double, zoom: Float = 17f) {
        baiduMap.animateMapStatus(
            MapStatusUpdateFactory.newLatLngZoom(LatLng(lat, lng), zoom),
        )
    }

    fun fitRoute(baiduMap: BaiduMap, trackPoints: List<RunTrackPoint>) {
        if (trackPoints.size < 2) return
        val fitKey = "${trackPoints.size}|${trackPoints.first().lat}|${trackPoints.last().lat}"
        if (fitKey == lastFitKey) return
        lastFitKey = fitKey
        val latLngs = trackPoints.map { LatLng(it.lat, it.lng) }
        try {
            val builder = LatLngBounds.Builder()
            latLngs.forEach { builder.include(it) }
            val bounds = builder.build()
            val w = mapView.width.coerceAtLeast(200)
            val h = mapView.height.coerceAtLeast(200)
            baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngBounds(bounds, w, h))
        } catch (_: Exception) {
            recenter(baiduMap, latLngs.last().latitude, latLngs.last().longitude, 16f)
        }
    }

    fun dispose() {
        runnerMarker?.remove()
        runnerMarker = null
        clearTrackOverlays()
        lastTrackKey = null
        lastRunnerKey = null
        lastFitKey = null
        try {
            mapView.map?.isMyLocationEnabled = false
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun clearTrackOverlays() {
        trackOverlays.forEach { overlay ->
            try {
                overlay.remove()
            } catch (_: Exception) {
                // ignore
            }
        }
        trackOverlays.clear()
    }
}
