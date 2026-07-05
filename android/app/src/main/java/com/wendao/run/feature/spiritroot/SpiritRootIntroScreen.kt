package com.wendao.run.feature.spiritroot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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

/** 灵根测试引导页 */
@Composable
fun SpiritRootIntroScreen(
    onStartTest: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        InkWashMotionBackdrop(showTrail = false)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            InkSealLabel(text = "灵根测试")
            Spacer(modifier = Modifier.height(12.dp))
            GoldDivider(modifier = Modifier.fillMaxWidth(0.4f))
            Spacer(modifier = Modifier.height(24.dp))
            CultivationCard {
                Text(
                    text = "道友，请移动身体，运转周天，让本座看看你的灵根资质",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = PaoxiuColors.InkBlack,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "完成约 1 公里试跑，系统将依据配速波动判定灵根",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = PaoxiuColors.InkMuted,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
            PaoxiuPrimaryButton(text = "开始灵根测试", onClick = onStartTest)
        }
    }
}
