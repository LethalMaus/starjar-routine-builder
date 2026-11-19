import XCTest
import FirebaseAppCheck
import RoutineShared
import NaturalLanguage

@testable import RoutineBuilder

// Deliberately in a separate scheme: running these tests can consume cloud quota.
final class LiveAITests: XCTestCase {
  @MainActor
  func testOnDeviceGenerationWhenAvailable() async throws {
    let bridge = NativeBridge()
    var availability = "unknown"
    bridge.localAvailability { availability = $0 }
    try XCTSkipUnless(
      availability == "available",
      "Apple on-device model unavailable on this test device; no local inference claimed.")
    let finished = expectation(description: "On-device response")
    bridge.generate(
      prompt:
        "Return JSON only with tasks and rewards arrays, suggesting one simple child-friendly task for choosing clothes and one shared-game reward. Each has name and positive integer pointAmount.",
      cloudAllowed: false, advanced: false
    ) { text, source, error in
      XCTAssertNil(error)
      XCTAssertEqual(source, "ON_DEVICE")
      XCTAssertNotNil(text)
      finished.fulfill()
    }
    await fulfillment(of: [finished], timeout: 130)
  }

  @MainActor
  func testLocalParentPromptProducesImportableIdeas() async throws {
    try await checkLocalPlanning(
      goal: "Make getting dressed and leaving for school easier. Suggest practical child-friendly tasks and free shared-time rewards.",
      feedback: "Clothes feel uncomfortable. Offer new gentle ways to make choosing and putting on clothes easier. Do not suggest walking to school.")
  }

  @MainActor
  func testLocalBedtimePlanningAndFollowUp() async throws {
    try await checkLocalPlanning(
      goal: "Help a primary-school child start bedtime calmly: brushing teeth and putting on pyjamas. Suggest small practical actions and free reward choices, never make sleep or affection a reward.",
      feedback: "The toothbrush feels uncomfortable. Offer different gentle ways to make toothbrushing easier, without forcing or rushing.")
  }

