package com.wendao.run.feature.sect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wendao.run.core.data.AuthRepository
import com.wendao.run.core.data.GameRepository
import com.wendao.run.core.network.model.RankingEntryDto
import com.wendao.run.core.network.model.SectDailyTaskDto
import com.wendao.run.core.network.model.SectDetailDto
import com.wendao.run.core.network.model.SectSummaryDto
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SectUiState(
    val loading: Boolean = true,
    val mySect: SectDetailDto? = null,
    val sects: List<SectSummaryDto> = emptyList(),
    val rankings: List<RankingEntryDto> = emptyList(),
    val createName: String = "",
    val createDesc: String = "",
    val message: String? = null,
    val error: String? = null,
    val mentorDaoName: String? = null,
    val dailyTask: SectDailyTaskDto? = null,
)

@HiltViewModel
class SectViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SectUiState())
    val uiState: StateFlow<SectUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            val mySect = gameRepository.mySect().getOrNull()
            val sects = gameRepository.sects().getOrElse { emptyList() }
            val rankings = gameRepository.weeklyRanking().getOrElse { emptyList() }
            val dailyTask = if (mySect != null) {
                gameRepository.dailySectTask().getOrNull()
            } else {
                null
            }
            authRepository.refreshProfile()
            _uiState.value = _uiState.value.copy(
                loading = false,
                mySect = mySect,
                sects = sects,
                rankings = rankings,
                mentorDaoName = authRepository.profile.value?.mentorDaoName,
                dailyTask = dailyTask,
            )
        }
    }

    fun updateCreateName(value: String) {
        _uiState.value = _uiState.value.copy(createName = value)
    }

    fun updateCreateDesc(value: String) {
        _uiState.value = _uiState.value.copy(createDesc = value)
    }

    fun createSect() {
        val name = _uiState.value.createName.trim()
        val desc = _uiState.value.createDesc.trim()
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入宗门名称")
            return
        }
        viewModelScope.launch {
            gameRepository.createSect(name, desc.ifBlank { "云游散修，共修大道" })
                .onSuccess {
                    refresh()
                    _uiState.value = _uiState.value.copy(message = "宗门创建成功")
                }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message) }
        }
    }

    fun joinSect(id: Long) {
        viewModelScope.launch {
            gameRepository.joinSect(id)
                .onSuccess {
                    refresh()
                    _uiState.value = _uiState.value.copy(message = "已加入宗门")
                }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message) }
        }
    }

    fun setMentor(userId: Long) {
        viewModelScope.launch {
            gameRepository.setMentor(userId)
                .onSuccess {
                    refresh()
                    _uiState.value = _uiState.value.copy(message = "拜师成功")
                }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message) }
        }
    }

    fun claimDailyTask() {
        viewModelScope.launch {
            gameRepository.claimDailySectTask()
                .onSuccess { task ->
                    refresh()
                    _uiState.value = _uiState.value.copy(message = task.message ?: "宗门任务奖励已领取")
                }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message) }
        }
    }
}
