package com.wendao.run.feature.grotto

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wendao.run.core.network.model.EquipmentDto
import com.wendao.run.core.network.model.TechniqueDto
import com.wendao.run.ui.components.CatalogActionRow
import com.wendao.run.ui.components.CultivationCard
import com.wendao.run.core.run.TrackEffectResolver
import com.wendao.run.ui.components.TrackEffectPreview
import com.wendao.run.ui.components.PaoxiuBackground
import com.wendao.run.ui.components.PaoxiuLayout
import com.wendao.run.ui.components.PaoxiuPrimaryButton
import com.wendao.run.ui.components.SectionHeader
import com.wendao.run.ui.theme.PaoxiuColors

/** 洞府页：灵田、闭关、藏经阁与装备库 */
@Composable
fun GrottoScreen(
    onOpenTechniqueDetail: (String) -> Unit = {},
    viewModel: GrottoViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.loading && state.grotto == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.padding(24.dp))
        }
        return
    }

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
            state.message?.let { msg ->
                item { Text(text = msg, color = MaterialTheme.colorScheme.primary) }
            }
            state.error?.let { err ->
                item { Text(text = err, color = MaterialTheme.colorScheme.error) }
            }
            item {
                val grotto = state.grotto
                val title = grotto?.grottoName?.takeIf { it.isNotBlank() } ?: "洞府"
                Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            }
            item {
                val grotto = state.grotto
                CultivationCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "灵田每 4 小时可收获 · 闭关即时获得修为 · 升级解锁更多功法",
                        style = MaterialTheme.typography.bodySmall,
                        color = PaoxiuColors.KeepTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.grottoNameInput,
                        onValueChange = viewModel::updateGrottoName,
                        label = { Text("洞府名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    PaoxiuPrimaryButton(text = "保存洞府名", onClick = { viewModel.renameGrotto() })
                    Text("洞府等级：${grotto?.grottoLevel ?: 1}", style = MaterialTheme.typography.titleMedium)
                    Text("灵田：${if (grotto?.canHarvest == true) "可收获" else "生长中"}")
                    Text("灵兽：${grotto?.spiritBeastName ?: "尚无"} Lv.${grotto?.spiritBeastLevel ?: 0}")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.harvest() },
                            enabled = grotto?.canHarvest == true,
                            modifier = Modifier.weight(1f),
                        ) { Text("收获灵田") }
                        OutlinedButton(
                            onClick = { viewModel.retreat() },
                            modifier = Modifier.weight(1f),
                        ) { Text("闭关") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.upgrade() },
                            modifier = Modifier.weight(1f),
                        ) { Text("升级 (${grotto?.upgradeCost ?: 0}石)") }
                        if ((grotto?.spiritBeastLevel ?: 0) > 0) {
                            OutlinedButton(
                                onClick = { viewModel.feedBeast() },
                                modifier = Modifier.weight(1f),
                            ) { Text("喂养灵兽") }
                        }
                    }
                }
            }

            item {
                SectionHeader("藏经阁 · 功法")
                Text(
                    "基础功法满足洞府等级后可免费领悟；点击查看具体修炼法门",
                    style = MaterialTheme.typography.bodySmall,
                    color = PaoxiuColors.KeepTextSecondary,
                )
            }
            items(state.techniques, key = { it.id }) { technique ->
                TechniqueCard(
                    technique = technique,
                    modifier = Modifier.fillMaxWidth(),
                    onBuy = { viewModel.buyTechnique(it) },
                    onEquip = { viewModel.equipTechnique(it) },
                    onViewDetail = { onOpenTechniqueDetail(technique.id) },
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionHeader("装备库")
            }
            items(state.equipment, key = { it.id }) { item ->
                EquipmentCard(
                    item = item,
                    spiritRoot = state.spiritRoot,
                    modifier = Modifier.fillMaxWidth(),
                    onBuy = { viewModel.buyEquipment(it) },
                    onEquip = { viewModel.equipEquipment(it) },
                )
            }
        }
    }
}

@Composable
private fun TechniqueCard(
    technique: TechniqueDto,
    modifier: Modifier = Modifier,
    onBuy: (String) -> Unit,
    onEquip: (String) -> Unit,
    onViewDetail: (String) -> Unit,
) {
    CultivationCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(technique.name, style = MaterialTheme.typography.titleMedium)
            Text(
                technique.tierLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (technique.isBasic) PaoxiuColors.KeepGreen else PaoxiuColors.ImmortalGold,
            )
        }
        Text(technique.description, style = MaterialTheme.typography.bodyMedium)
        Text(
            technique.practiceMethod,
            style = MaterialTheme.typography.bodySmall,
            color = PaoxiuColors.KeepTextSecondary,
        )
        val priceText = when {
            technique.owned -> null
            technique.freeComprehend -> "免费领悟 · 洞府 Lv.${technique.requiredGrottoLevel}"
            else -> "领悟 ${technique.price} 石 · 洞府 Lv.${technique.requiredGrottoLevel}"
        }
        priceText?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
        if (technique.proficiency > 0) {
            Text(
                "熟练度 ${technique.proficiency}/100",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { onViewDetail(technique.id) },
                modifier = Modifier.weight(1f),
            ) { Text("查看法门") }
            when {
                !technique.owned && technique.canComprehend -> {
                    OutlinedButton(
                        onClick = { onBuy(technique.id) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (technique.freeComprehend) "领悟" else "兑换")
                    }
                }
                !technique.owned -> Unit
                !technique.active -> {
                    OutlinedButton(
                        onClick = { onEquip(technique.id) },
                        modifier = Modifier.weight(1f),
                    ) { Text("装备") }
                }
                else -> {
                    Text(
                        "已装备",
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun EquipmentCard(
    item: EquipmentDto,
    spiritRoot: String?,
    modifier: Modifier = Modifier,
    onBuy: (String) -> Unit,
    onEquip: (String) -> Unit,
) {
    val previewEffect = TrackEffectResolver.previewForEquipment(item.id, spiritRoot)
    CultivationCard(modifier = modifier) {
        Text("${item.name}（${item.slot}）", style = MaterialTheme.typography.titleMedium)
        Text(item.description, style = MaterialTheme.typography.bodyMedium)
        TrackEffectPreview(effect = previewEffect)
        Text("价格 ${item.price} 石 · 洞府 Lv.${item.requiredGrottoLevel}", style = MaterialTheme.typography.bodySmall)
        when {
            !item.owned -> CatalogActionRow(
                actionLabel = "购买",
                onAction = { onBuy(item.id) },
            )
            !item.equipped -> CatalogActionRow(
                actionLabel = "穿戴",
                onAction = { onEquip(item.id) },
            )
            else -> CatalogActionRow(statusLabel = "已穿戴")
        }
    }
}
