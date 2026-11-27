import UIKit
import XCTest

@MainActor
final class SmokeTests: XCTestCase {
    private var counter = 0

    private var folder: URL {
        URL(fileURLWithPath: ProcessInfo.processInfo.environment["SMOKE_OUT"] ?? "/tmp/acolyte-smoke")
    }

    private func launch(_ open: String) -> XCUIApplication {
        continueAfterFailure = false
        try? FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        let app = XCUIApplication()
        app.launchArguments = [
            "-AppleLanguages", "(en)", "-AppleLocale", "en_US",
            "-acolyte_screenshot_server", "http://127.0.0.1:6263",
            "-acolyte_screenshot_user", "noor",
            "-acolyte_screenshot_password", "demo",
            "-acolyte_screenshot_open", open,
        ]
        app.launch()
        return app
    }

    private func step(_ app: XCUIApplication, _ test: String, _ name: String, _ button: XCUIRemote.Button? = nil, wait: TimeInterval = 1.3) {
        if let button { XCUIRemote.shared.press(button) }
        Thread.sleep(forTimeInterval: wait)
        counter += 1
        let tag = test + "-" + String(format: "%02d", counter) + "-" + name
        XCTAssertTrue(app.state == .runningForeground, "the app is not running at \(tag)")
        save(tag)
        check(app, test, tag, moved: button != nil)
    }

    private func save(_ tag: String) {
        let image = XCUIScreen.main.screenshot().image
        let size = CGSize(width: 1280, height: 720)
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        let data = UIGraphicsImageRenderer(size: size, format: format).jpegData(withCompressionQuality: 0.6) { _ in
            image.draw(in: CGRect(origin: .zero, size: size))
        }
        try? data.write(to: folder.appendingPathComponent(tag + ".jpg"))
    }

    private func check(_ app: XCUIApplication, _ test: String, _ tag: String, moved: Bool) {
        guard let root = try? app.snapshot() else {
            XCTFail("no view hierarchy at \(tag)")
            return
        }
        let screen = root.frame
        var lines: [String] = ["== " + tag]
        var focused: CGRect?
        var oversized: CGRect?
        collect(root, screen: screen, lines: &lines, focused: &focused, oversized: &oversized)
        let text = lines.joined(separator: "\n") + "\n"
        let file = folder.appendingPathComponent(test + ".txt")
        if let handle = try? FileHandle(forWritingTo: file) {
            handle.seekToEndOfFile()
            handle.write(Data(text.utf8))
            try? handle.close()
        } else {
            try? Data(text.utf8).write(to: file)
        }
        if let oversized {
            XCTFail("a page is laid out larger than the screen at \(tag): \(oversized)")
        }
        guard moved else { return }
        guard let focused else {
            XCTFail("nothing has focus at \(tag)")
            return
        }
        XCTAssertTrue(screen.contains(CGPoint(x: focused.midX, y: focused.midY)), "focus is off screen at \(tag): \(focused)")
    }

    private func collect(_ node: any XCUIElementSnapshot, screen: CGRect, lines: inout [String], focused: inout CGRect?, oversized: inout CGRect?) {
        let type = node.elementType
        let frame = node.frame
        if node.hasFocus { focused = frame }
        if type == .scrollView, abs(frame.width - screen.width) < 1, frame.height > screen.height + 1 { oversized = frame }
        if type == .staticText || type == .button || type == .scrollView || node.hasFocus {
            let mark = node.hasFocus ? "*" : " "
            let box = "[\(Int(frame.minX)),\(Int(frame.minY)) \(Int(frame.width))x\(Int(frame.height))]"
            lines.append("\(mark) \(type.rawValue) \(box) \(node.label.prefix(48))")
        }
        for child in node.children {
            collect(child, screen: screen, lines: &lines, focused: &focused, oversized: &oversized)
        }
    }

    private func walk(_ app: XCUIApplication, _ test: String, _ moves: String) {
        for move in moves {
            switch move {
            case "D": step(app, test, "down", .down)
            case "U": step(app, test, "up", .up)
            case "L": step(app, test, "left", .left)
            case "R": step(app, test, "right", .right)
            case "S": step(app, test, "select", .select, wait: 7)
            case "M": step(app, test, "menu", .menu, wait: 3)
            default: break
            }
        }
    }

    private func title(_ test: String, _ target: String) {
        let app = launch(target)
        step(app, test, "open", wait: 14)
        walk(app, test, "DDRRRDDRRDDUUUUUUU")
        app.terminate()
    }

    func testTitleMovie() { title("title-movie", "title:movie:910004") }

    func testTitleShowOwned() { title("title-show-owned", "title:show:920001") }

    func testTitleShowOther() { title("title-show-other", "title:show:920021") }

    func testHomeScroll() {
        let app = launch("home")
        step(app, "home", "open", wait: 14)
        walk(app, "home", "DRRRRRRDRRRRRDRRRRDRRDDUUUUUUU")
        app.terminate()
    }

    func testHomeOpen() {
        let app = launch("home")
        step(app, "open", "open", wait: 14)
        walk(app, "open", "DSMRSMRSMDSMRSMRSMDSMRSMRSM")
        app.terminate()
    }

    func testDownloads() {
        let app = launch("downloads")
        step(app, "downloads", "open", wait: 16)
        walk(app, "downloads", "DDDDDDDDDDUUUUUUUUUUUUDDDUUUU")
        app.terminate()
    }

    func testRequests() {
        let app = launch("requests")
        step(app, "requests", "open", wait: 16)
        walk(app, "requests", "DDDDDDDUUUUUUUUU")
        app.terminate()
    }

    func testRequestsApprove() {
        let app = launch("requests")
        step(app, "approve", "open", wait: 16)
        walk(app, "approve", "DDD")
        XCUIRemote.shared.press(.select)
        XCTAssertTrue(app.buttons["Approve"].firstMatch.waitForExistence(timeout: 8), "the actions of a request did not open")
        let approve = app.buttons.matching(NSPredicate(format: "label == %@ AND hasFocus == true", "Approve"))
        for _ in 0..<4 where approve.count == 0 {
            XCUIRemote.shared.press(.down)
            Thread.sleep(forTimeInterval: 1)
        }
        save("approve-actions")
        XCTAssertTrue(approve.count > 0, "Approve cannot be reached")
        XCUIRemote.shared.press(.select)
        XCTAssertTrue(app.buttons["Undo"].firstMatch.waitForExistence(timeout: 8), "approving left no undo line")
        step(app, "approve", "approved", wait: 2)
        app.terminate()
    }

    func testCalendar() {
        let app = launch("calendar")
        step(app, "calendar", "open", wait: 16)
        walk(app, "calendar", "DDDDDDRRUUUUUUUU")
        app.terminate()
    }

    func testExplore() {
        let app = launch("explore")
        step(app, "explore", "open", wait: 16)
        walk(app, "explore", "DDRRDRRDDUUUUUUU")
        app.terminate()
    }
}
