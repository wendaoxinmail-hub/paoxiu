package com.wendao.run.feature.run

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wendao.run.ui.components.CultivationCard
import com.wendao.run.ui.components.PaoxiuBackground
import com.wendao.run.ui.components.PaoxiuLayout
import com.wendao.run.ui.components.SectionHeader

/** 全部修炼记录 */
@Composable
fun RunHistoryScreen(
    onBack: () -> Unit,
    onOpenRun: (String) -> Unit,
    viewModel: RunHistoryViewModel = hiltViewModel(),
) {
    val runs by viewModel.allRuns.collectAsStateWithLifecycle()

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
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回") }
                Text("修炼记录", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (runs.isEmpty()) {
                item { Text("暂无记录，去修炼页开始第一次跑步吧") }
            }
            items(runs, key = { it.id }) { run ->
                CultivationCard(
                    modifier = Modifier.clickable { onOpenRun(run.id) },
                ) {
                    Text("${run.summaryDistance()} · ${run.summaryDuration()}", style = MaterialTheme.typography.titleMedium)
                    Text("配速 ${run.summaryPace()} · ${run.syncStatusLabel()}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
