import Foundation

/// One product in the sale being rung up.
public struct CartLine: Equatable, Hashable, Sendable {
    public var product: String
    public var qty: Int

    public init(product: String, qty: Int) {
        self.product = product
        self.qty = qty
    }
}

/// The sale being rung up right now.
public struct Cart: Equatable, Sendable {
    public var lines: [CartLine] = []
    public var discountType = DiscountType.none
    public var discountValue = 0
    public var note = ""

    public init() {}

    public var isEmpty: Bool { lines.isEmpty }
    public var count: Int { lines.reduce(0) { $0 + $1.qty } }

    public func qty(of product: String) -> Int {
        lines.first { $0.product == product }?.qty ?? 0
    }

    public mutating func add(_ product: String, qty: Int = 1) {
        if let i = lines.firstIndex(where: { $0.product == product }) {
            lines[i].qty += qty
        } else {
            lines.append(CartLine(product: product, qty: qty))
        }
    }

    /// Changes the amount; 0 or less removes the line.
    public mutating func set(_ product: String, qty: Int) {
        guard let i = lines.firstIndex(where: { $0.product == product }) else {
            if qty > 0 { add(product, qty: qty) }
            return
        }
        if qty <= 0 { lines.remove(at: i) } else { lines[i].qty = qty }
    }

    public mutating func clear() {
        self = Cart()
    }
}

public extension Doc {
    func product(_ id: String) -> Product? {
        products.first { $0.id == id }
    }

    func categoryName(_ id: String) -> String? {
        categories.first { $0.id == id }?.name
    }

    func sale(_ id: String) -> Sale? {
        sales.first { $0.id == id }
    }

    /// A new id like "p-101".
    mutating func makeId(_ prefix: String) -> String {
        let id = "\(prefix)-\(nextId)"
        nextId += 1
        return id
    }

    /// The cart as receipt lines, with today's names and prices. Lines whose product was deleted are left out.
    func saleLines(_ cart: Cart) -> [SaleLine] {
        cart.lines.compactMap { l in
            product(l.product).map { SaleLine(product: $0.id, name: $0.name, price: $0.price, qty: l.qty) }
        }
    }

    func totals(_ cart: Cart, method: PayMethod) -> Totals {
        Totals.compute(lines: saleLines(cart).map { ($0.price, $0.qty) }, discountType: cart.discountType,
                       discountValue: cart.discountValue, taxRate: business.taxRate, taxInclusive: business.taxInclusive,
                       cashRoundTo: business.cashRoundTo, method: method)
    }

    /// Whether one more of `p` can go into the cart (sold-out products and stock limits).
    func canAddOne(_ p: Product, cart: Cart) -> Bool {
        !p.trackStock || cart.qty(of: p.id) < p.stock
    }

    /// Saves the sale, lowers stock and returns it. `paid` is only used for cash.
    @discardableResult
    mutating func completeSale(_ cart: Cart, method: PayMethod, paid: Int, time: Int) -> Sale {
        let lines = saleLines(cart)
        let t = totals(cart, method: method)
        var s = Sale()
        s.number = nextNumber
        s.id = "s-\(nextNumber)"
        nextNumber += 1
        s.time = time
        s.lines = lines
        s.discountType = cart.discountType
        s.discountValue = cart.discountValue
        s.subtotal = t.subtotal
        s.discount = t.discount
        s.tax = t.tax
        s.total = t.total
        s.rounding = t.rounding
        s.due = t.due
        s.taxName = business.taxName
        s.taxRate = business.taxRate
        s.taxInclusive = business.taxInclusive
        s.method = method
        s.paid = method == .cash ? paid : t.due
        s.change = max(0, s.paid - s.due)
        s.note = cart.note
        changeStock(lines, by: -1)
        sales.append(s)
        return s
    }

    /// Marks a sale refunded and puts its stock back.
    mutating func refund(_ saleId: String, time: Int) {
        guard let i = sales.firstIndex(where: { $0.id == saleId }), !sales[i].refunded else { return }
        sales[i].refunded = true
        sales[i].refundTime = time
        changeStock(sales[i].lines, by: 1)
    }

    private mutating func changeStock(_ lines: [SaleLine], by sign: Int) {
        for l in lines {
            if let i = products.firstIndex(where: { $0.id == l.product }), products[i].trackStock {
                products[i].stock += sign * l.qty
            }
        }
    }

    /// Adds a type's starter categories and products. Prices are written for 2 decimals; ×100 for 0 decimals.
    mutating func addStarterProducts(_ type: BusinessType, decimals: Int) {
        var ids: [String] = []
        for name in type.categories {
            if let existing = categories.first(where: { $0.name == name }) {
                ids.append(existing.id)
            } else {
                let id = makeId("c")
                categories.append(ProductCategory(id: id, name: name))
                ids.append(id)
            }
        }
        for sp in type.products {
            let id = makeId("p")
            products.append(Product(id: id, name: sp.name, price: decimals == 0 ? sp.price * 100 : sp.price,
                                    category: sp.category >= 0 && sp.category < ids.count ? ids[sp.category] : "",
                                    icon: sp.icon, color: sp.color, sku: sp.sku,
                                    trackStock: sp.stock != nil, stock: sp.stock ?? 0))
        }
    }

    /// Removes a category; its products move to "no category".
    mutating func deleteCategory(_ id: String) {
        categories.removeAll { $0.id == id }
        for i in products.indices where products[i].category == id {
            products[i].category = ""
        }
    }

    /// "SST 6%" or "Includes SST 6%" (for the cart and receipts of new sales).
    func taxLabel() -> String {
        Doc.taxLabel(name: business.taxName, rate: business.taxRate, inclusive: business.taxInclusive)
    }

    static func taxLabel(name: String, rate: Int, inclusive: Bool) -> String {
        let n = name.isEmpty ? "Tax" : name
        return (inclusive ? "Includes " : "") + "\(n) \(Money.percentText(rate))%"
    }

    /// Products in the order of their categories (no category last), keeping their own order inside a category.
    func groupedProducts(_ list: [Product]) -> [(category: ProductCategory?, products: [Product])] {
        var out: [(ProductCategory?, [Product])] = []
        for c in categories {
            let ps = list.filter { $0.category == c.id }
            if !ps.isEmpty { out.append((c, ps)) }
        }
        let known = Set(categories.map(\.id))
        let rest = list.filter { !known.contains($0.category) }
        if !rest.isEmpty { out.append((nil, rest)) }
        return out
    }
}
