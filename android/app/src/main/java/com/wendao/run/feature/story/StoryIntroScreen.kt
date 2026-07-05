package com.wendao.run.feature.story

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wendao.run.ui.components.CultivationCard
import com.wendao.run.ui.components.GoldDivider
import com.wendao.run.ui.components.PaoxiuPrimaryButton
import com.wendao.run.ui.splash.InkSealLabel
import com.wendao.run.ui.splash.InkWashMotionBackdrop
import com.wendao.run.ui.theme.PaoxiuColors

/**
 * 韩天尊遗训开场（可跳过），首次入道展示。
 */
@Composable
fun StoryIntroScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    viewModel: StoryIntroViewModel = hiltViewModel(),
) {
    fun finish(skip: Boolean) {
        viewModel.markSeen()
        if (skip) onSkip() else onContinue()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        InkWashMotionBackdrop(showTrail = false)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            InkSealLabel(text = "跑修纪元")
            Spacer(modifier = Modifier.height(12.dp))
            GoldDivider(modifier = Modifier.fillMaxWidth(0.5f))
            Spacer(modifier = Modifier.height(24.dp))
            CultivationCard {
                Text(
                    "灵气虽竭，步灵可生。",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = PaoxiuColors.InkBlack,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "以足为剑，以汗为丹，以路为阵。凡能以双脚丈量大地者，可重开仙界之门。",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = PaoxiuColors.InkBlack.copy(alpha = 0.78f),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "—— 韩天尊遗训",
                style = MaterialTheme.typography.bodySmall,
                color = PaoxiuColors.InkMuted,
            )
            Spacer(modifier = Modifier.height(40.dp))
            PaoxiuPrimaryButton(text = "感悟遗训，测灵根", onClick = { finish(skip = false) })
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = { finish(skip = true) }, modifier = Modifier.fillMaxWidth()) {
                Text("跳过")
            }
        }
    }
}
