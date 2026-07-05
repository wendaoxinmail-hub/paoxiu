package com.wendao.run.feature.spiritroot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wendao.run.core.util.ShareUtils

@Composable
fun SpiritRootResultScreen(
    runId: String,
    onEnterCultivation: () -> Unit,
    viewModel: SpiritRootResultViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(runId) {
        viewModel.detect(runId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "灵根显现",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        if (state.loading) {
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator()
            Text(
                text = "灵气汇聚中…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (state.error != null) {
            Text(
                text = state.error!!,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Button(onClick = { viewModel.detect(runId) }) {
                Text("重试")
            }
        } else {
            val result = state.result!!
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = result.spiritRootLabel,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = result.description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "境界：${result.realmLabel}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "道号：${result.daoName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            OutlinedButton(
                onClick = {
                    ShareUtils.shareText(
                        context,
                        "分享灵根",
                        "【跑修】我觉醒了${result.spiritRootLabel}！${result.description}",
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("分享灵根")
            }
            Button(
                onClick = onEnterCultivation,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("踏入修仙之路")
            }
        }
    }
}
