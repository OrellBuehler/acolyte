import SwiftUI

struct TVBackdropLayer: View {
    let source: String?
    var blurred = false

    var body: some View {
        ZStack {
            Color.clear
                .overlay {
                    AuthImage(source: source, size: .hero) { Theme.background }
                        .scaleEffect(blurred ? 1.15 : 1)
                        .blur(radius: blurred ? 50 : 0)
                }
                .clipped()
            if blurred {
                Theme.background.opacity(0.68)
            } else {
                LinearGradient(
                    stops: [
                        .init(color: Theme.background.opacity(0.94), location: 0),
                        .init(color: Theme.background.opacity(0.55), location: 0.5),
                        .init(color: Theme.background.opacity(0.05), location: 1),
                    ],
                    startPoint: .leading,
                    endPoint: .trailing
                )
                LinearGradient(
                    stops: [
                        .init(color: Theme.background.opacity(0), location: 0.35),
                        .init(color: Theme.background.opacity(0.94), location: 1),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
        }
        .id(source ?? "")
        .transition(.opacity)
        .animation(.easeInOut(duration: 0.4), value: source)
        .ignoresSafeArea()
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}
