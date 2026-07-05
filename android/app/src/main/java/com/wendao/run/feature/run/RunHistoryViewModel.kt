package com.wendao.run.feature.run

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wendao.run.core.database.entity.RunRecordEntity
import com.wendao.run.core.run.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class RunHistoryViewModel @Inject constructor(
    runRepository: RunRepository,
) : ViewModel() {

    val allRuns: StateFlow<List<RunRecordEntity>> = runRepository.observeAllFinished()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
