package com.wendao.run.feature.run

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wendao.run.core.location.RunMapView
import com.wendao.run.core.location.openAppSettings
import com.wendao.run.core.weather.WeatherSnapshot
import com.wendao.run.core.run.QiIndexUtils
import com.wendao.run.core.run.RunGeoUtils
import com.wendao.run.core.run.RunSessionStatus
import com.wendao.run.ui.components.KeepStatBlock
import com.wendao.run.ui.components.RealmProgressBar
import com.wendao.run.ui.theme.PaoxiuColors
import kotlinx.coroutines.delay

/** 底部数据面板高度（定位按钮需在其上方，对齐 Keep） */
private val RunStatsPanelHeight = 200.dp

/**
 * Keep 式修炼页：全屏地图 + 底部浮层数据/操作区。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunScreen(
    onBack: () -> Unit,
    onRunFinished: (String) -> Unit,
    viewModel: RunViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activeRun by viewModel.activeRun.collectAsStateWithLifecycle()
    val trackPoints by viewModel.trackPoints.collectAsStateWithLifecycle()
    val trackEffect by viewModel.trackEffect.collectAsStateWithLifecycle()
    val finishedRunId by viewModel.finishedRunId.collectAsStateWithLifecycle()
    val isFinishing by viewModel.isFinishing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val locationGate by viewModel.locationGate.collectAsStateWithLifecycle()
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    var lastMilestone by remember { mutableIntStateOf(0) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        viewModel.onLocationPermissionResult(
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true,
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.onNotificationPermissionResult()
    }

    fun launchLocationPermission() {
        locationPermissionLauncher.launch(RunViewModel.locationPermissions)
    }

    fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshLocationGate(onRequestPermission = ::launchLocationPermission)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshLocationGate(onRequestPermission = ::launchLocationPermission)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(locationGate) {
        if (locationGate == RunLocationGate.Ready) {
            requestNotificationsIfNeeded()
        }
    }

    LaunchedEffect(activeRun?.distanceM) {
        val distance = activeRun?.distanceM ?: return@LaunchedEffect
        if (!viewModel.isSpiritRootTest) return@LaunchedEffect
        val milestone = (distance / 200).toInt()
        if (milestone > lastMilestone && milestone in 1..5) {
            lastMilestone = milestone
        }
    }

    LaunchedEffect(finishedRunId) {
        val runId = finishedRunId ?: return@LaunchedEffect
        delay(300)
        onRunFinished(runId)
        viewModel.consumeFinishedRun()
    }

    val title = if (viewModel.isSpiritRootTest) "灵根测试" else "修炼中"

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaoxiuColors.KeepSurface.copy(alpha = 0.92f),
                    titleContentColor = PaoxiuColors.KeepTextPrimary,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            RunMapView(
                trackPoints = trackPoints,
                runnerLat = activeRun?.displayLat ?: activeRun?.lastLat,
                runnerLng = activeRun?.displayLng ?: activeRun?.lastLng,
                modifier = Modifier.fillMaxSize(),
                locateBottomPadding = RunStatsPanelHeight + 12.dp,
                trackEffect = trackEffect,
                isLiveRun = true,
                onLocationPermissionRequired = {
                    viewModel.requestLocationPermission(::launchLocationPermission)
                },
            )

            if (locationGate == RunLocationGate.NeedPermission ||
                locationGate == RunLocationGate.Denied ||
                locationGate == RunLocationGate.GpsOff
            ) {
                LocationPermissionBanner(
                    gate = locationGate,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    onRequestPermission = { viewModel.requestLocationPermission(::launchLocationPermission) },
                    onOpenSettings = { context.openAppSettings() },
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(PaoxiuColors.KeepSurface.copy(alpha = 0.96f))
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (viewModel.isSpiritRootTest) {
                    val progress = ((activeRun?.distanceM ?: 0.0) / viewModel.spiritRootTargetM)
                        .coerceIn(0.0, 1.0)
                        .toFloat()
                    RealmProgressBar(progress = progress)
                    Text(
                        text = "灵根试炼 ${RunGeoUtils.formatDistance(activeRun?.distanceM ?: 0.0)} / 1.00 km",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                when {
                    isFinishing -> {
                        Text(
                            text = "正在收功结算…",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    locationGate != RunLocationGate.Ready && activeRun == null -> {
                        Text(
                            text = when (locationGate) {
                                RunLocationGate.GpsOff -> "等待 GPS 开启…"
                                RunLocationGate.Denied -> "未获得定位权限"
                                else -> "请允许精确位置权限"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    activeRun == null -> {
                        Text(
                            text = "正在启阵定位…",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (!viewModel.isSpiritRootTest) {
                            RunQiWeatherSection(weather = weather)
                        }
                    }
                    else -> {
                        val run = activeRun!!
                        Text(
                            text = "轨迹 · ${trackEffect.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            KeepStatBlock("距离", RunGeoUtils.formatDistance(run.distanceM))
                            KeepStatBlock("时长", RunGeoUtils.formatDuration(run.durationSec))
                            KeepStatBlock(
                                "配速",
                                RunGeoUtils.formatPace(
                                    run.paceSecPerKm ?: RunGeoUtils.paceFromSpeedKmh(run.currentSpeedKmh),
                                ),
                                accent = true,
                            )
                        }
                        if (!viewModel.isSpiritRootTest) {
                            RunQiWeatherSection(
                                weather = weather,
                                paceSecPerKm = run.paceSecPerKm,
                                currentSpeedKmh = run.currentSpeedKmh,
                            )
                        }
                        run.locationHint?.let { hint ->
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                error?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (activeRun?.status) {
                        RunSessionStatus.PAUSED -> {
                            OutlinedButton(
                                onClick = viewModel::resumeRun,
                                modifier = Modifier.weight(1f),
                            ) { Text("继续") }
                        }
                        RunSessionStatus.RUNNING -> {
                            OutlinedButton(
                                onClick = viewModel::pauseRun,
                                modifier = Modifier.weight(1f),
                            ) { Text("暂停") }
                        }
                        null -> Unit
                    }
                    Button(
                        onClick = viewModel::stopRun,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PaoxiuColors.HeartDemon,
                        ),
                        enabled = activeRun != null && !isFinishing,
                    ) {
                        Text(
                            when {
                                isFinishing -> "收功中…"
                                viewModel.isSpiritRootTest -> "完成试炼"
                                else -> "收功"
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RunQiWeatherSection(
    weather: WeatherSnapshot?,
    paceSecPerKm: Double? = null,
    currentSpeedKmh: Float = 0f,
) {
    val qi = QiIndexUtils.fromWeather(weather)
        .takeIf { it > 0 }
        ?: QiIndexUtils.estimate(paceSecPerKm, currentSpeedKmh)
    Text(
        text = "气机指数 $qi · ${QiIndexUtils.label(qi)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.secondary,
    )
    Text(
        text = QiIndexUtils.comfortHint(weather) ?: "正在感应天气…",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun LocationPermissionBanner(
    gate: RunLocationGate,
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PaoxiuColors.KeepSurface.copy(alpha = 0.98f))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = when (gate) {
                RunLocationGate.GpsOff -> "请打开手机 GPS"
                RunLocationGate.Denied -> "定位权限未开启"
                else -> "需要精确位置权限"
            },
            style = MaterialTheme.typography.titleMedium,
            color = PaoxiuColors.KeepTextPrimary,
        )
        Text(
            text = when (gate) {
                RunLocationGate.GpsOff -> "修炼需要 GPS 信号，请在系统设置中打开「位置信息」。"
                else -> "跑修需要「精确位置」才能记录轨迹，请选择「使用时允许」并开启精确位置。"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (gate == RunLocationGate.Denied || gate == RunLocationGate.GpsOff) {
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("去系统设置")
            }
        }
        if (gate != RunLocationGate.GpsOff) {
            OutlinedButton(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
                Text(if (gate == RunLocationGate.Denied) "再次请求权限" else "授予定位权限")
            }
        }
    }
}
