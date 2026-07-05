package com.wendao.run.core.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object WeChatAuthResultBus {
    private val _results = MutableSharedFlow<WeChatAuthResult>(extraBufferCapacity = 1)
    val results: SharedFlow<WeChatAuthResult> = _results.asSharedFlow()

    fun publishSuccess(code: String) {
        _results.tryEmit(WeChatAuthResult.Success(code))
    }

    fun publishError(message: String) {
        _results.tryEmit(WeChatAuthResult.Error(message))
    }
}

sealed interface WeChatAuthResult {
    data class Success(val code: String) : WeChatAuthResult
    data class Error(val message: String) : WeChatAuthResult
}
