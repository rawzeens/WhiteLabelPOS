import Foundation

// The shop as one document (SPEC section 2). The same JSON is the save file and the backup file, so a backup from
// the Android app opens here and the other way round. Missing keys get their defaults and unknown keys are ignored.

extension KeyedDecodingContainer {
    /// The value for `key`, or `fallback` when it is missing or has the wrong type.
    func v<T: Decodable>(_ key: Key, _ fallback: T) -> T {
        (try? decodeIfPresent(T.self, forKey: key)) ?? fallback
    }
}

public struct Brand: Codable, Equatable, Sendable {
    public var name = ""
    public var tagline = ""
    /// Base64 PNG (max 512×512) or "" to show the monogram.
    public var logo = ""
    public var primary = "#2563EB"
    public var accent = "#F59E0B"
    public var design = "fresh"

    public init() {}

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        let d = Brand()
        name = c.v(.name, d.name)
        tagline = c.v(.tagline, d.tagline)
        logo = c.v(.logo, d.logo)
        primary = c.v(.primary, d.primary)
        accent = c.v(.accent, d.accent)
        design = c.v(.design, d.design)
    }
}

public struct Business: Codable, Equatable, Sendable {
    public var type = "cafe"
    public var phone = ""
    public var address = ""
    public var regNo = ""
    public var currency = "MYR"
    public var taxName = "SST"
    /// Basis points: 600 = 6 %.
    public var taxRate = 0
    public var taxInclusive = false
    /// Cash totals round to this step in the smallest unit; 0 = off.
    public var cashRoundTo = 5
    public var footer = "Thank you!"

    public init() {}

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        let d = Business()
        type = c.v(.type, d.type)
        phone = c.v(.phone, d.phone)
        address = c.v(.address, d.address)
        regNo = c.v(.regNo, d.regNo)
        currency = c.v(.currency, d.currency)
        taxName = c.v(.taxName, d.taxName)
        taxRate = c.v(.taxRate, d.taxRate)
        taxInclusive = c.v(.taxInclusive, d.taxInclusive)
        cashRoundTo = c.v(.cashRoundTo, d.cashRoundTo)
        footer = c.v(.footer, d.footer)
    }
}

public struct ProductCategory: Codable, Equatable, Identifiable, Hashable, Sendable {
    public var id = ""
    public var name = ""

    public init(id: String, name: String) {
        self.id = id
        self.name = name
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = c.v(.id, "")
        name = c.v(.name, "")
    }
}

public struct Product: Codable, Equatable, Identifiable, Hashable, Sendable {
    public var id = ""
    public var name = ""
    /// Smallest unit (sen, cents).
    public var price = 0
    /// Category id, "" = no category.
    public var category = ""
    /// An emoji, or "" to show the initials.
    public var icon = ""
    public var color = "#64748B"
    public var sku = ""
    public var trackStock = false
    public var stock = 0

    public init(id: String = "", name: String = "", price: Int = 0, category: String = "", icon: String = "",
                color: String = "#64748B", sku: String = "", trackStock: Bool = false, stock: Int = 0) {
        self.id = id
        self.name = name
        self.price = price
        self.category = category
        self.icon = icon
        self.color = color
        self.sku = sku
        self.trackStock = trackStock
        self.stock = stock
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        let d = Product()
        id = c.v(.id, d.id)
        name = c.v(.name, d.name)
        price = c.v(.price, d.price)
        category = c.v(.category, d.category)
        icon = c.v(.icon, d.icon)
        color = c.v(.color, d.color)
        sku = c.v(.sku, d.sku)
        trackStock = c.v(.trackStock, d.trackStock)
        stock = c.v(.stock, d.stock)
    }

    /// Stock is tracked and nothing is left.
    public var soldOut: Bool { trackStock && stock <= 0 }
}

public enum PayMethod: String, Codable, CaseIterable, Identifiable, Sendable {
    case cash, card, qr, ewallet

    public var id: String { rawValue }

    public var label: String {
        switch self {
        case .cash: return "Cash"
        case .card: return "Card"
        case .qr: return "QR pay"
        case .ewallet: return "E-wallet"
        }
    }
}

