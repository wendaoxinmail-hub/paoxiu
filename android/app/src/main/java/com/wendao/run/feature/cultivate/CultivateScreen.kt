package com.wendao.run.feature.cultivate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wendao.run.feature.run.summaryDistance
import com.wendao.run.feature.run.syncStatusLabel
import com.wendao.run.feature.run.summaryDuration
import com.wendao.run.feature.run.summaryPace
import com.wendao.run.ui.components.CultivationCard
import androidx.compose.foundation.clickable
import com.wendao.run.ui.components.PaoxiuBackground
import com.wendao.run.ui.components.PaoxiuLayout
import com.wendao.run.ui.components.PaoxiuPrimaryButton
import com.wendao.run.ui.components.RealmHeroCard
import com.wendao.run.ui.components.SectionHeader
import com.wendao.run.ui.theme.PaoxiuColors

/**
 * 修炼首页：Keep 式「大卡 + 主按钮 + 历史列表」，国风境界展示。
 */
@Composable
fun CultivateScreen(
    onStartRun: () -> Unit,
    onOpenRunDetail: (String) -> Unit = {},
    onOpenRunHistory: () -> Unit = {},
    onOpenTechniqueDetail: (String) -> Unit = {},
    viewModel: CultivateViewModel = hiltViewModel(),
) {
    val recentRuns by viewModel.recentRuns.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val techniques by viewModel.techniques.collectAsStateWithLifecycle()
    val activeTechnique = techniques.find { it.id == (profile?.activeTechniqueId ?: "basic_step") }
    val techniqueName = activeTechnique?.name ?: "基础步法"

    val xpProgress = profile?.let { p ->
        if (p.xpToNext > 0 && p.realmXp > 0) {
            (p.realmXp.toFloat() / (p.realmXp + p.xpToNext)).coerceIn(0f, 1f)
        } else 0f
    } ?: 0f
    val xpText = profile?.takeIf { it.xpToNext > 0 }?.let { "修为 ${it.realmXp} · 距下一境 ${it.xpToNext}" }

    PaoxiuBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = PaoxiuLayout.ScreenHorizontalDp.dp,
                    vertical = PaoxiuLayout.ScreenVerticalDp.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(PaoxiuLayout.SectionSpacingDp.dp),
        ) {
            item {
                Text(
                    text = "修炼",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            item {
                RealmHeroCard(
                    realmLabel = profile?.realmLabel ?: "凡人·未入门",
                    spiritRoot = profile?.spiritRootLabel,
                    fortune = viewModel.dailyFortune,
                    streakDays = profile?.runStreakDays ?: 0,
                    techniqueName = buildString {
                        append(techniqueName)
                        activeTechnique?.takeIf { it.proficiency > 0 }?.let { append(" · 熟练 Lv.${it.proficiency}") }
                    },
                    xpProgress = xpProgress,
                    xpText = xpText,
                )
            }
            item {
                PaoxiuPrimaryButton(text = "开始修炼", onClick = onStartRun)
            }
            activeTechnique?.let { technique ->
                item {
                    CultivationCard(
                        modifier = Modifier.clickable {
                            onOpenTechniqueDetail(technique.id)
                        },
                    ) {
                        Text("当前功法", style = MaterialTheme.typography.labelMedium, color = PaoxiuColors.KeepTextSecondary)
                        Text(technique.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            technique.practiceMethod,
                            style = MaterialTheme.typography.bodySmall,
                            color = PaoxiuColors.KeepTextSecondary,
                        )
                        Text(
                            "查看修炼法门 →",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            if (recentRuns.isNotEmpty()) {
                item {
                    SectionHeader("近期修炼")
                    Text(
                        "查看全部记录",
                        modifier = Modifier.clickable(onClick = onOpenRunHistory),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                items(recentRuns, key = { it.id }) { run ->
                    CultivationCard(modifier = Modifier.clickable { onOpenRunDetail(run.id) }) {
                        Text(
                            "${run.summaryDistance()} · ${run.summaryDuration()}",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            "配速 ${run.summaryPace()} · ${run.syncStatusLabel()}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (run.rewardStones != null && run.rewardStones!! > 0) {
                            Text(
                                "+${run.rewardStones} 灵石 · +${run.rewardXp ?: 0} 修为",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        run.adventureTitle?.let {
                            Text("奇遇 · $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}
