import Foundation
import Observation

struct DlToastMessage: Identifiable, Equatable {
    let id: Int
    let text: String
    let tone: ToneKind
}

@MainActor
@Observable
final class DlToastModel {
    private(set) var message: DlToastMessage?

    @ObservationIgnored private var counter = 0
    @ObservationIgnored private var task: Task<Void, Never>?

    func show(_ text: String, tone: ToneKind) {
        counter += 1
        let id = counter
        message = DlToastMessage(id: id, text: text, tone: tone)
        task?.cancel()
        task = Task { [weak self] in
            try? await Task.sleep(for: .seconds(3.5))
            guard !Task.isCancelled else { return }
            self?.dismiss(id)
        }
    }

    func dismiss(_ id: Int) {
        if message?.id == id { message = nil }
    }
}

extension ErrorInfo {
    var detailed: String {
        message.isEmpty ? localized : localized + " " + message
    }
}
