import Foundation

// The files in whitelabel/app: the 5 design environments, the business types with their starter products and demo
// shops, and the currencies. The app reads them from its bundle; the checks read them from the repository.

public struct Design: Codable, Equatable, Identifiable, Sendable {
    public var id = "fresh"
    public var name = "Fresh"
    public var about = ""
    public var dark = false
    public var page = "#F3F5F8"
    public var surface = "#FFFFFF"
    public var raised = "#E9EDF2"
    public var ink = "#101828"
    public var muted = "#667085"
    public var line = "#E4E7EC"
    public var radius = 18.0
    /// rounded | sans | serif | condensed | light
    public var font = "rounded"
    public var caps = false
    /// Letter spacing in em.
    public var tracking = 0.0
    /// plain | glow | centered | bar | minimal
    public var header = "plain"
    /// tiles | glow | menu | blocks | lines
    public var products = "tiles"
    public var columns = 3
    /// rounded | pill | square
    public var buttons = "rounded"
    /// circle | rounded | ring | square | outline
    public var logoShape = "circle"
    /// soft | outline | pill | solid | underline
    public var chips = "soft"

    public init() {}

    public static let fallback = Design()

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        let d = Design()
        id = c.v(.id, d.id)
        name = c.v(.name, d.name)
        about = c.v(.about, d.about)
        dark = c.v(.dark, d.dark)
        page = c.v(.page, d.page)
        surface = c.v(.surface, d.surface)
        raised = c.v(.raised, d.raised)
        ink = c.v(.ink, d.ink)
        muted = c.v(.muted, d.muted)
        line = c.v(.line, d.line)
        radius = c.v(.radius, d.radius)
        font = c.v(.font, d.font)
        caps = c.v(.caps, d.caps)
        tracking = c.v(.tracking, d.tracking)
        header = c.v(.header, d.header)
        products = c.v(.products, d.products)
        columns = max(1, c.v(.columns, d.columns))
        buttons = c.v(.buttons, d.buttons)
        logoShape = c.v(.logoShape, d.logoShape)
        chips = c.v(.chips, d.chips)
    }
}

public struct DemoShop: Codable, Equatable, Sendable {
    public var name = ""
    public var tagline = ""
    public var primary = "#2563EB"
    public var accent = "#F59E0B"
    public var design = "fresh"
    public var phone = ""
    public var address = ""
    public var regNo = ""
    public var footer = "Thank you!"

    public init() {}

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        let d = DemoShop()
        name = c.v(.name, d.name)
        tagline = c.v(.tagline, d.tagline)
        primary = c.v(.primary, d.primary)
        accent = c.v(.accent, d.accent)
        design = c.v(.design, d.design)
        phone = c.v(.phone, d.phone)
        address = c.v(.address, d.address)
        regNo = c.v(.regNo, d.regNo)
        footer = c.v(.footer, d.footer)
    }
}

public struct StarterProduct: Codable, Equatable, Sendable {
    public var name = ""
    public var price = 0
    /// Index into the type's categories.
    public var category = 0
    public var icon = ""
    public var color = "#64748B"
    public var sku = ""
    /// Present → stock is tracked.
    public var stock: Int?

    public init() {}

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        name = c.v(.name, "")
        price = c.v(.price, 0)
        category = c.v(.category, 0)
        icon = c.v(.icon, "")
        color = c.v(.color, "#64748B")
        sku = c.v(.sku, "")
        stock = try? c.decodeIfPresent(Int.self, forKey: .stock)
    }
}

public struct BusinessType: Codable, Equatable, Identifiable, Sendable {
    public var id = ""
    public var name = ""
    public var icon = ""
    public var demo = DemoShop()
    public var categories: [String] = []
    public var products: [StarterProduct] = []

    public init() {}

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = c.v(.id, "")
        name = c.v(.name, "")
        icon = c.v(.icon, "")
        demo = c.v(.demo, DemoShop())
        categories = c.v(.categories, [])
        products = c.v(.products, [])
    }
}

public struct Currency: Codable, Equatable, Identifiable, Hashable, Sendable {
    public var code = "MYR"
    public var name = "Malaysian ringgit"
    public var symbol = "RM"
    public var decimals = 2
    public var cashRoundTo = 5
    public var taxName = "SST"

    public var id: String { code }

    public init() {}

    public static let fallback = Currency()

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        let d = Currency()
        code = c.v(.code, d.code)
        name = c.v(.name, d.name)
        symbol = c.v(.symbol, d.symbol)
        decimals = c.v(.decimals, d.decimals)
        cashRoundTo = c.v(.cashRoundTo, d.cashRoundTo)
        taxName = c.v(.taxName, d.taxName)
    }
}

/// Everything from whitelabel/app.
public struct Config: Sendable {
    public var designs: [Design]
    public var types: [BusinessType]
    public var currencies: [Currency]

    public init(designs: [Design], types: [BusinessType], currencies: [Currency]) {
        self.designs = designs.isEmpty ? [Design.fallback] : designs
        self.types = types
        self.currencies = currencies.isEmpty ? [Currency.fallback] : currencies
    }

    private struct DesignFile: Decodable { var designs: [Design] }
    private struct CatalogFile: Decodable { var types: [BusinessType] }
    private struct CurrencyFile: Decodable { var currencies: [Currency] }

    public static func load(designs: Data, catalogs: Data, currencies: Data) throws -> Config {
        let d = JSONDecoder()
        return Config(designs: try d.decode(DesignFile.self, from: designs).designs,
                      types: try d.decode(CatalogFile.self, from: catalogs).types,
                      currencies: try d.decode(CurrencyFile.self, from: currencies).currencies)
    }

    public static let fallback = Config(designs: [Design.fallback], types: [], currencies: [Currency.fallback])

    /// The design with this id; an unknown id gives "fresh" (or the first design).
    public func design(_ id: String) -> Design {
        designs.first { $0.id == id } ?? designs.first { $0.id == "fresh" } ?? designs[0]
    }

    public func type(_ id: String) -> BusinessType? {
        types.first { $0.id == id }
    }

    public func currency(_ code: String) -> Currency {
        currencies.first { $0.code == code } ?? currencies.first { $0.code == "MYR" } ?? currencies[0]
    }
}