  @MainActor
  private func checkLocalPlanning(goal: String, feedback: String) async throws {
    let bridge = NativeBridge()
    var availability = "unknown"
    bridge.localAvailability { availability = $0 }
    try XCTSkipUnless(availability == "available", "Local model unavailable; no inference claimed")
    let features = FeatureChoices(badgeCount: 0, frequency: false, streaks: false, reminders: false, urgency: false)
    for language in ["en", "de"] {
      let plan = ParentPlan(id: "local-validation", title: "School mornings", language: language, items: [], discarded: [])
      let request = PlanEngine.shared.request(plan: plan,
        goal: goal,
        feedback: "", age: "primary", minutes: "30", features: features,
        constraints: ["no_purchases", "no_screens"])
      let prompt = try await PlanEngine.shared.generationPrompt(request: request, advanced: false)
      let started = Date()
      let result: (String?, String?, String?) = await withCheckedContinuation { continuation in
        bridge.generate(prompt: prompt, cloudAllowed: false, advanced: false) { text, source, error in
          continuation.resume(returning: (text, source, error))
        }
      }
      XCTAssertNil(result.2)
      XCTAssertEqual(result.1, "ON_DEVICE")
      let text = try XCTUnwrap(result.0)
      let response = XCTAttachment(string: text)
      response.name = "Synthetic local parent response " + language
      response.lifetime = .keepAlways
      add(response)
      let merged = try PlanEngine.shared.merge(plan: plan, response: text, features: features, newId: { UUID().uuidString })
      XCTAssertTrue(merged.items.contains { $0.kind == "task" })
      XCTAssertTrue(merged.items.contains { $0.kind == "reward" })
      let recognizer = NLLanguageRecognizer()
      recognizer.processString(merged.items.map { $0.name + " " + $0.description_ }.joined(separator: " "))
      XCTAssertEqual(recognizer.dominantLanguage?.rawValue, language, "Generated text must match the requested language")
      XCTAssertTrue(merged.items.filter { $0.kind == "task" }.allSatisfy { (3...10).contains($0.pointAmount) })
      XCTAssertTrue(merged.items.filter { $0.kind == "reward" }.allSatisfy { (10...30).contains($0.pointAmount) })
      let keptDocument = PlanTransfer.shared.editable(plan: merged)
        .replacingOccurrences(of: "\"state\":\"suggested\"", with: "\"state\":\"kept\"")
      let kept = PlanTransfer.shared.read(text: keptDocument, newId: { UUID().uuidString }, language: language, title: "School mornings")
      let exported = PlanTransfer.shared.templates(plan: kept)
      let imported = PlanTransfer.shared.read(text: exported, newId: { UUID().uuidString }, language: language, title: "School mornings")
      XCTAssertEqual(imported.items.count, merged.items.count)
      let attachment = XCTAttachment(string: exported)
      attachment.name = "Validated local StarJar export " + language
      attachment.lifetime = .keepAlways
      add(attachment)
      print("LOCAL_PARENT_VERIFIED language=\(language) source=\(result.1 ?? "none") items=\(merged.items.count) seconds=\(Date().timeIntervalSince(started)) cloudAllowed=false")
      // Keep an edited task and reject another: follow-ups must append without altering either decision.
      var editable = try XCTUnwrap(try JSONSerialization.jsonObject(with: Data(PlanTransfer.shared.editable(plan: kept).utf8)) as? [String: Any])
      var savedPlan = try XCTUnwrap(editable["plan"] as? [String: Any])
      var savedItems = try XCTUnwrap(savedPlan["items"] as? [[String: Any]])
      savedItems[0]["pointAmount"] = 9
      savedPlan["items"] = savedItems
      editable["plan"] = savedPlan
      let edited = PlanTransfer.shared.read(text: String(decoding: try JSONSerialization.data(withJSONObject: editable), as: UTF8.self), newId: { UUID().uuidString }, language: language, title: "School mornings")
      let removed = try XCTUnwrap(edited.items.first { $0.id != edited.items[0].id })
      let previous = PlanEngine.shared.discard(plan: edited, id: removed.id)
      let followRequest = PlanEngine.shared.request(plan: previous,
        goal: goal,
        feedback: feedback,
        age: "primary", minutes: "30", features: features, constraints: ["no_purchases", "no_screens"])
      let followPrompt = try await PlanEngine.shared.generationPrompt(request: followRequest, advanced: false)
      let follow: (String?, String?, String?) = await withCheckedContinuation { continuation in
        bridge.generate(prompt: followPrompt, cloudAllowed: false, advanced: false) { text, source, error in continuation.resume(returning: (text, source, error)) }
      }
      XCTAssertNil(follow.2)
      XCTAssertEqual(follow.1, "ON_DEVICE")
      let followText = try XCTUnwrap(follow.0)
      let followAttachment = XCTAttachment(string: followText)
      followAttachment.name = "Synthetic local follow-up " + language
      followAttachment.lifetime = .keepAlways
      add(followAttachment)
      let updated = try PlanEngine.shared.merge(plan: previous, response: followText, features: features, newId: { UUID().uuidString })
      XCTAssertEqual(Array(updated.items.prefix(previous.items.count)), previous.items)
      XCTAssertFalse(updated.items.contains { $0.name == removed.name })
      XCTAssertGreaterThan(updated.items.count, previous.items.count)
      let additions = updated.items.dropFirst(previous.items.count)
      let followRecognizer = NLLanguageRecognizer()
      followRecognizer.processString(additions.map { $0.name + " " + $0.description_ }.joined(separator: " "))
      XCTAssertEqual(followRecognizer.dominantLanguage?.rawValue, language)
      print("LOCAL_FOLLOWUP_VERIFIED language=\(language) additions=\(additions.count) keptUnchanged=true discardedExcluded=true cloudAllowed=false")

    }
  }

  @MainActor
  func testCloudGenerationUsesProtectedFirebaseRoute() async throws {
    guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else {
      throw XCTSkip(
        "Provide private Firebase configuration and register the App Check debug token first.")
    }
    let token = try await AppCheck.appCheck().token(forcingRefresh: true)
    XCTAssertFalse(token.token.isEmpty) // Never log or persist tokens.
    let finished = expectation(description: "Protected cloud response")
    let bridge = NativeBridge()
    bridge.generate(
      prompt:
        "Return JSON only: tasks and rewards arrays. Suggest one child-friendly task for choosing clothes and one free shared-game reward. Each item has name, description, positive integer pointAmount, icon Check or Games, color BLUE, limitType NONE, limitValue 1, requireApproval true. No personal details.",
      cloudAllowed: true, advanced: true
    ) { text, source, error in
      XCTAssertNil(error)
      XCTAssertEqual(source, "IN_CLOUD")
      if let text, let data = text.data(using: .utf8),
        let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
      {
        XCTAssertNotNil(json["tasks"] as? [[String: Any]])
        XCTAssertNotNil(json["rewards"] as? [[String: Any]])
      } else {
        XCTFail("Expected structured JSON")
      }
      finished.fulfill()
    }
    await fulfillment(of: [finished], timeout: 130)
  }
}
