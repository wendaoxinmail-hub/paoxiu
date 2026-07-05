package com.wendao.run.core.location

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.wendao.run.core.run.RunTrackPoint
import com.wendao.run.core.run.TrackVisualEffect

/**
 * 跑步/轨迹地图（Keep 式全屏底图 + 右下原生定位按钮）。
 * 跑步中蓝点用 Marker 绘制，不用百度 MyLocation（避免漂移/闪退）。
 */
@Composable
fun RunMapView(
    trackPoints: List<RunTrackPoint>,
    runnerLat: Double? = null,
    runnerLng: Double? = null,
    modifier: Modifier = Modifier,
    fitRoute: Boolean = false,
    showStartEndMarkers: Boolean = false,
    showLocateButton: Boolean = true,
    locateBottomPadding: Dp = 16.dp,
    trackEffect: TrackVisualEffect = TrackVisualEffect.Default,
    isLiveRun: Boolean = false,
    onLocationPermissionRequired: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val locateBottomPx = with(LocalDensity.current) { locateBottomPadding.roundToPx() }

    var locateToken by remember { mutableIntStateOf(0) }
    var lastAnimatedLocateToken by remember { mutableIntStateOf(0) }
    var lastLocateClickMs by remember { mutableStateOf(0L) }
    var hasInitialCenter by remember { mutableStateOf(false) }

    val mapLocationController = remember { MapViewLocationController(context) }
    val runnerLatState by rememberUpdatedState(runnerLat)
    val runnerLngState by rememberUpdatedState(runnerLng)

    fun requestLocate() {
        val now = System.currentTimeMillis()
        if (now - lastLocateClickMs < 600L) return
        lastLocateClickMs = now
        val rLat = runnerLatState
        val rLng = runnerLngState
        PaoxiuLocationLog.map(
            "locate click",
            "runner=($rLat,$rLng) finePerm=${context.hasFineLocationPermission()}",
        )
        if (rLat != null && rLng != null) {
            locateToken++
            PaoxiuLocationLog.map("locate use runner coords")
            return
        }
        context.mapLocationTracker().peekLastFix()?.let { cached ->
            PaoxiuLocationLog.map("locate use cache", "(${cached.lat},${cached.lng})")
            locateToken++
            return
        }
        Toast.makeText(context, "正在定位…", Toast.LENGTH_SHORT).show()
        context.mapLocationTracker().requestSingleFix { update ->
            if (update != null) {
                locateToken++
            } else {
                Toast.makeText(context, "定位失败，请到室外或检查定位权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants[android.Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            requestLocate()
        }
    }

    val onLocateClick = rememberUpdatedState {
        if (!context.hasFineLocationPermission()) {
            onLocationPermissionRequired?.invoke()
                ?: permissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
        } else {
            requestLocate()
        }
    }

    LaunchedEffect(runnerLat, runnerLng, isLiveRun) {
        if (runnerLat != null && runnerLng != null && isLiveRun && !hasInitialCenter) {
            hasInitialCenter = true
            locateToken++
        }
    }

    DisposableEffect(isLiveRun) {
        if (!isLiveRun && context.hasFineLocationPermission()) {
            mapLocationController.start { locateToken++ }
        }
        onDispose { mapLocationController.stop() }
    }

    val mapHost = remember(locateBottomPx, isLiveRun) {
        MapHostView(
            context = context,
            locateBottomMarginPx = locateBottomPx,
            locateOnRight = true,
            showZoomControls = !isLiveRun,
        )
    }

    DisposableEffect(lifecycleOwner, mapHost) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapHost.resumeMap()
                Lifecycle.Event.ON_PAUSE -> mapHost.pauseMap()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapHost.resumeMap()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapHost.destroyMap()
        }
    }

    val focusLat = runnerLat
    val focusLng = runnerLng

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapHost },
            update = { host ->
                if (host.destroyed) return@AndroidView
                host.onLocateClick = { onLocateClick.value() }
                host.setLocateVisible(showLocateButton)
                val baiduMap = host.mapView.map ?: return@AndroidView
                val controller = host.overlayController
                controller.prepareMap(baiduMap)

                controller.updateTrack(
                    baiduMap,
                    trackPoints,
                    trackEffect,
                    isLiveRun,
                    showStartEndMarkers = showStartEndMarkers,
                )

                if (focusLat != null && focusLng != null) {
                    controller.updateRunner(baiduMap, focusLat, focusLng)
                }

                val shouldRecenter = locateToken > lastAnimatedLocateToken
                if (shouldRecenter) {
                    lastAnimatedLocateToken = locateToken
                    if (focusLat != null && focusLng != null) {
                        controller.recenter(baiduMap, focusLat, focusLng)
                    } else if (trackPoints.size == 1) {
                        val p = trackPoints.first()
                        controller.recenter(baiduMap, p.lat, p.lng)
                    }
                } else if (fitRoute && trackPoints.size >= 2) {
                    controller.fitRoute(baiduMap, trackPoints)
                }
            },
        )
    }
}
