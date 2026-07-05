package com.wendao.run.feature.cultivate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wendao.run.core.data.AuthRepository
import com.wendao.run.core.data.GameRepository
import com.wendao.run.core.database.entity.RunRecordEntity
import com.wendao.run.core.network.model.TechniqueDto
import com.wendao.run.core.network.model.UserProfileDto
import com.wendao.run.core.run.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 修炼首页 ViewModel：聚合用户档案、功法与近期跑步记录。
 */
@HiltViewModel
class CultivateViewModel @Inject constructor(
    private val runRepository: RunRepository,
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository,
) : ViewModel() {

    val recentRuns: StateFlow<List<RunRecordEntity>> = runRepository
        .observeRecentFinished(limit = 5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val profile: StateFlow<UserProfileDto?> = authRepository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val dailyFortune: String = FORTUNES[LocalDate.now().dayOfYear % FORTUNES.size]

    private val _techniques = kotlinx.coroutines.flow.MutableStateFlow<List<TechniqueDto>>(emptyList())
    val techniques: StateFlow<List<TechniqueDto>> = _techniques
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            runRepository.syncPendingRuns()
            authRepository.refreshProfile()
            gameRepository.techniques().onSuccess { _techniques.value = it }
        }
    }

    companion object {
        private val FORTUNES = listOf(
            "灵气充沛，宜慢跑固本",
            "东方有霞，宜稳配速修行",
            "今日忌贪快，守中为佳",
            "云气汇聚，长跑可得奇遇",
            "心若止水，步法自精",
        )
    }
}
