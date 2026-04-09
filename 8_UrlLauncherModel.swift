import Combine
import shared
import SwiftUI

// ─── Swift ViewModel wrapper ─────────────────────────────────────────────────

/// Wraps the shared KMM UrlLauncherViewModel.
/// @Published properties drive SwiftUI re-renders automatically.
class UrlLauncherModel: ViewModel<UrlLauncherViewModel> {

    // One-time init guard (mirrors the Kotlin hasInitialized pattern)
    private var hasInitialized = false

    // ── Observed state (mirrors KMM UiState) ─────────────────────────────────
    @Published private(set) var uiState = UrlLauncherContractMyUiState.companion.default()

    // ── Side-effect triggers ──────────────────────────────────────────────────
    @Published var urlToOpen: URL?       // set by OpenUrl effect → sheet / SFSafariVC
    @Published var toastMessage: String? // set by ShowMessage effect

    // ── Init ─────────────────────────────────────────────────────────────────
    override init(model: UrlLauncherViewModel = KoinHelper().provideUrlLauncherViewModel) {
        super.init(model: model)
        uiState = model.currentState
        setupObservers()
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private func setupObservers() {
        // State → @Published
        model.watchState(scope: model.viewModelScope) { [weak self] state in
            self?.uiState = state
        }
        .asCancellable()
        .store(in: &subscriptions)

        // Effects → @Published triggers
        model.watchEffect(scope: model.viewModelScope) { [weak self] effect in
            switch effect {
            case let open as UrlLauncherContractSideEffectOpenUrl:
                self?.urlToOpen = URL(string: open.url)

            case let msg as UrlLauncherContractSideEffectShowMessage:
                self?.toastMessage = msg.text

            default:
                break
            }
        }
        .asCancellable()
        .store(in: &subscriptions)
    }

    // ── Public API called by the view ─────────────────────────────────────────

    /// Call once from onFirstAppear.
    func dispatchInitialEventIfNeeded() {
        guard !hasInitialized else { return }
        hasInitialized = true
        model.onAction(event: UrlLauncherContractActionViewReady())
    }

    func emit(_ action: UrlLauncherContractAction) {
        model.onAction(event: action)
    }
}
