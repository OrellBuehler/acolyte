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
        .preferredColorScheme(.dark)
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
    @State private var tabs: TVTabSelection

    init(account: AccountInfo) {
        self.account = account
        #if DEBUG
        _tabs = State(initialValue: TVTabSelection(TVScreenshotLaunch.startTab(for: account)))
        #else
        _tabs = State(initialValue: TVTabSelection(.home))
        #endif
    }

    var body: some View {
        TabView(selection: Binding(get: { tabs.value }, set: { tabs.value = $0 })) {
            ForEach(TVSections.tabs(for: account)) { section in
                Tab(section.title, systemImage: section.symbol, value: section) {
                    TVTabHost(account: account, tabs: tabs) {
                        TVSectionScreen(section: section, account: account)
                    }
                }
            }
        }
    }
}
