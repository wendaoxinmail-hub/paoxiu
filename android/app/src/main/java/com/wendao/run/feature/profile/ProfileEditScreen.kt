package com.wendao.run.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.wendao.run.ui.components.CultivationCard
import com.wendao.run.ui.components.PaoxiuBackground
import com.wendao.run.ui.components.PaoxiuLayout
import com.wendao.run.ui.components.PaoxiuPrimaryButton

/** 编辑道号等个人信息 */
@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    viewModel: ProfileEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PaoxiuBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PaoxiuLayout.ScreenHorizontalDp.dp, vertical = PaoxiuLayout.ScreenVerticalDp.dp),
            verticalArrangement = Arrangement.spacedBy(PaoxiuLayout.SectionSpacingDp.dp),
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回") }
            Text("编辑资料", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            CultivationCard {
                OutlinedTextField(
                    value = state.daoName,
                    onValueChange = viewModel::updateDaoName,
                    label = { Text("道号") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Text("最多 16 字", style = MaterialTheme.typography.bodySmall)
            }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            PaoxiuPrimaryButton(text = "保存", onClick = { viewModel.save(onBack) }, enabled = !state.saving)
        }
    }
}
