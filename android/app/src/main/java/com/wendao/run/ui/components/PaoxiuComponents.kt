package com.wendao.run.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wendao.run.ui.theme.PaoxiuColors
import com.wendao.run.ui.theme.StatLabelStyle
import com.wendao.run.ui.theme.StatNumberStyle

/**
 * Keep 式白卡片 + 国风赤金细边框。
 */
@Composable
fun CultivationCard(
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) {
                PaoxiuColors.BreakthroughGlow
            } else {
                PaoxiuColors.KeepSurface
            },
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (highlight) {
                PaoxiuColors.KeepGreen.copy(alpha = 0.35f)
            } else {
                PaoxiuColors.KeepDivider
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlight) 4.dp else 1.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/**
 * Keep 风格数据块：大号数值 + 小号标签，用于跑步页/首页核心指标。
 */
@Composable
fun KeepStatBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = StatNumberStyle.copy(
                color = if (accent) PaoxiuColors.KeepGreen else PaoxiuColors.KeepTextPrimary,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = StatLabelStyle)
    }
}

/**
 * 区块标题 + 水墨分隔线。
 */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        GoldDivider()
    }
}

/** 赤金细线分隔，模拟法阵纹边 */
@Composable
fun GoldDivider(modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { 1f },
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp),
        color = PaoxiuColors.KeepGreen.copy(alpha = 0.45f),
        trackColor = PaoxiuColors.KeepDivider,
        strokeCap = StrokeCap.Round,
        drawStopIndicator = {},
    )
}

/**
 * 藏经阁/装备库条目底部操作区：统一高度，避免卡片长短不一。
 */
@Composable
fun CatalogActionRow(
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    statusLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            actionLabel != null && onAction != null -> {
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
            statusLabel != null -> {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * 主行动按钮：Keep 式全宽绿色胶囊。
 */
@Composable
fun PaoxiuPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(
            containerColor = PaoxiuColors.KeepGreen,
            contentColor = PaoxiuColors.KeepSurface,
            disabledContainerColor = PaoxiuColors.KeepGreen.copy(alpha = 0.35f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * 境界修为进度条。
 *
 * @param progress 0f..1f
 */
@Composable
fun RealmProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape),
        color = PaoxiuColors.KeepGreen,
        trackColor = PaoxiuColors.KeepGreen.copy(alpha = 0.15f),
        strokeCap = StrokeCap.Round,
        drawStopIndicator = {},
    )
}

/**
 * 首页顶部境界摘要（Keep 首页大卡风格）。
 */
@Composable
fun RealmHeroCard(
    realmLabel: String,
    spiritRoot: String?,
    fortune: String,
    streakDays: Int,
    techniqueName: String,
    xpProgress: Float,
    xpText: String?,
    modifier: Modifier = Modifier,
) {
    CultivationCard(modifier = modifier) {
        Text(
            text = realmLabel,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        spiritRoot?.let {
            Text("灵根 · $it", style = MaterialTheme.typography.bodyMedium, color = PaoxiuColors.KeepGreen)
        }
        if (streakDays > 0) {
            Text(
                "道心 · 连续 $streakDays 天",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text("今日运势 · $fortune", style = MaterialTheme.typography.bodyMedium)
        Text("功法 · $techniqueName", style = MaterialTheme.typography.bodySmall)
        if (xpText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            RealmProgressBar(progress = xpProgress)
            Text(xpText, style = MaterialTheme.typography.bodySmall)
        }
    }
}
