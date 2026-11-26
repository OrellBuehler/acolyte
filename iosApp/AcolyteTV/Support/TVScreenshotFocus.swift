import SwiftUI

extension View {
    func screenshotFocus(_ focus: FocusState<String?>.Binding, id: String?) -> some View {
        #if DEBUG
        modifier(TVScreenshotFocusModifier(focus: focus, id: id))
        #else
        self
        #endif
    }
}

#if DEBUG
private struct TVScreenshotFocusModifier: ViewModifier {
    let focus: FocusState<String?>.Binding
    let id: String?

    func body(content: Content) -> some View {
        content.task(id: id) {
            guard UserDefaults.standard.bool(forKey: "acolyte_screenshot_focus"), let id else { return }
            try? await Task.sleep(for: .seconds(3))
            focus.wrappedValue = id
        }
    }
}
#endif
