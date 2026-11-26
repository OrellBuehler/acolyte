import SwiftUI

extension View {
    func screenshotFocus(_ focus: FocusState<String?>.Binding, id: String?, screen: String) -> some View {
        #if DEBUG
        modifier(TVScreenshotFocusModifier(focus: focus, id: id, screen: screen))
        #else
        self
        #endif
    }
}

#if DEBUG
private struct TVScreenshotFocusModifier: ViewModifier {
    let focus: FocusState<String?>.Binding
    let id: String?
    let screen: String

    func body(content: Content) -> some View {
        content.task(id: id) {
            let defaults = UserDefaults.standard
            let open = defaults.string(forKey: "acolyte_screenshot_open") ?? ""
            guard defaults.bool(forKey: "acolyte_screenshot_focus"), let id, open == screen || open.hasPrefix(screen + ":") else { return }
            try? await Task.sleep(for: .seconds(3))
            focus.wrappedValue = id
        }
    }
}
#endif
