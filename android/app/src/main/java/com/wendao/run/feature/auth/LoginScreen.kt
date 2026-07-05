package com.wendao.run.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.wendao.run.core.network.model.UserDto
import com.wendao.run.ui.components.CultivationCard
import com.wendao.run.ui.components.GoldDivider
import com.wendao.run.ui.components.PaoxiuBackground
import com.wendao.run.ui.components.PaoxiuPrimaryButton
import com.wendao.run.ui.theme.PaoxiuColors

/**
 * 入道页：韩天尊遗训意象 + Keep 式单一主行动（游客入道）。
 */
@Composable
fun LoginScreen(
    onGuestLoggedIn: (UserDto, needsStoryIntro: Boolean) -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.loggedIn, state.user) {
        if (state.loggedIn) {
            val user = state.user ?: UserDto(
                id = 0,
                daoName = "道友",
                spiritStones = 0,
                realmLabel = "凡人·未入门",
                spiritRootTestCompleted = false,
            )
            onGuestLoggedIn(user, viewModel.needsStoryIntro())
        }
    }

    PaoxiuBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "跑修",
                style = MaterialTheme.typography.displayLarge,
            )
            Spacer(modifier = Modifier.height(12.dp))
            GoldDivider(modifier = Modifier.fillMaxWidth(0.4f))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "以足为剑，以汗为丹，以路为阵",
                style = MaterialTheme.typography.bodyLarge,
                color = PaoxiuColors.InkMuted,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "灵气虽竭，步灵可生",
                style = MaterialTheme.typography.bodyMedium,
                color = PaoxiuColors.SpiritJade.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(48.dp))

            if (state.loading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                PaoxiuPrimaryButton(text = "游客入道", onClick = viewModel::loginAsGuest)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = viewModel::loginWithWeChat,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading,
                ) {
                    Text(
                        if (state.weChatConfigured) "微信入道" else "微信入道（需配置 AppID）",
                    )
                }
            }

            state.error?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(32.dp))
            CultivationCard {
                Text(
                    "注册即赠 5000 灵石",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text("本期无充值 · 以修证道", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
