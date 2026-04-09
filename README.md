# KMM · MVI · SwiftUI + Compose

A minimal, production-ready example of sharing a ViewModel between **Android (Jetpack Compose)** and **iOS (SwiftUI)** using Kotlin Multiplatform Mobile and the MVI pattern.

---

## The Idea

Write your ViewModel logic **once in Kotlin**. Both platforms consume the same state, dispatch the same actions, and react to the same side effects — with zero duplication.

```
┌─────────────────────────────────┐
│         Shared KMM Module       │
│                                 │
│  Contract (State/Action/Effect) │
│            ↓                    │
│       KMMViewModel              │
│    (MVI delegate via `by mvi`)  │
└────────────┬────────────────────┘
             │
     ┌───────┴────────┐
     ▼                ▼
 Android (Compose)  iOS (SwiftUI)
 vm.use { effect }  UrlLauncherModel
```

---

## Architecture

### 1. `KMMViewModel` — shared base class

Compiles to `androidx.lifecycle.ViewModel` on Android and a custom lifecycle class on iOS. Exposes `viewModelScope` on both platforms.

### 2. `MVI<State, Event, Effect>` — interface + delegate

```kotlin
interface MVI<State, Event, Effect> {
    val uiState: StateFlow<State>
    val sideEffects: Flow<Effect>
    fun onAction(event: Event)
}
```

Implemented via `by mvi(initialState)` — no boilerplate.

### 3. Contract — one file per feature

```kotlin
object UrlLauncherContract {
    data class MyUiState(val url: String, val isLoading: Boolean, val openCount: Int)
    sealed class Action { ... }
    sealed class SideEffect { ... }
}
```

### 4. ViewModel — just implement `onAction`

```kotlin
class UrlLauncherViewModel :
    KMMViewModel(),
    MVI<State, Action, Effect> by mvi(State.default()) {

    override fun onAction(event: Action) { ... }
}
```

---

## Android — `vm.use()`

A single call replaces manual `collectAsStateWithLifecycle` + `LaunchedEffect`:

```kotlin
val state = vm.use(initEvent = InitEvent.Once(Action.ViewReady)) { effect ->
    when (effect) {
        is SideEffect.OpenUrl -> { /* open browser */ }
        is SideEffect.ShowMessage -> { /* show toast */ }
    }
}
```

- `InitEvent.Once` — fires `ViewReady` once, even after back-stack return
- `InitEvent.OnScreenAppear` — fires every time the screen enters composition
- Returns `State` directly, ready to use in Compose

---

## iOS — `ViewModel` + `FlowWatch`

A Swift `@Observable` base class bridges the KMM ViewModel:

## iOS ViewModel & Lifecycle

### The problem

Kotlin's `Flow` and coroutine scopes are not natively observable from Swift. On iOS the ViewModel also has no `ViewModelStore` — there is no automatic `onCleared()` call.

Three pieces solve this:

---

### 1. `DisposableHandle` — coroutine cancellation token

A thin wrapper that hides the full coroutines API from Swift and lets iOS cancel any running collector with a single `.dispose()` call.

```kotlin
interface DisposableHandle : kotlinx.coroutines.DisposableHandle

fun DisposableHandle(block: () -> Unit): DisposableHandle = object : DisposableHandle {
    override fun dispose() { block() }
}

// Combine two handles into one
operator fun DisposableHandle.plus(other: DisposableHandle): DisposableHandle =
    DisposableHandle { this.dispose(); other.dispose() }
```

---

### 2. `FlowWatch` — observe a `Flow` from Swift

Launches a coroutine on `Dispatchers.Main` and returns a `DisposableHandle`. Cancelling the handle stops the collection.

```kotlin
fun <T> Flow<T>.watch(
    scope: CoroutineScope,
    onNext: (T) -> Unit,
): DisposableHandle = watchFlow(this, scope, onNext)
```

Usage from Swift:
```swift
let handle = viewModel.uiState.watch(scope: viewModel.viewModelScope) { state in
    self.uiState = state
}
// later:
handle.dispose()
```

---

### 3. `KMMViewModel` (iOS actual) — manual `dispose()`

On iOS there is no `ViewModelStore`, so the scope must be cancelled manually. `dispose()` cancels `viewModelScope` and is called from Swift's `deinit`.

```kotlin
actual open class KMMViewModel actual constructor() {
    actual val viewModelScope: CoroutineScope = createViewModelScope()

    fun dispose() = viewModelScope.cancel()

    protected actual open fun onCleared() { dispose() }
}
```

---

### 4. `ViewModel<T>` (Swift) — ties it all together

A generic `ObservableObject` base class that holds the KMM ViewModel and calls `dispose()` when Swift deallocates it.

```swift
class ViewModel<T: KMMViewModel>: ObservableObject {
    var model: T
    var subscriptions: Set<AnyCancellable> = []

    init(model: T) { self.model = model }

    deinit { model.dispose() }  // cancels viewModelScope
}

extension DisposableHandle {
    func asCancellable() -> AnyCancellable {
        AnyCancellable { self.dispose() }
    }
}
```

`DisposableHandle.asCancellable()` lets you store Flow watchers in `subscriptions` so they are automatically cancelled when the `ViewModel` is deallocated alongside `dispose()`.

---

### Lifecycle flow

```
SwiftUI View appears
    └─▶ @StateObject creates ViewModel<T>
            └─▶ init(model:) holds KMM ViewModel
                    └─▶ FlowWatch starts collecting uiState → publishes to SwiftUI

SwiftUI View disappears (deallocated)
    └─▶ deinit fires
            └─▶ model.dispose() → viewModelScope.cancel()
                    └─▶ all FlowWatch coroutines stop
```

```swift
class UrlLauncherModel: ViewModel<UrlLauncherViewModel> {
    // uiState is automatically published via FlowWatch
    // emit() dispatches actions to the shared ViewModel
}
```

```swift
struct UrlLauncherView: View {
    @StateObject private var viewModel = UrlLauncherModel()

    var body: some View {
        Text(viewModel.uiState.url)
        Button("Open URL") {
            viewModel.emit(UrlLauncherContractActionOpenUrlClicked())
        }
        .onFirstAppear {
            viewModel.dispatchInitialEventIfNeeded()
        }
    }
}
```

---

## File Map

| # | File | Purpose |
|---|------|---------|
| 1 | `DisposableHandle.kt` | iOS coroutine cancellation bridge |
| 2 | `FlowWatch.kt` | Observe Kotlin `Flow` from Swift |
| 3 | `KMMViewModel.kt` | Shared ViewModel base (Android + iOS) |
| 4 | `MVI.kt` | MVI interface + `mvi()` delegate |
| 5 | `UrlLauncherContract.kt` | State / Action / SideEffect definitions |
| 6 | `UrlLauncherViewModel.kt` | Shared ViewModel implementation |
| 7 | `ViewModel.swift` | Swift `@Observable` base wrapping KMM VM |
| 8 | `UrlLauncherModel.swift` | iOS-side ViewModel |
| 9 | `FirstAppear.swift` | `onFirstAppear` SwiftUI modifier |
| 10 | `UrlLauncherView.swift` | SwiftUI screen |
| 11 | `UrlLauncherScreen.kt` | Compose screen |
