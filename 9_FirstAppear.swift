import SwiftUI

// ─── onFirstAppear modifier ──────────────────────────────────────────────────

/// Like .onAppear but fires only once, regardless of how many times
/// the view appears (e.g. after a sheet dismiss, tab switch, etc.)
struct FirstAppear: ViewModifier {
    let action: () -> Void
    @State private var hasAppeared = false

    func body(content: Content) -> some View {
        content.onAppear {
            guard !hasAppeared else { return }
            hasAppeared = true
            action()
        }
    }
}

extension View {
    func onFirstAppear(_ action: @escaping () -> Void) -> some View {
        modifier(FirstAppear(action: action))
    }
}
