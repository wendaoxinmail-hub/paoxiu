package com.wendao.run.feature.sect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.wendao.run.core.network.model.SectDailyTaskDto
import com.wendao.run.core.network.model.SectMemberDto
import com.wendao.run.core.network.model.SectSummaryDto
import com.wendao.run.ui.components.PaoxiuPrimaryButton
import com.wendao.run.ui.components.CultivationCard
import com.wendao.run.ui.components.PaoxiuBackground
import com.wendao.run.ui.components.SectionHeader

/** 宗门页：开宗/入宗、师徒与周榜 */
@Composable
fun SectScreen(
    viewModel: SectViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.loading && state.mySect == null && state.sects.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator(modifier = Modifier.padding(24.dp))
        }
        return
    }

    PaoxiuBackground {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("宗门", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        }
        state.message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }

        val mySect = state.mySect
        if (mySect != null) {
            item {
                CultivationCard {
                    Column(modifier = Modifier.padding(0.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(mySect.name, style = MaterialTheme.typography.titleLarge)
                        Text(mySect.description, style = MaterialTheme.typography.bodyMedium)
                        Text("弟子 ${mySect.memberCount} 人")
                        state.mentorDaoName?.let { Text("师父：$it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
            state.dailyTask?.let { task ->
                item {
                    DailyTaskCard(task, onClaim = { viewModel.claimDailyTask() })
                }
            }
            items(mySect.members, key = { it.userId }) { member ->
                MemberRow(member, onMentor = { viewModel.setMentor(member.userId) })
            }
        } else {
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("创建宗门", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = state.createName,
                            onValueChange = viewModel::updateCreateName,
                            label = { Text("宗门名") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = state.createDesc,
                            onValueChange = viewModel::updateCreateDesc,
                            label = { Text("简介") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(onClick = { viewModel.createSect() }, modifier = Modifier.fillMaxWidth()) {
                            Text("开宗立派")
                        }
                    }
                }
            }
            item { Text("可加入宗门", style = MaterialTheme.typography.titleMedium) }
            items(state.sects, key = { it.id }) { sect ->
                SectJoinCard(sect, onJoin = { viewModel.joinSect(sect.id) })
            }
        }

        item { SectionHeader("本周里程榜") }
        items(state.rankings, key = { it.rank }) { entry ->
            Text("#${entry.rank} ${entry.daoName} · ${(entry.distanceMeters / 1000).toInt()} km")
        }
    }
    }
}

@Composable
private fun DailyTaskCard(task: SectDailyTaskDto, onClaim: () -> Unit) {
    val progressKm = (task.contributionMeters / 1000).toInt()
    val targetKm = (task.targetMeters / 1000).toInt()
    CultivationCard(highlight = task.completed && !task.claimed) {
        Text("宗门日常 · 共修里程", style = MaterialTheme.typography.titleMedium)
        Text("今日贡献 $progressKm / $targetKm 公里", style = MaterialTheme.typography.bodyMedium)
        Text(
            "奖励 +${task.rewardStones} 灵石 · +${task.rewardXp} 修为",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        when {
            task.claimed -> Text("今日已领取", color = MaterialTheme.colorScheme.primary)
            task.completed -> PaoxiuPrimaryButton(text = "领取奖励", onClick = onClaim)
            else -> Text("完成修炼跑即可累积贡献", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MemberRow(member: SectMemberDto, onMentor: () -> Unit) {
    Card {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("${member.daoName} · ${member.realmLabel}")
                Text(member.role, style = MaterialTheme.typography.bodySmall)
            }
            if (member.role != "LEADER") {
                OutlinedButton(onClick = onMentor) { Text("拜师") }
            }
        }
    }
}

@Composable
private fun SectJoinCard(sect: SectSummaryDto, onJoin: () -> Unit) {
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(sect.name, style = MaterialTheme.typography.titleSmall)
            Text(sect.description, style = MaterialTheme.typography.bodySmall)
            Text("弟子 ${sect.memberCount} 人")
            Button(onClick = onJoin, modifier = Modifier.padding(top = 8.dp)) { Text("加入") }
        }
    }
}
