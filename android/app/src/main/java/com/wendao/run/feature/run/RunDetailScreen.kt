package com.wendao.run.feature.run

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wendao.run.core.location.RunMapView
import com.wendao.run.core.util.ShareUtils
import com.wendao.run.ui.components.CultivationCard
import com.wendao.run.ui.components.PaoxiuBackground
import com.wendao.run.ui.components.PaoxiuLayout
import com.wendao.run.ui.components.PaoxiuPrimaryButton
import com.wendao.run.ui.components.SectionHeader
import com.wendao.run.ui.theme.PaoxiuColors

/** Keep 式修炼详情：轨迹地图 + 数据网格 + 分段 + 配速图表（不含 AI 总结） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunDetailScreen(
    runId: String,
    onBack: () -> Unit,
    viewModel: RunDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(runId) { viewModel.load(runId) }

    Scaffold(
        containerColor = PaoxiuColors.KeepCanvas,
        topBar = {
            TopAppBar(
                title = { Text("修炼详情", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    state.run?.let { run ->
                        IconButton(
                            onClick = {
                                ShareUtils.shareText(
                                    context,
                                    "分享修炼轨迹",
                                    buildShareText(run, state.trackPoints.size, state.analytics),
                                )
                            },
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "分享")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaoxiuColors.KeepSurface,
                    titleContentColor = PaoxiuColors.KeepTextPrimary,
                ),
            )
        },
    ) { padding ->
        PaoxiuBackground {
            if (state.loading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val run = state.run
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (run == null) {
                        Text(
                            text = "未找到记录",
                            modifier = Modifier.padding(PaoxiuLayout.ScreenHorizontalDp.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        return@Column
                    }

                    Column(
                        modifier = Modifier.padding(horizontal = PaoxiuLayout.ScreenHorizontalDp.dp),
                        verticalArrangement = Arrangement.spacedBy(PaoxiuLayout.SectionSpacingDp.dp),
                    ) {
                        RunReportHero(run = run)
                    }

                    Spacer(modifier = Modifier.height(PaoxiuLayout.SectionSpacingDp.dp))

                    key(runId) {
                        RunMapView(
                            trackPoints = state.trackPoints,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(detailMapHeightDp().dp),
                            fitRoute = state.trackPoints.size >= 2,
                            showStartEndMarkers = state.trackPoints.size >= 2,
                            trackEffect = state.trackEffect,
                        )
                    }
                    if (state.trackPoints.isNotEmpty()) {
                        Text(
                            text = "轨迹 · ${state.trackEffect.label}",
                            modifier = Modifier.padding(horizontal = PaoxiuLayout.ScreenHorizontalDp.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }

                    Column(
                        modifier = Modifier.padding(
                            horizontal = PaoxiuLayout.ScreenHorizontalDp.dp,
                            vertical = PaoxiuLayout.ScreenVerticalDp.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(PaoxiuLayout.SectionSpacingDp.dp),
                    ) {
                        if (state.trackPoints.isEmpty()) {
                            Text(
                                text = "本次修炼未记录到轨迹点",
                                style = MaterialTheme.typography.bodySmall,
                                color = PaoxiuColors.InkMuted,
                            )
                        }

                        CultivationCard {
                            SectionHeader("修炼数据")
                            Spacer(modifier = Modifier.height(8.dp))
                            RunStatsGrid(
                                run = run,
                                keepStats = state.keepStats,
                            )
                        }

                        state.analytics?.let { analytics ->
                            val trendTags = listOfNotNull(
                                analytics.trend.paceTrendLabel,
                                analytics.trend.distanceTrendLabel,
                            )
                            if (trendTags.isNotEmpty()) {
                                CultivationCard {
                                    RunTrendSection(trend = analytics.trend)
                                }
                            }

                            if (analytics.splits.isNotEmpty()) {
                                CultivationCard {
                                    RunSplitsSection(splits = analytics.splits)
                                }
                            }

                            if (analytics.paceSeries.size >= 2) {
                                CultivationCard {
                                    RunPaceChartSection(
                                        paceSeries = analytics.paceSeries,
                                        avgPaceSecPerKm = run.avgPaceSecPerKm,
                                    )
                                    if (analytics.paceZones.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        RunPaceZoneSection(zones = analytics.paceZones)
                                    }
                                }
                            }

                            CultivationCard {
                                RunQiSection(avgQiIndex = analytics.avgQiIndex)
                            }

                            CultivationCard {
                                RunTrainingEffectSection(analytics = analytics)
                            }
                        }

                        RunReportExtras(run = run)

                        PaoxiuPrimaryButton(
                            text = "分享修炼成果",
                            onClick = {
                                ShareUtils.shareText(
                                    context,
                                    "分享修炼成果",
                                    buildShareText(run, state.trackPoints.size, state.analytics),
                                )
                            },
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

private fun buildShareText(
    run: com.wendao.run.core.database.entity.RunRecordEntity,
    pointCount: Int,
    analytics: com.wendao.run.core.run.RunAnalytics?,
): String {
    return buildString {
        appendLine("【跑修】${run.summaryDistance()} · ${run.summaryDuration()}")
        appendLine("配速 ${run.summaryPace()} · 轨迹 $pointCount 点")
        analytics?.avgQiIndex?.takeIf { it > 0 }?.let {
            appendLine("气机指数 $it · ${com.wendao.run.core.run.QiIndexUtils.label(it)}")
        }
        run.realmLabelAfter?.let { appendLine("境界：$it") }
        append("—— 以足为剑，以汗为丹")
    }
}
