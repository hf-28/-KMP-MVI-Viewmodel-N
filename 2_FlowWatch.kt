package com.beno

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Collect a [Flow] inside [scope] and call [onNext] on each emission.
 * Returns a [DisposableHandle] that cancels the collection job when disposed.
 * This is the bridge that makes Kotlin Flows observable from Swift without
 * exposing coroutine internals.
 */
fun <T> Flow<T>.watch(
    scope: CoroutineScope,
    onNext: (T) -> Unit,
): DisposableHandle {
    val job = scope.launch(Dispatchers.Main) {
        collect { onNext(it) }
    }
    return DisposableHandle { job.cancel() }
}
