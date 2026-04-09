import SafariServices
import shared
import SwiftUI

// ─── SwiftUI View ────────────────────────────────────────────────────────────

struct UrlLauncherView: View {
    @StateObject private var viewModel = UrlLauncherModel()

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {

                if viewModel.uiState.isLoading {
                    ProgressView()
                } else {
                    Text(viewModel.uiState.url)
                    openButton
                    openCountLabel
                }
            }
            .padding()
            .navigationTitle("URL Launcher")
        }
        // ── Lifecycle ────────────────────────────────────────────────────────
        .onFirstAppear {
            // Fires once → dispatches ViewReady → ViewModel loads initial data
            viewModel.dispatchInitialEventIfNeeded()
        }
        // ── Side-effect: open URL ────────────────────────────────────────────
        .sheet(item: $viewModel.urlToOpen) { url in
            SafariView(url: url)
                .ignoresSafeArea()
        }
        // ── Side-effect: show message ────────────────────────────────────────
        .alert(
            viewModel.toastMessage ?? "",
            isPresented: Binding(
                get: { viewModel.toastMessage != nil },
                set: { if !$0 { viewModel.toastMessage = nil } }
            )
        ) {
            Button("OK", role: .cancel) {}
        }
    }

    // ── Sub-views ─────────────────────────────────────────────────────────────

    private var openButton: some View {
        Button("Open URL") {
            viewModel.emit(UrlLauncherContractActionOpenUrlClicked())
        }
        .buttonStyle(.borderedProminent)
    }

    private var openCountLabel: some View {
        Text("Opened \(viewModel.uiState.openCount) time(s)")
            .foregroundStyle(.secondary)
            .font(.caption)
    }
}

// ─── SFSafariViewController wrapper ─────────────────────────────────────────

struct SafariView: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> SFSafariViewController {
        SFSafariViewController(url: url)
    }

    func updateUIViewController(_ vc: SFSafariViewController, context: Context) {}
}

// Make URL identifiable so it works with .sheet(item:)
extension URL: @retroactive Identifiable {
    public var id: String { absoluteString }
}

// ─── Preview ─────────────────────────────────────────────────────────────────

#Preview {
    UrlLauncherView()
}
