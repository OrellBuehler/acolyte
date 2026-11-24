import SwiftUI

@main
struct AcolyteTVApp: App {
    @State private var model = TVAppModel()

    var body: some Scene {
        WindowGroup {
            TVRootView()
                .environment(model)
        }
    }
}
