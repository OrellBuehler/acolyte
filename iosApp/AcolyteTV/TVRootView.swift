import SwiftUI

struct TVRootView: View {
    @Environment(TVAppModel.self) private var model
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()
            switch model.session {
            case .restoring:
                TVBrandMark(size: 160)
            case let .signedOut(_, _, expired):
                TVSignInView(expired: expired)
                    .transition(.opacity)
            case let .signedIn(account):
                TVMainTabs(account: account)
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

struct TVBrandMark: View {
    var size: CGFloat = 120

    var body: some View {
        Image("BrandMark")
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)
            .clipShape(RoundedRectangle(cornerRadius: size * 0.22, style: .continuous))
            .accessibilityHidden(true)
    }
}

struct TVMainTabs: View {
    let account: AccountInfo

    var body: some View {
        TabView {
            Tab(L10n.tr("tv.tab.home"), systemImage: "house") {
                TVHomeView()
            }
            Tab(L10n.tr("tv.tab.settings"), systemImage: "gearshape") {
                TVSettingsView(account: account)
            }
        }
    }
}
