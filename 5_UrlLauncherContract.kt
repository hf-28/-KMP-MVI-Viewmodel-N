package com.beno.example.urllauncher

import com.beno.mvi.UiEvent
import com.beno.mvi.UiSideEffect
import com.beno.mvi.UiState

/**
 * MVI contract for the URL-launcher example.
 *
 * ┌──────────────┐   Action   ┌─────────────┐  SideEffect  ┌──────────────┐
 * │  SwiftUI /   │ ─────────► │  ViewModel  │ ────────────► │  SwiftUI /   │
 * │  Compose UI  │            │  (shared)   │              │  Compose UI  │
 * │              │ ◄───────── │             │              │              │
 * └──────────────┘   State    └─────────────┘              └──────────────┘
 */
interface UrlLauncherContract {

    // ── State ─────────────────────────────────────────────────────────────────
    data class MyUiState(
        val isLoading: Boolean = false,
        val url: String = "https://example.com",
        val openCount: Int = 0,
    ) : UiState {
        companion object {
            fun default() = MyUiState()
        }
    }

    // ── Actions (user intents) ────────────────────────────────────────────────
    sealed class Action : UiEvent() {
        /** Screen is ready – load initial data. */
        data object ViewReady : Action()

        /** User tapped the "Open URL" button. */
        data object OpenUrlClicked : Action()

        /** User typed a new URL in the text field. */
        data class UrlChanged(val newUrl: String) : Action()
    }

    // ── Side-effects (one-shot events) ────────────────────────────────────────
    sealed class SideEffect : UiSideEffect {
        /** Tell the platform to open this URL in a browser. */
        data class OpenUrl(val url: String) : SideEffect()

        /** Show a toast / snack-bar message. */
        data class ShowMessage(val text: String) : SideEffect()
    }
}
