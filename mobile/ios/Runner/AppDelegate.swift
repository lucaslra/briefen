import Flutter
import UIKit

@main
@objc class AppDelegate: FlutterAppDelegate, FlutterImplicitEngineDelegate {
  private let shareChannel = "dev.azurecoder.briefen/share"
  private let appGroupId = "group.dev.azurecoder.briefen"
  private let sharedUrlKey = "shared_url"
  private var methodChannel: FlutterMethodChannel?
  private var pendingUrl: String?

  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }

  // Called when the app is opened via the briefen:// URL scheme from the Share Extension
  override func application(
    _ app: UIApplication,
    open url: URL,
    options: [UIApplication.OpenURLOptionsKey: Any] = [:]
  ) -> Bool {
    if url.scheme == "briefen" && url.host == "share" {
      // Read the URL the extension wrote to the App Group
      if let defaults = UserDefaults(suiteName: appGroupId),
        let sharedUrl = defaults.string(forKey: sharedUrlKey),
        !sharedUrl.isEmpty
      {
        defaults.removeObject(forKey: sharedUrlKey)
        defaults.synchronize()
        if let ch = methodChannel {
          ch.invokeMethod("sharedUrl", arguments: sharedUrl)
        } else {
          pendingUrl = sharedUrl
        }
      }
    }
    return true
  }

  func didInitializeImplicitFlutterEngine(_ engineBridge: FlutterImplicitEngineBridge) {
    GeneratedPluginRegistrant.register(with: engineBridge.pluginRegistry)

    guard let registrar = engineBridge.pluginRegistry.registrar(forPlugin: "ShareChannel") else {
      return
    }
    let messenger = registrar.messenger()
    methodChannel = FlutterMethodChannel(name: shareChannel, binaryMessenger: messenger)
    methodChannel?.setMethodCallHandler { [weak self] call, result in
      guard let self else { return }
      if call.method == "getInitialUrl" {
        result(self.pendingUrl)
        self.pendingUrl = nil
      } else {
        result(FlutterMethodNotImplemented)
      }
    }
  }
}
