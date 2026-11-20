import XCTest

final class SmokeTests: XCTestCase {
  func testKeyboardCanBeDismissedAndFormRemainsScrollable() throws {
    continueAfterFailure = false
    let app = XCUIApplication()
    app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US", "-parent-plan", ""]
    app.launch()
    let field = app.textViews["What would you like to make easier?"]
    XCTAssertTrue(field.waitForExistence(timeout: 10))
    field.tap()
    field.typeText("Help a child get dressed for school. Suggest small tasks and free rewards.")
    field.tap()
    Thread.sleep(forTimeInterval: 1)
    XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 5))
    let shown = XCTAttachment(screenshot: app.screenshot())
    shown.name = "Form with keyboard"; shown.lifetime = .keepAlways; add(shown)
    // A visible native Done action must dismiss even a multiline prompt.
    let done = app.keyboards.buttons.matching(NSPredicate(format: "label ==[c] %@", "done")).firstMatch
    XCTAssertTrue(done.waitForExistence(timeout: 5))
    if done.isHittable {
      done.tap()
    } else {
      // Headless simulators may expose the keyboard below the display while using hardware input.
      // Return must invoke the same Done action rather than inserting another line.
      app.typeText("\n")
    }
    XCTAssertTrue(app.keyboards.firstMatch.waitForNonExistence(timeout: 5))
    let generate = app.buttons["Find tasks and rewards"]
    for _ in 0..<12 {
      if generate.exists && app.frame.insetBy(dx: 0, dy: 16).contains(generate.frame) { break }
      app.swipeUp(velocity: .slow)
      Thread.sleep(forTimeInterval: 0.8)
    }
    XCTAssertTrue(generate.isEnabled)
    XCTAssertTrue(app.frame.insetBy(dx: 0, dy: 16).contains(generate.frame))
    // Do not invoke generation: this regression must not consume model quota.
    XCTAssertFalse(app.keyboards.firstMatch.exists)
    let finished = XCTAttachment(screenshot: app.screenshot())
    finished.name = "Reachable generation action"; finished.lifetime = .keepAlways; add(finished)
    app.terminate()
  }

  func testDirectEntryManualPlanAndReview() throws {
    let app = XCUIApplication()
    app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
    app.launch()
    XCTAssertFalse(app.buttons["I am 18 or older · Get started"].exists)
    XCTAssertFalse(app.buttons["Deutsch"].exists)
    XCTAssertFalse(app.buttons["English"].exists)
    if !app.descendants(matching: .any).matching(identifier: "Find what works for your child.").firstMatch.exists {
      for _ in 0..<5 {
        app.swipeDown(velocity: .slow)
        Thread.sleep(forTimeInterval: 0.8)
        if app.buttons["New ideas"].exists && app.frame.insetBy(dx: 0, dy: 16).contains(app.buttons["New ideas"].frame) { break }
      }
      let newIdeas = app.buttons["New ideas"].frame
      app.coordinate(withNormalizedOffset: .zero).withOffset(CGVector(dx: newIdeas.midX, dy: newIdeas.midY)).tap()
      if app.buttons["Start new"].waitForExistence(timeout: 3) { app.buttons["Start new"].tap() }
    }
    XCTAssertTrue(app.descendants(matching: .any).matching(identifier: "Find what works for your child.").firstMatch.waitForExistence(timeout: 10))
    XCTAssertFalse(app.staticTexts["For parents aged 18+"].exists)
    let write = app.buttons["Write my own"]
    for _ in 0..<10 {
      if write.exists && app.frame.insetBy(dx: 0, dy: 16).contains(write.frame) { break }
      app.swipeUp(velocity: .slow)
      // XCTest does not track Compose scroll deceleration as an animation.
      Thread.sleep(forTimeInterval: 0.8)
    }
    XCTAssertTrue(app.frame.insetBy(dx: 0, dy: 16).contains(write.frame), "Manual button must be fully onscreen before tapping")
    XCTAssertTrue(app.buttons["Write my own"].exists)
    // Use the settled visible frame rather than an accessibility scroll activation.
    // Tap the verified visible centre, as a user would.
    let centre = CGVector(dx: write.frame.midX, dy: write.frame.midY)
    app.coordinate(withNormalizedOffset: .zero).withOffset(centre).tap()
    let arrived = app.descendants(matching: .any).matching(identifier: "Choose what fits your family").firstMatch.waitForExistence(timeout: 10)
    if !arrived { print("MANUAL_FLOW_HIERARCHY: " + app.debugDescription) }
    XCTAssertTrue(arrived)
    // Saving and deleting a copy must leave the current editable draft intact.
    tapVisible(app.buttons["Save ideas"], in: app, upward: true)
    tapVisible(app.buttons["Saved plans"], in: app, upward: false)
    let delete = app.buttons["Delete: My plan"].firstMatch
    tapVisible(delete, in: app, upward: true)
    XCTAssertTrue(app.buttons["Cancel"].waitForExistence(timeout: 3))
    app.buttons["Cancel"].tap()
    XCTAssertTrue(delete.exists)
    tapVisible(delete, in: app, upward: true)
    app.buttons["Delete"].tap()
    XCTAssertTrue(app.descendants(matching: .any).matching(identifier: "Saved plan deleted.").firstMatch.waitForExistence(timeout: 5))
    XCTAssertFalse(delete.exists)
    if #available(iOS 17.0, *) {
      try app.performAccessibilityAudit(for: [.hitRegion, .sufficientElementDescription, .trait, .contrast, .textClipped])
    }
    let screenshot = XCTAttachment(screenshot: app.screenshot())
    screenshot.lifetime = .keepAlways
    add(screenshot)
    tapVisible(app.buttons["Saved plans"], in: app, upward: false)
    XCTAssertTrue(app.textViews["Plan title"].waitForExistence(timeout: 5))
    XCTAssertEqual(app.textViews["Plan title"].value as? String, "My plan")
  }
  private func tapVisible(_ element: XCUIElement, in app: XCUIApplication, upward: Bool) {
    for _ in 0..<12 {
      if element.exists && app.frame.insetBy(dx: 0, dy: 16).contains(element.frame) { break }
      let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.8, dy: upward ? 0.8 : 0.3))
      let end = app.coordinate(withNormalizedOffset: CGVector(dx: 0.8, dy: upward ? 0.3 : 0.8))
      // Hold at the destination before lifting so Compose does not keep flinging.
      start.press(forDuration: 0.1, thenDragTo: end, withVelocity: .slow, thenHoldForDuration: 1.0)
      Thread.sleep(forTimeInterval: 0.8)
    }
    XCTAssertTrue(element.exists && app.frame.insetBy(dx: 0, dy: 16).contains(element.frame))
    Thread.sleep(forTimeInterval: 1.5)
    element.tap()
    Thread.sleep(forTimeInterval: 0.8)
  }
  func testGermanDeviceLocaleAndUnsupportedLanguageFallback() {
    let app = XCUIApplication()
    for (language, locale, expected) in [
      ("de", "de_DE", "Neue Ideen"),
      ("fr", "fr_FR", "New ideas")
    ] {
      app.launchArguments = ["-AppleLanguages", "(\(language))", "-AppleLocale", locale]
      app.launch()
      for _ in 0..<6 { if app.buttons[expected].isHittable { break }; app.swipeDown(velocity: .slow); Thread.sleep(forTimeInterval: 0.8) }
      XCTAssertTrue(app.buttons[expected].waitForExistence(timeout: 10))
      XCTAssertFalse(app.buttons["Deutsch"].exists)
      XCTAssertFalse(app.buttons["English"].exists)
      app.terminate()
    }
  }

}
