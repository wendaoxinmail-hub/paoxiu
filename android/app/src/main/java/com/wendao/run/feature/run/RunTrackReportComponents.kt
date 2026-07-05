package com.wendao.run.feature.run

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wendao.run.core.database.entity.RunRecordEntity
import com.wendao.run.core.run.KmSplit
import com.wendao.run.core.run.PaceSample
import com.wendao.run.core.run.PaceZoneSlice
import com.wendao.run.core.run.QiIndexUtils
import com.wendao.run.core.run.RunAnalytics
import com.wendao.run.core.run.KeepRunStats
import com.wendao.run.core.run.RunGeoUtils
import com.wendao.run.core.run.RunKeepMetrics
import com.wendao.run.core.run.RunTrendComparison
import com.wendao.run.core.run.formatSplitDuration
import com.wendao.run.ui.components.CultivationCard
import com.wendao.run.ui.components.KeepStatBlock
import com.wendao.run.ui.components.SectionHeader
import com.wendao.run.ui.theme.PaoxiuColors

private const val DETAIL_MAP_HEIGHT_DP = 280

@Composable
fun RunReportHero(
    run: RunRecordEntity,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "户外修炼",
            style = MaterialTheme.typography.titleMedium,
            color = PaoxiuColors.InkMuted,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "%.2f".format(run.distanceM / 1000.0),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = PaoxiuColors.SilkWhite,
                ),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "公里",
                style = MaterialTheme.typography.titleMedium,
                color = PaoxiuColors.InkMuted,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
fun RunStatsGrid(
    run: RunRecordEntity,
    keepStats: KeepRunStats?,
    modifier: Modifier = Modifier,
) {
    val stats = keepStats ?: RunKeepMetrics.compute(run, emptyList())
    val rows = listOf(
        listOf(
            Triple("总时间", run.summaryDuration(), false),
            Triple("平均配速", run.summaryPace(), true),
            Triple("千卡", RunKeepMetrics.formatCalories(stats.caloriesKcal), false),
        ),
        listOf(
            Triple("移动时间", RunGeoUtils.formatDuration(stats.movingDurationSec), false),
            Triple("平均心率", RunKeepMetrics.formatHeartRate(stats.avgHeartRateBpm), false),
            Triple("平均步频", RunKeepMetrics.formatCadence(stats.avgCadenceSpm), false),
        ),
        listOf(
            Triple("平均步幅", RunKeepMetrics.formatStride(stats.avgStrideCm), false),
            Triple("累计爬升", RunKeepMetrics.formatElevation(stats.elevationGainM), false),
            Triple("累计下降", RunKeepMetrics.formatElevation(stats.elevationLossM), false),
        ),
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { (label, value, accent) ->
                    KeepStatBlock(
                        label = label,
                        value = value,
                        accent = accent,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun RunTrendSection(
    trend: RunTrendComparison,
    modifier: Modifier = Modifier,
) {
    val tags = listOfNotNull(trend.paceTrendLabel, trend.distanceTrendLabel)
    if (tags.isEmpty()) return
    Column(modifier = modifier) {
        SectionHeader("修炼趋势")
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tags) { tag ->
                TrendChip(text = tag)
            }
        }
    }
}

@Composable
private fun TrendChip(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = PaoxiuColors.GrottoTeal,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = PaoxiuColors.KeepGreen)
    }
}

