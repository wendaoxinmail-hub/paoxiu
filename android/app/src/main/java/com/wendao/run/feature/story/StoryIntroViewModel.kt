package com.wendao.run.feature.story

import androidx.lifecycle.ViewModel
import com.wendao.run.core.data.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StoryIntroViewModel @Inject constructor(
    private val sessionStore: SessionStore,
) : ViewModel() {

    fun markSeen() = sessionStore.setStoryIntroSeen()
}
