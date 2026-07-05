package com.wendao.run.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wendao.run.core.run.TrackEffectResolver
import com.wendao.run.ui.components.CultivationCard
import com.wendao.run.ui.components.KeepStatBlock
import com.wendao.run.ui.components.PaoxiuBackground
import com.wendao.run.ui.components.PaoxiuLayout
import com.wendao.run.ui.components.PaoxiuPrimaryButton
import com.wendao.run.ui.components.SectionHeader
import com.wendao.run.ui.components.TrackEffectPreview

@Composable
fun ProfileScreen(
    onOpenMarket: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onOpenRunHistory: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val techniques by viewModel.techniques.collectAsStateWithLifecycle()
    val equipment by viewModel.equipment.collectAsStateWithLifecycle()
    val trackEffect = remember(profile) {
        TrackEffectResolver.resolve(
            spiritRoot = profile?.spiritRoot,
            weaponId = profile?.equippedWeaponId,
            armorId = profile?.equippedArmorId,
            accessoryId = profile?.equippedAccessoryId,
        )
    }

    PaoxiuBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = PaoxiuLayout.ScreenHorizontalDp.dp,
                    vertical = PaoxiuLayout.ScreenVerticalDp.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(PaoxiuLayout.SectionSpacingDp.dp),
        ) {
            Text("我", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            CultivationCard {
                Text(
                    profile?.daoName ?: "—",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text("境界 · ${profile?.realmLabel ?: "—"}", style = MaterialTheme.typography.bodyLarge)
                profile?.grottoName?.let { Text("洞府 · $it", style = MaterialTheme.typography.bodyMedium) }
                profile?.mentorDaoName?.let { Text("师父 · $it", style = MaterialTheme.typography.bodyMedium) }
                profile?.spiritRootLabel?.let { Text("灵根 · $it", style = MaterialTheme.typography.bodyMedium) }
                profile?.sectId?.let { Text("宗门 · #$it", style = MaterialTheme.typography.bodySmall) }
            }
            if (profile?.spiritRootTestCompleted == true) {
                CultivationCard {
                    SectionHeader("轨迹显化")
                    Text(
                        "灵根与装备叠加决定跑步轨迹特效",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TrackEffectPreview(effect = trackEffect)
                }
            }
            CultivationCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    KeepStatBlock("灵石", "${profile?.spiritStones ?: 0}", accent = true)
                    KeepStatBlock("修为", "${profile?.realmXp ?: 0}")
                    KeepStatBlock("道心", "${profile?.runStreakDays ?: 0}天")
                }
            }
            SectionHeader("个人")
            CultivationCard {
                Text("编辑道号与资料", modifier = Modifier.fillMaxWidth().clickable(onClick = onEditProfile))
                Text("修炼记录与轨迹", modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenRunHistory))
            }
            SectionHeader("行囊")
            CultivationCard {
                Text("功法 ${techniques.count { it.owned }} / ${techniques.size}", style = MaterialTheme.typography.bodyMedium)
                Text("装备 ${equipment.count { it.owned }} / ${equipment.size}", style = MaterialTheme.typography.bodyMedium)
                Text("洞府 Lv.${profile?.grottoLevel ?: 1}", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onOpenMarket, modifier = Modifier.fillMaxWidth()) { Text("灵石商城") }
            Text(
                "气机推演：未连接心脉仪时以步罡配速估算，仅供参考",
                style = MaterialTheme.typography.bodySmall,
            )
            PaoxiuPrimaryButton(
                text = "退出登录",
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
