import SwiftUI

struct RootView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()
            switch model.session {
            case .restoring:
                BrandMark()
            case let .signedOut(_, _, expired):
                ConnectView(expired: expired)
                    .transition(.opacity)
            case let .signedIn(account):
                MainTabs(account: account)
                    .transition(.opacity)
            }
        }
        .animation(.smooth(duration: 0.3), value: model.session)
        .tint(Theme.accent)
        .task {
            await model.start()
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                model.foreground()
            }
        }
    }
}
