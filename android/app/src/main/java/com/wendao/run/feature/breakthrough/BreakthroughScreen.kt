package com.wendao.run.feature.breakthrough

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wendao.run.ui.components.CultivationCard
import com.wendao.run.ui.components.PaoxiuBackground
import com.wendao.run.ui.components.PaoxiuPrimaryButton
import com.wendao.run.ui.theme.PaoxiuColors

/**
 * 境界突破（渡劫）结算页：突破成功后的沉浸式反馈。
 */
@Composable
fun BreakthroughScreen(
    runId: String,
    onDone: () -> Unit,
    viewModel: BreakthroughViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(runId) {
        viewModel.load(runId)
    }

    PaoxiuBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("渡劫成功", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(24.dp))
            CultivationCard(highlight = true) {
                Text(
                    state.realmLabel ?: "境界提升",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "天雷已过，丹田稳固。道友修行更进一层。",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                state.adventureTitle?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("途中奇遇 · $it", style = MaterialTheme.typography.bodySmall, color = PaoxiuColors.SpiritJade)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            PaoxiuPrimaryButton(text = "继续修炼", onClick = onDone)
        }
    }
}