@Composable
fun RunSplitsSection(
    splits: List<KmSplit>,
    modifier: Modifier = Modifier,
) {
    if (splits.isEmpty()) return
    val fastest = splits.minByOrNull { it.paceSecPerKm ?: Double.MAX_VALUE }
    Column(modifier = modifier) {
        SectionHeader("分段详情")
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        ) {
            Text(
                text = "公里",
                modifier = Modifier.weight(0.7f),
                style = MaterialTheme.typography.labelMedium,
                color = PaoxiuColors.InkMuted,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "耗时",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = PaoxiuColors.InkMuted,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "配速",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = PaoxiuColors.InkMuted,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "气机",
                modifier = Modifier.weight(0.8f),
                style = MaterialTheme.typography.labelMedium,
                color = PaoxiuColors.InkMuted,
                textAlign = TextAlign.Center,
            )
        }
        splits.forEach { split ->
            val isFastest = split == fastest && split.paceSecPerKm != null
            val textColor = if (isFastest) PaoxiuColors.KeepGreen else PaoxiuColors.KeepTextPrimary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isFastest) PaoxiuColors.KeepGreen.copy(alpha = 0.12f)
                        else Color.Transparent,
                        RoundedCornerShape(4.dp),
                    )
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${split.kmIndex}",
                    modifier = Modifier.weight(0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = formatSplitDuration(split.durationSec),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = RunGeoUtils.formatPace(split.paceSecPerKm),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (split.avgQiIndex > 0) "${split.avgQiIndex}" else "—",
                    modifier = Modifier.weight(0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun RunPaceChartSection(
    paceSeries: List<PaceSample>,
    avgPaceSecPerKm: Double?,
    modifier: Modifier = Modifier,
) {
    if (paceSeries.size < 2) return
    val bestPace = paceSeries.minOfOrNull { it.paceSecPerKm }
    Column(modifier = modifier) {
        SectionHeader("配速曲线")
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "平均 ${RunGeoUtils.formatPace(avgPaceSecPerKm)}",
                style = MaterialTheme.typography.bodySmall,
                color = PaoxiuColors.InkMuted,
            )
            bestPace?.let {
                Text(
                    text = "最快 ${RunGeoUtils.formatPace(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PaoxiuColors.SpiritJade,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        PaceLineChart(
            samples = paceSeries,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
        )
    }
}

@Composable
private fun PaceLineChart(
    samples: List<PaceSample>,
    modifier: Modifier = Modifier,
) {
    val lineColor = PaoxiuColors.SpiritJade
    val gridColor = PaoxiuColors.InkMuted.copy(alpha = 0.2f)
    Canvas(modifier = modifier) {
        if (samples.size < 2) return@Canvas
        val maxMin = samples.minOf { it.paceSecPerKm }
        val maxMax = samples.maxOf { it.paceSecPerKm }
        val paceRange = (maxMax - maxMin).coerceAtLeast(30.0)
        val maxMinute = samples.maxOf { it.minute }.coerceAtLeast(1f)
        val w = size.width
        val h = size.height
        val pad = 8f

        for (i in 0..3) {
            val y = pad + (h - 2 * pad) * i / 3f
            drawLine(gridColor, Offset(pad, y), Offset(w - pad, y), strokeWidth = 1f)
        }

        val path = Path()
        samples.forEachIndexed { index, sample ->
            val x = pad + (sample.minute / maxMinute) * (w - 2 * pad)
            val normalized = ((sample.paceSecPerKm - maxMin) / paceRange).toFloat()
            val y = pad + normalized * (h - 2 * pad)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 3f))
    }
}

@Composable
fun RunPaceZoneSection(
    zones: List<PaceZoneSlice>,
    modifier: Modifier = Modifier,
) {
    if (zones.isEmpty()) return
    Column(modifier = modifier) {
        SectionHeader("配速区间")
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            zones.forEach { zone ->
                Box(
                    modifier = Modifier
                        .weight(zone.ratio.coerceAtLeast(0.05f))
                        .fillMaxHeight()
                        .background(Color(zone.colorArgb), RoundedCornerShape(2.dp)),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            zones.forEach { zone ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(8.dp)
                            .background(Color(zone.colorArgb), RoundedCornerShape(2.dp)),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${zone.label} ${(zone.ratio * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaoxiuColors.InkMuted,
                    )
                }
            }
        }
    }
}

@Composable
fun RunQiSection(
    avgQiIndex: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader("气机推演")
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "平均 ${if (avgQiIndex > 0) avgQiIndex else "—"} · ${QiIndexUtils.label(avgQiIndex)}",
            style = MaterialTheme.typography.bodySmall,
            color = PaoxiuColors.InkMuted,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "未连接心脉仪，气机由配速与步罡节奏推演，不代表真实心率。",
            style = MaterialTheme.typography.bodySmall,
            color = PaoxiuColors.InkMuted.copy(alpha = 0.7f),
        )
    }
}

@Composable
fun RunTrainingEffectSection(
    analytics: RunAnalytics,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader("修炼效果")
        Spacer(modifier = Modifier.height(12.dp))
        TrainingBar(
            label = "有氧修行",
            score = analytics.trainingAerobic,
            description = trainingLabel(analytics.trainingAerobic),
        )
        Spacer(modifier = Modifier.height(12.dp))
        TrainingBar(
            label = "灵力凝聚",
            score = analytics.trainingSpirit,
            description = trainingLabel(analytics.trainingSpirit),
        )
    }
}

@Composable
private fun TrainingBar(
    label: String,
    score: Float,
    description: String,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = PaoxiuColors.KeepTextPrimary)
            Text(
                text = "%.1f · $description".format(score),
                style = MaterialTheme.typography.bodySmall,
                color = PaoxiuColors.KeepGreen,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(PaoxiuColors.KeepSectionBg, RoundedCornerShape(4.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(score / 5f)
                    .fillMaxHeight()
                    .background(PaoxiuColors.KeepGreen, RoundedCornerShape(4.dp)),
            )
        }
    }
}

private fun trainingLabel(score: Float): String = when {
    score >= 4f -> "显著提升"
    score >= 3f -> "稳步精进"
    score >= 2f -> "维持状态"
    else -> "初窥门径"
}

@Composable
fun RunReportExtras(
    run: RunRecordEntity,
    modifier: Modifier = Modifier,
) {
    val extras = buildList {
        run.realmLabelAfter?.let { add("境界 · $it") }
        run.adventureTitle?.let { add("奇遇 · $it") }
    }
    if (extras.isEmpty()) return
    CultivationCard(modifier = modifier) {
        extras.forEach { line ->
            Text(text = line, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

fun detailMapHeightDp(): Int = DETAIL_MAP_HEIGHT_DP
