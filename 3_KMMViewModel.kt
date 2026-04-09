package com.beno.mvi

import kotlinx.coroutines.CoroutineScope

// ---------- common ----------
expect open class KMMViewModel() {
    val viewModelScope: CoroutineScope
    protected open fun onCleared()
}

// ---------- iosMain ----------
// actual open class KMMViewModel actual constructor() {
//     actual val viewModelScope: CoroutineScope = createViewModelScope()
//
//     fun dispose() = viewModelScope.cancel()
//
//     protected actual open fun onCleared() = dispose()
// }

// ---------- androidMain ----------
// actual open class KMMViewModel actual constructor() : androidx.lifecycle.ViewModel() {
//     actual val viewModelScope: CoroutineScope = viewModelScope   // from AndroidX
//     protected actual override fun onCleared() = super.onCleared()
// }