public enum DiscountType: String, Codable, CaseIterable, Sendable {
    case none, amount, percent
}

public struct SaleLine: Codable, Equatable, Hashable, Sendable {
    public var product = ""
    public var name = ""
    public var price = 0
    public var qty = 0

    public init(product: String, name: String, price: Int, qty: Int) {
        self.product = product
        self.name = name
        self.price = price
        self.qty = qty
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        product = c.v(.product, "")
        name = c.v(.name, "")
        price = c.v(.price, 0)
        qty = c.v(.qty, 0)
    }
}

public struct Sale: Codable, Equatable, Identifiable, Hashable, Sendable {
    public var id = ""
    public var number = 0
    /// Milliseconds since 1970 (UTC).
    public var time = 0
    public var lines: [SaleLine] = []
    public var discountType = DiscountType.none
    public var discountValue = 0
    public var subtotal = 0
    public var discount = 0
    public var tax = 0
    public var total = 0
    public var rounding = 0
    public var due = 0
    public var taxName = ""
    public var taxRate = 0
    public var taxInclusive = false
    public var method = PayMethod.cash
    public var paid = 0
    public var change = 0
    public var note = ""
    public var refunded = false
    public var refundTime = 0

    public init() {}

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        let d = Sale()
        id = c.v(.id, d.id)
        number = c.v(.number, d.number)
        time = c.v(.time, d.time)
        lines = c.v(.lines, d.lines)
        discountType = c.v(.discountType, d.discountType)
        discountValue = c.v(.discountValue, d.discountValue)
        subtotal = c.v(.subtotal, d.subtotal)
        discount = c.v(.discount, d.discount)
        tax = c.v(.tax, d.tax)
        total = c.v(.total, d.total)
        rounding = c.v(.rounding, d.rounding)
        due = c.v(.due, d.due)
        taxName = c.v(.taxName, d.taxName)
        taxRate = c.v(.taxRate, d.taxRate)
        taxInclusive = c.v(.taxInclusive, d.taxInclusive)
        method = c.v(.method, d.method)
        paid = c.v(.paid, d.paid)
        change = c.v(.change, d.change)
        note = c.v(.note, d.note)
        refunded = c.v(.refunded, d.refunded)
        refundTime = c.v(.refundTime, d.refundTime)
    }

    public var itemCount: Int { lines.reduce(0) { $0 + $1.qty } }
    public var date: Date { Date(timeIntervalSince1970: Double(time) / 1000) }
}

public struct Doc: Codable, Equatable, Sendable {
    public var format = "whitelabel-pos"
    public var version = 1
    public var setupDone = false
    public var brand = Brand()
    public var business = Business()
    /// "" or 4 digits.
    public var pin = ""
    public var categories: [ProductCategory] = []
    public var products: [Product] = []
    public var sales: [Sale] = []
    public var nextNumber = 1001
    public var nextId = 100

    public init() {}

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        let d = Doc()
        format = c.v(.format, "")
        version = c.v(.version, d.version)
        setupDone = c.v(.setupDone, d.setupDone)
        brand = c.v(.brand, d.brand)
        business = c.v(.business, d.business)
        pin = c.v(.pin, d.pin)
        categories = c.v(.categories, d.categories)
        products = c.v(.products, d.products)
        sales = c.v(.sales, d.sales)
        nextNumber = c.v(.nextNumber, d.nextNumber)
        nextId = c.v(.nextId, d.nextId)
    }
}

public enum DocFileError: Error {
    case notABackup
}

/// Reading and writing the shop file / backup.
public enum DocFile {
    public static func read(_ data: Data) throws -> Doc {
        let doc = try JSONDecoder().decode(Doc.self, from: data)
        guard doc.format == "whitelabel-pos" else { throw DocFileError.notABackup }
        return doc
    }

    public static func write(_ doc: Doc) -> Data {
        let e = JSONEncoder()
        e.outputFormatting = [.prettyPrinted, .sortedKeys, .withoutEscapingSlashes]
        return (try? e.encode(doc)) ?? Data()
    }
}
