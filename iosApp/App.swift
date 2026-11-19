import FirebaseAppCheck
import FirebaseCore
import RecaptchaEnterprise
import RoutineShared
import SwiftUI

@main
struct RoutineBuilderApp: App {
  @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
  var body: some Scene {
    WindowGroup {
      NativeEditor(bridge: delegate.bridge, inbox: delegate.inbox)
        .ignoresSafeArea()
        .onOpenURL { delegate.open($0) }
    }
  }
}
struct NativeEditor: UIViewControllerRepresentable {
  let bridge: NativeBridge
  let inbox: DocumentInbox
  func makeUIViewController(context: Context) -> UIViewController {
    let controller = MainViewControllerKt.MainViewController(bridge: bridge, inbox: inbox, judgingAccess: bridge.judgingAccess)
    bridge.presenter = controller
    return controller
  }
  func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
final class AppDelegate: NSObject, UIApplicationDelegate {
  let bridge = NativeBridge()
  let inbox = DocumentInbox()
  func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions options: [UIApplication.LaunchOptionsKey: Any]? = nil
  ) -> Bool {
    #if DEBUG
      AppCheck.setAppCheckProviderFactory(AppCheckDebugProviderFactory())
    #else
      AppCheck.setAppCheckProviderFactory(ConfiguredAppCheckFactory())
    #endif
    if Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil {
      FirebaseApp.configure()
    }
    #if DEBUG || ATTESTATION_TEST
      if let mode = ProcessInfo.processInfo.arguments.first(where: {
        $0.hasPrefix("--routine-probe=")
      })?.split(separator: "=").last {
        runProbe(String(mode))
      }
    #endif
    return true
  }
  #if DEBUG || ATTESTATION_TEST
    // Developer smoke-test entry point; no production UI or private prompt recording.
    private func runProbe(_ mode: String) {
      guard ["availability", "attestation", "cloud"].contains(mode) else { return }
      func record(_ object: [String: Any]) {
        guard
          let directory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
            .first,
          let data = try? JSONSerialization.data(withJSONObject: object.merging(["probe": mode, "recordedAt": ISO8601DateFormatter().string(from: Date())]) { _, new in new }, options: .sortedKeys)
        else { return }
        try? data.write(
          to: directory.appendingPathComponent("routine-generation-probe.json"),
          options: [.atomic, .completeFileProtection])
      }
      record(["status": "started", "inferencePerformed": false])
      Task { @MainActor in
        if mode == "attestation" {
          do {
            let token = try await AppCheck.appCheck().token(forcingRefresh: true)
            record(["platform": "iOS", "appCheckVerified": !token.token.isEmpty,
                    "judgingAccess": bridge.judgingAccess, "inferencePerformed": false])
          } catch {
            let error = error as NSError
            record(["platform": "iOS", "appCheckVerified": false,
                    "errorDomain": error.domain, "errorCode": error.code,
                    "diagnosticCategories": ["attestation failed", "too many attempts", "api key not valid", "permission_denied", "invalid_argument", "unauthenticated", "recaptcha", "quota", "network", "expired", "403", "401", "fetch", "score", "throttl"].filter { error.description.lowercased().contains($0) },
                    "underlyingDomain": (error.userInfo[NSUnderlyingErrorKey] as? NSError)?.domain ?? "none",
                    "underlyingCode": (error.userInfo[NSUnderlyingErrorKey] as? NSError)?.code ?? 0,
                    "inferencePerformed": false])
          }
        } else if mode == "availability" {
          bridge.localAvailability {
            record(["platform": "iOS", "onDeviceAvailability": $0, "inferencePerformed": false])
          }
        } else {
          bridge.generate(
            prompt:
              "Return JSON only with tasks and rewards arrays. Suggest one child-friendly task for choosing clothes and one free shared-game reward. Each object has name, description, positive integer pointAmount, icon Check or Games, color BLUE, limitType NONE, limitValue 1 and requireApproval true. No personal details.",
            cloudAllowed: true, advanced: true
          ) { text, source, error in
            let parsed = text?.data(using: .utf8).flatMap {
              try? JSONSerialization.jsonObject(with: $0) as? [String: Any]
            }
            record([
              "platform": "iOS", "source": source ?? "none",
              "success": error == nil && parsed?["tasks"] is [[String: Any]]
                && parsed?["rewards"] is [[String: Any]], "errorType": error ?? "none",
              "syntheticResponse": text ?? "",
            ])
          }
        }
      }
    }
  #endif
  func open(_ url: URL) {
    let access = url.startAccessingSecurityScopedResource()
    defer { if access { url.stopAccessingSecurityScopedResource() } }
    guard url.isFileURL, let handle = try? FileHandle(forReadingFrom: url) else { return }
    defer { try? handle.close() }
    guard let data = try? handle.read(upToCount: 200001), data.count <= 200000,
      let text = String(data: data, encoding: .utf8)
    else {
      inbox.document = "{}"
      return
    }
    inbox.document = text
  }
}
final class ConfiguredAppCheckFactory: NSObject, AppCheckProviderFactory {
  func createProvider(with app: FirebaseApp) -> AppCheckProvider? {
    guard let url = Bundle.main.url(forResource: "AppCheck.local", withExtension: "plist"),
      let data = try? Data(contentsOf: url),
      let settings = try? PropertyListSerialization.propertyList(from: data, format: nil) as? [String: Any],
      let siteKey = settings["RecaptchaSiteKey"] as? String,
      !siteKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    else { return nil }
    return RecaptchaProviderFactory(siteKey: siteKey).createProvider(with: app)
  }
}
