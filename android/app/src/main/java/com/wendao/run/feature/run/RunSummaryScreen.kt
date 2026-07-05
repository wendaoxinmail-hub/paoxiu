package com.wendao.run.feature.run

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wendao.run.core.util.ShareUtils
import com.wendao.run.ui.components.CultivationCard
import com.wendao.run.core.location.RunMapView
import com.wendao.run.ui.components.PaoxiuBackground
import com.wendao.run.ui.components.PaoxiuLayout
import com.wendao.run.ui.components.PaoxiuPrimaryButton
import kotlinx.coroutines.delay

/**
 * 修炼小结：Keep 式数据汇总 + 境界突破高亮 + 分享。
 */
@Composable
fun RunSummaryScreen(
    runId: String,
    onDone: () -> Unit,
    onBreakthrough: (String) -> Unit = {},
    onOpenDetail: (String) -> Unit = {},
    viewModel: RunSummaryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showMap by remember { mutableStateOf(false) }

    LaunchedEffect(runId) {
        viewModel.load(runId)
    }

    LaunchedEffect(state.trackPoints) {
        if (state.trackPoints.isNotEmpty()) {
            delay(800)
            showMap = true
        }
    }

    PaoxiuBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "修炼小结",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            if (state.loading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                if (state.trackPoints.isNotEmpty()) {
                    if (showMap) {
                        RunMapView(
                            trackPoints = state.trackPoints,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(PaoxiuLayout.MapHeightDp.dp),
                            fitRoute = state.trackPoints.size >= 2,
                            showStartEndMarkers = true,
                            trackEffect = state.trackEffect,
                        )
                    } else {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        text = "轨迹 · ${state.trackEffect.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                val run = state.run
                if (run?.leveledUp == true) {
                    CultivationCard(highlight = true) {
                        Text("境界突破", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Text("恭喜道友晋升 ${run.realmLabelAfter ?: "新境界"}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                CultivationCard {
                    if (run == null) {
                        Text("未找到本次修炼记录")
                    } else {
                        SummaryRow("距离", run.summaryDistance())
                        SummaryRow("时长", run.summaryDuration())
                        SummaryRow("配速", run.summaryPace())
                        SummaryRow("同步", run.syncStatusLabel())
                        run.rejectReason?.let { SummaryRow("校验", it) }
                        if ((run.rewardStones ?: 0) > 0 || (run.rewardXp ?: 0) > 0) {
                            SummaryRow("收获", "+${run.rewardStones ?: 0} 灵石 · +${run.rewardXp ?: 0} 修为")
                        }
                        run.realmLabelAfter?.let { SummaryRow("境界", it) }
                        run.adventureTitle?.let { title ->
                            SummaryRow("奇遇", title)
                            run.adventureDescription?.let { desc ->
                                Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
                run?.let { finished ->
                    OutlinedButton(
                        onClick = { onOpenDetail(runId) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("查看轨迹详情") }
                    OutlinedButton(
                        onClick = { ShareUtils.shareText(context, "分享修炼成果", buildShareText(finished)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("分享修炼成果")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (!state.loading && state.run?.leveledUp == true) {
                PaoxiuPrimaryButton(text = "进入渡劫仪式", onClick = { onBreakthrough(runId) })
                Spacer(modifier = Modifier.height(8.dp))
            }
            PaoxiuPrimaryButton(text = "返回修炼页", onClick = onDone)
        }
    }
}

private fun buildShareText(run: com.wendao.run.core.database.entity.RunRecordEntity): String {
    val lines = buildList {
        add("【跑修】本次修炼 ${run.summaryDistance()} · ${run.summaryDuration()}")
        if ((run.rewardXp ?: 0) > 0) add("修为 +${run.rewardXp}")
        if ((run.rewardStones ?: 0) > 0) add("灵石 +${run.rewardStones}")
        run.realmLabelAfter?.let { add("境界：$it") }
        run.adventureTitle?.let { add("奇遇：$it") }
        add("—— 以足为剑，以汗为丹")
    }
    return lines.joinToString("\n")
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Text(text = "$label · $value", style = MaterialTheme.typography.bodyLarge)
}
