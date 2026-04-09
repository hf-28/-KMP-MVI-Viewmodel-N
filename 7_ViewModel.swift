import Combine
import shared   // the KMM XCFramework

// ─── Base wrapper ────────────────────────────────────────────────────────────

/// Generic ObservableObject that wraps any KMMViewModel.
/// Subclass this in each feature; it handles the lifecycle (dispose on deinit).
class ViewModel<T: KMMViewModel>: ObservableObject {
    let model: T
    var subscriptions: Set<AnyCancellable> = []

    init(model: T) {
        self.model = model
    }

    deinit {
        model.dispose()   // cancels viewModelScope on the Kotlin side
    }
}

// ─── Combine bridge ──────────────────────────────────────────────────────────

extension DisposableHandle {
    /// Wrap a Kotlin DisposableHandle as an AnyCancellable so it plays nicely
    /// with Combine's store(in:) / Set<AnyCancellable>.
    func asCancellable() -> AnyCancellable {
        AnyCancellable { self.dispose() }
    }
}
