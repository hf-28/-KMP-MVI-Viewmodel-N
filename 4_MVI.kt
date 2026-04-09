package com.beno

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

// ─── Factory ────────────────────────────────────────────────────────────────

fun <UiState, UiAction, UiEffect> mvi(
    initialState: UiState,
): MVI<UiState, UiAction, UiEffect> = MVIDelegate(initialState)

// ─── Interface ───────────────────────────────────────────────────────────────

interface MVI<UiState, UiAction, UiEffect> {
    val uiState: StateFlow<UiState>
    val currentState: UiState
    val sideEffects: Flow<UiEffect>

    fun onAction(event: UiAction)
    fun updateState(block: UiState.() -> UiState)
    suspend fun emitEffect(effect: () -> UiEffect)

    // iOS-friendly observation methods (no coroutine knowledge needed in Swift)
    fun watchState(scope: CoroutineScope, onNext: (UiState) -> Unit): DisposableHandle
    fun watchEffect(scope: CoroutineScope, onNext: (UiEffect) -> Unit): DisposableHandle
}

// ─── Delegate (implementation via by-delegation) ─────────────────────────────

class MVIDelegate<UiState, UiAction, UiEffect>(
    initialState: UiState,
) : MVI<UiState, UiAction, UiEffect> {

    private val _uiState = MutableStateFlow(initialState)
    override val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    override val currentState: UiState get() = uiState.value

    private val _sideEffects = Channel<UiEffect>()
    override val sideEffects: Flow<UiEffect> = _sideEffects.receiveAsFlow()

    override fun onAction(event: UiAction) = Unit   // overridden by ViewModel

    override fun updateState(block: UiState.() -> UiState) = _uiState.update(block)

    override suspend fun emitEffect(effect: () -> UiEffect) = _sideEffects.send(effect())

    override fun watchState(scope: CoroutineScope, onNext: (UiState) -> Unit) =
        uiState.watch(scope, onNext)

    override fun watchEffect(scope: CoroutineScope, onNext: (UiEffect) -> Unit) =
        sideEffects.watch(scope, onNext)
}
