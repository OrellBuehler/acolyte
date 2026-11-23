import Foundation

enum Format {
    static func date(iso: String) -> Date? {
        guard !iso.isEmpty else { return nil }
        let plain = iso.replacingOccurrences(of: #"\.\d+"#, with: "", options: .regularExpression)
        return try? Date(plain, strategy: .iso8601)
    }

    static func relative(_ date: Date) -> String {
        date.formatted(.relative(presentation: .named, unitsStyle: .wide))
    }
}
