import XCTest
@testable import RoutineBuilder

final class AccessPolicyTests: XCTestCase {
  func testJudgingAccessPreservesAnExhaustedStandardAllowance() throws {
    let suite = "routine-access-test-\(UUID().uuidString)"
    let store = UserDefaults(suiteName: suite)!
    defer { store.removePersistentDomain(forName: suite) }
    let standard = NativeBridge(store: store, judgingAccess: false)
    XCTAssertNil(try standard.reserveCloudAttempt(cloudAllowed: false))
    for _ in 0..<10 { XCTAssertNotNil(try standard.reserveCloudAttempt(cloudAllowed: true)) }
    XCTAssertThrowsError(try standard.reserveCloudAttempt(cloudAllowed: true))
    let before = store.dictionary(forKey: "cloud-attempts") as NSDictionary?
    let judging = NativeBridge(store: store, judgingAccess: true)
    for _ in 0..<20 { XCTAssertNil(try judging.reserveCloudAttempt(cloudAllowed: true)) }
    XCTAssertEqual(before, store.dictionary(forKey: "cloud-attempts") as NSDictionary?)
    XCTAssertThrowsError(try standard.reserveCloudAttempt(cloudAllowed: true))
  }
}
