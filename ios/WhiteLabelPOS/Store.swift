import Foundation
import Observation
import POSCore
import SwiftUI

enum MainTab: Hashable {
    case sell, orders, products, reports, settings
}

enum SellRoute: Hashable {
    case cart, checkout, receipt(String)
}

enum ProductRoute: Hashable {
    case edit(String), new, categories
}

enum SettingsRoute: Hashable {
    case step(Int), pin
}

/// Keeps the shop on the device (one file, the same format as a backup) and saves after every change. Also holds the
/// sale being rung up and where each tab is.
@Observable
final class Store {
    private(set) var doc: Doc
    let config: Config
    var cart = Cart()
    var tab = MainTab.sell
    var sellPath: [SellRoute] = []
    var orderPath: [String] = []
    var productsPath: [ProductRoute] = []
    var settingsPath: [SettingsRoute] = []
    /// The owner PIN was typed since the app opened.
    var unlocked = false
    /// Opens setup at this step even when the shop is set up (screenshot switch).
    var setupStep: Int?
    /// Changes when the whole shop is replaced, so setup starts fresh.
    private(set) var setupKey = 0

    private let queue = DispatchQueue(label: "whitelabelpos.save")

    init() {
        config = Store.loadConfig()
        doc = Store.readFile() ?? Doc()
        #if DEBUG
        applyLaunchSwitches()
        #endif
    }

    /// A store with given settings and shop (previews and picture checks); nothing is read from or saved to the device
    /// until something changes.
    init(config: Config, doc: Doc) {
        self.config = config
        self.doc = doc
    }

    var look: Look { Look(design: config.design(doc.brand.design), brand: doc.brand) }
    var currency: Currency { config.currency(doc.business.currency) }

    func money(_ amount: Int) -> String {
        Money.format(amount, symbol: currency.symbol, decimals: currency.decimals)
    }

    /// The amount as typed in a field ("26.20", no symbol).
    func plain(_ amount: Int) -> String {
        Money.format(amount, symbol: "", decimals: currency.decimals)
    }

    // MARK: - changing things

    /// Changes the shop and saves it.
    func update(_ change: (inout Doc) -> Void) {
        change(&doc)
        save()
    }

    /// Replaces the whole shop (restore, demo, start over, finishing setup).
    func replace(_ newDoc: Doc) {
        doc = newDoc
        cart.clear()
        sellPath = []
        orderPath = []
        productsPath = []
        settingsPath = []
        setupStep = nil
        tab = .sell
        setupKey += 1
        save()
    }

    func loadDemo(_ typeId: String) {
        guard let t = config.type(typeId) else { return }
        replace(Demo.shop(t, now: Date(), calendar: .current))
        unlocked = false
    }

    func startOver() {
        unlocked = false
        replace(Doc())
    }

    func addToCart(_ p: Product) -> Bool {
        guard doc.canAddOne(p, cart: cart) else { return false }
        cart.add(p.id)
        return true
    }

    @discardableResult
    func completeSale(method: PayMethod, paid: Int) -> Sale {
        var sale = Sale()
        let c = cart
        update { sale = $0.completeSale(c, method: method, paid: paid, time: Store.nowMs()) }
        cart.clear()
        return sale
    }

    func refund(_ saleId: String) {
        update { $0.refund(saleId, time: Store.nowMs()) }
    }

    static func nowMs() -> Int {
        Int((Date().timeIntervalSince1970 * 1000).rounded())
    }

    // MARK: - files

    private static var folder: URL {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("WhiteLabelPOS", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }

    private static var file: URL { folder.appendingPathComponent("shop.json") }

    private static func readFile() -> Doc? {
        guard let data = try? Data(contentsOf: file) else { return nil }
        return try? DocFile.read(data)
    }

    private func save() {
        let data = DocFile.write(doc)
        let target = Store.file
        queue.async {
            try? data.write(to: target, options: .atomic)
        }
    }

    private static func loadConfig() -> Config {
        func data(_ name: String) -> Data? {
            Bundle.main.url(forResource: name, withExtension: "json").flatMap { try? Data(contentsOf: $0) }
        }
        guard let d = data("designs"), let c = data("catalogs"), let cu = data("currencies"),
              let config = try? Config.load(designs: d, catalogs: c, currencies: cu) else { return .fallback }
        return config
    }

    /// Name of the backup file.
    var backupName: String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        let name = doc.brand.name.isEmpty ? "Shop" : doc.brand.name
        return "\(name) backup \(f.string(from: Date())).json"
    }

    // MARK: - screenshot switches (debug builds only)

    #if DEBUG
    private func applyLaunchSwitches() {
        let ud = UserDefaults.standard
        if let demo = ud.string(forKey: "demo"), let t = config.type(demo) {
            doc = Demo.shop(t, now: Date(), calendar: .current)
        }
        if let design = ud.string(forKey: "design"), config.designs.contains(where: { $0.id == design }) {
            doc.brand.design = design
        }
        guard let screen = ud.string(forKey: "screen") else { return }
        if screen.hasPrefix("setup"), let n = Int(screen.dropFirst(5)) {
            setupStep = min(max(n, 0), 5)
            return
        }
        switch screen {
        case "till":
            // the till with a sale under way (badges on products; on wide screens the sale on the right)
            cart = Demo.sampleCart(doc)
        case "cart":
            cart = Demo.sampleCart(doc)
            sellPath = [.cart]
        case "checkout":
            cart = Demo.sampleCart(doc)
            sellPath = [.cart, .checkout]
        case "receipt":
            cart = Demo.sampleCart(doc)
            let due = doc.totals(cart, method: .cash).due
            let sale = doc.completeSale(cart, method: .cash, paid: Money.nextThousand(due), time: Store.nowMs())
            cart.clear()
            sellPath = [.receipt(sale.id)]
        case "orders":
            tab = .orders
        case "products":
            tab = .products
            unlocked = true
        case "product":
            tab = .products
            unlocked = true
            if let p = doc.products.first { productsPath = [.edit(p.id)] }
        case "reports":
            tab = .reports
        case "settings":
            tab = .settings
            unlocked = true
        default:
            tab = .sell
        }
    }
    #endif
}
