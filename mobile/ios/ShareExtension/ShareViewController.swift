import UIKit
import Social

class ShareViewController: UIViewController {

  override func viewDidAppear(_ animated: Bool) {
    super.viewDidAppear(animated)

    guard
      let extensionItem = extensionContext?.inputItems.first as? NSExtensionItem,
      let itemProvider = extensionItem.attachments?.first
    else {
      extensionContext?.completeRequest(returningItems: nil)
      return
    }

    let urlType = "public.url"
    let textType = "public.plain-text"

    if itemProvider.hasItemConformingToTypeIdentifier(urlType) {
      itemProvider.loadItem(forTypeIdentifier: urlType, options: nil) { [weak self] url, _ in
        let urlString = (url as? URL)?.absoluteString ?? (url as? String) ?? ""
        self?.openBriefen(with: urlString)
      }
    } else if itemProvider.hasItemConformingToTypeIdentifier(textType) {
      itemProvider.loadItem(forTypeIdentifier: textType, options: nil) { [weak self] text, _ in
        let urlString = text as? String ?? ""
        self?.openBriefen(with: urlString)
      }
    } else {
      extensionContext?.completeRequest(returningItems: nil)
    }
  }

  private func openBriefen(with urlString: String) {
    let suiteName = "group.dev.azurecoder.briefen"
    let defaults = UserDefaults(suiteName: suiteName)
    defaults?.set(urlString, forKey: "shared_url")
    defaults?.synchronize()

    let scheme = "briefen://share"
    var responder: UIResponder? = self
    while responder != nil {
      if let application = responder as? UIApplication {
        application.open(URL(string: scheme)!, options: [:])
        break
      }
      responder = responder?.next
    }

    extensionContext?.completeRequest(returningItems: nil)
  }
}
