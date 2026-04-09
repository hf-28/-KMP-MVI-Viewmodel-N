package com.beno.example.urllauncher

import com.beno.MVI
import com.beno.mvi.KMMViewModel
import com.beno.mvi.mvi
import kotlinx.coroutines.launch

/**
 * Shared ViewModel (compiled for Android *and* iOS via KMM).
 *
 * Pattern:
 *   class MyViewModel : KMMViewModel(),
 *       MVI<State, Action, Effect> by mvi(State.default())
 *
 * `by mvi(...)` wires the delegate so you only implement [onAction].
 */
class UrlLauncherViewModel :
    KMMViewModel(),
    MVI<UrlLauncherContract.MyUiState,
        UrlLauncherContract.Action,
        UrlLauncherContract.SideEffect>
    by mvi(UrlLauncherContract.MyUiState.default()) {

    // ── Entry point for all user events ──────────────────────────────────────
    override fun onAction(event: UrlLauncherContract.Action) {
        when (event) {
            UrlLauncherContract.Action.ViewReady     -> onViewReady()
            UrlLauncherContract.Action.OpenUrlClicked -> onOpenUrl()
            is UrlLauncherContract.Action.UrlChanged  -> onUrlChanged(event.newUrl)
        }
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    private fun onViewReady() {
        // Simulate an async load (e.g. fetch default URL from prefs / API)
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            // … pretend we fetched something …
            updateState { copy(isLoading = false, url = "https://example.com") }
        }
    }

    private fun onOpenUrl() {
        val url = currentState.url
        if (url.isBlank()) {
            viewModelScope.launch {
                emitEffect { UrlLauncherContract.SideEffect.ShowMessage("URL is empty!") }
            }
            return
        }
        updateState { copy(openCount = openCount + 1) }
        viewModelScope.launch {
            // This is a side-effect: the platform will actually open the browser
            emitEffect { UrlLauncherContract.SideEffect.OpenUrl(url) }
        }
    }

    private fun onUrlChanged(newUrl: String) {
        updateState { copy(url = newUrl) }
    }
}
