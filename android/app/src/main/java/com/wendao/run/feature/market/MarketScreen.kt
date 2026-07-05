package com.wendao.run.feature.market

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wendao.run.core.network.model.MarketItemDto
import com.wendao.run.ui.components.CultivationCard
import com.wendao.run.ui.components.PaoxiuBackground
import com.wendao.run.ui.components.SectionHeader

/** 灵石商城：消耗灵石兑换道具（无真实支付） */
@Composable
fun MarketScreen(
    onBack: () -> Unit,
    viewModel: MarketViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PaoxiuBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("灵石商城", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    OutlinedButton(onClick = onBack) { Text("返回") }
                }
                Text("灵石 ${state.spiritStones}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
            state.message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
            state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (state.loading) {
                item { CircularProgressIndicator() }
            }
            item { SectionHeader("道具") }
            items(state.items, key = { it.id }) { marketItem ->
                MarketItemCard(marketItem, onBuy = { viewModel.buy(marketItem.id) })
            }
        }
    }
}

@Composable
private fun MarketItemCard(item: MarketItemDto, onBuy: () -> Unit) {
    CultivationCard {
        Text(item.name, style = MaterialTheme.typography.titleMedium)
        Text(item.description, style = MaterialTheme.typography.bodySmall)
        Text("${item.price} 灵石", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        if (item.type == "consumable" && item.inventoryCount > 0) {
            Text("库存 ${item.inventoryCount} 颗", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(4.dp))
        when {
            item.type == "cosmetic" && item.owned -> Text("已拥有", color = MaterialTheme.colorScheme.primary)
            else -> Button(onClick = onBuy, enabled = !item.owned || item.type == "consumable") {
                Text(if (item.type == "consumable") "购买" else if (item.owned) "已拥有" else "兑换")
            }
        }
    }
}
