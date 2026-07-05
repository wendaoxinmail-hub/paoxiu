package com.wendao.run.feature.technique

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wendao.run.core.network.model.TechniqueDto
import com.wendao.run.ui.components.CultivationCard
import com.wendao.run.ui.components.PaoxiuBackground
import com.wendao.run.ui.components.PaoxiuLayout
import com.wendao.run.ui.components.PaoxiuPrimaryButton
import com.wendao.run.ui.components.RealmProgressBar
import com.wendao.run.ui.components.SectionHeader
import com.wendao.run.ui.theme.PaoxiuColors

/** 修炼法门详情：修炼方式 / 条件 / 流程 / 熟练度 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechniqueDetailScreen(
    techniqueId: String,
    onBack: () -> Unit,
    viewModel: TechniqueDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(techniqueId) { viewModel.load(techniqueId) }

    Scaffold(
        containerColor = PaoxiuColors.KeepCanvas,
        topBar = {
            TopAppBar(
                title = { Text("修炼法门", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            when {
                state.loading -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                state.technique == null -> Column(
                    modifier = Modifier.padding(padding).padding(16.dp),
                ) {
                    Text(state.error ?: "未找到功法", color = MaterialTheme.colorScheme.error)
                }
                else -> TechniqueDetailContent(
                    technique = state.technique!!,
                    message = state.message,
                    error = state.error,
                    actionInProgress = state.actionInProgress,
                    modifier = Modifier.padding(padding),
                    onComprehend = viewModel::comprehend,
                    onEquip = viewModel::equip,
                )
            }
        }
    }
}

@Composable
private fun TechniqueDetailContent(
    technique: TechniqueDto,
    message: String?,
    error: String?,
    actionInProgress: Boolean,
    modifier: Modifier = Modifier,
    onComprehend: () -> Unit,
    onEquip: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = PaoxiuLayout.ScreenHorizontalDp.dp,
                vertical = PaoxiuLayout.ScreenVerticalDp.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(PaoxiuLayout.SectionSpacingDp.dp),
    ) {
        message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        CultivationCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = technique.name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = technique.tierLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (technique.isBasic) PaoxiuColors.KeepGreen else PaoxiuColors.ImmortalGold,
                )
            }
            Text(technique.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                "解锁境界 · ${technique.unlockRealm} · 洞府 Lv.${technique.requiredGrottoLevel}",
                style = MaterialTheme.typography.bodySmall,
                color = PaoxiuColors.KeepTextSecondary,
            )
            if (technique.proficiency > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "熟练度 ${technique.proficiency} / 100",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                RealmProgressBar(progress = technique.proficiency / 100f)
            }
        }

        CultivationCard {
            SectionHeader("修炼方式")
            Text(technique.practiceMethod, style = MaterialTheme.typography.bodyLarge)
        }

        CultivationCard {
            SectionHeader("修炼条件")
            Text(technique.practiceCondition, style = MaterialTheme.typography.bodyMedium)
        }

        CultivationCard {
            SectionHeader("修炼流程")
            Text(technique.practiceFlow, style = MaterialTheme.typography.bodyMedium)
        }

        CultivationCard {
            SectionHeader("熟练度效果")
            Text(technique.masteryEffect, style = MaterialTheme.typography.bodyMedium)
        }

        CultivationCard {
            SectionHeader("收益加成")
            Text(
                "修为 ×${"%.2f".format(technique.xpMultiplier)} · 灵石 ×${"%.2f".format(technique.stoneMultiplier)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        when {
            !technique.owned && technique.canComprehend -> {
                val label = if (technique.freeComprehend) "免费领悟" else "领悟（${technique.price} 灵石）"
                PaoxiuPrimaryButton(
                    text = label,
                    onClick = onComprehend,
                    enabled = !actionInProgress,
                )
            }
            !technique.owned -> {
                Text(
                    "洞府需升至 Lv.${technique.requiredGrottoLevel} 方可领悟",
                    style = MaterialTheme.typography.bodySmall,
                    color = PaoxiuColors.KeepTextSecondary,
                )
            }
            !technique.active -> {
                PaoxiuPrimaryButton(
                    text = "装备为当前功法",
                    onClick = onEquip,
                    enabled = !actionInProgress,
                )
            }
            else -> {
                OutlinedButton(onClick = { }, enabled = false, modifier = Modifier.fillMaxWidth()) {
                    Text("当前已装备")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
