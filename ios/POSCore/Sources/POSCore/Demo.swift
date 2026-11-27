import Foundation

/// The demo shops behind "Try a demo shop" (SPEC section 8). The made-up sales use the same number sequence as the
/// Android app, so both give the same sales for the same day and time.
public enum Demo {
    /// The SPEC's number sequence: state = (state × 1103515245 + 12345) mod 2^31, starting at 42.
    public struct Numbers {
        public private(set) var state: Int64 = 42

        public init() {}

        public mutating func next() -> Int64 {
            state = (state * 1103515245 + 12345) % 2147483648
            return state
        }

        public mutating func rand(_ n: Int) -> Int {
            Int(next() % Int64(n))
        }
    }

    /// A complete demo shop of `type`: brand, business, starter products and 30 days of sales up to `now`.
    public static func shop(_ type: BusinessType, now: Date, calendar: Calendar) -> Doc {
        var doc = Doc()
        doc.setupDone = true
        doc.brand.name = type.demo.name
        doc.brand.tagline = type.demo.tagline
        doc.brand.logo = ""
        doc.brand.primary = type.demo.primary
        doc.brand.accent = type.demo.accent
        doc.brand.design = type.demo.design
        doc.business.type = type.id
        doc.business.phone = type.demo.phone
        doc.business.address = type.demo.address
        doc.business.regNo = type.demo.regNo
        doc.business.footer = type.demo.footer
        doc.business.currency = "MYR"
        doc.business.taxName = "SST"
        doc.business.taxRate = 600
        doc.business.taxInclusive = false
        doc.business.cashRoundTo = 5
        doc.addStarterProducts(type, decimals: 2)
        doc.sales = sales(doc, now: now, calendar: calendar)
        doc.nextNumber = 1001 + doc.sales.count
        return doc
    }

    /// 30 days of made-up sales for the products in `doc`, using its tax and rounding.
    public static func sales(_ doc: Doc, now: Date, calendar: Calendar) -> [Sale] {
        guard !doc.products.isEmpty else { return [] }
        var n = Numbers()
        let nowMs = Int((now.timeIntervalSince1970 * 1000).rounded())
        let today = calendar.startOfDay(for: now)
        var made: [(order: Int, sale: Sale)] = []
        for d in stride(from: 29, through: 0, by: -1) {
            let day = calendar.date(byAdding: .day, value: -d, to: today) ?? today
            let dayMs = Int((day.timeIntervalSince1970 * 1000).rounded())
            let count = 5 + n.rand(8)
            for _ in 0..<count {
                let minute = 480 + n.rand(780)
                let lineCount = 1 + n.rand(3)
                var lines: [SaleLine] = []
                for _ in 0..<lineCount {
                    let p = doc.products[n.rand(doc.products.count)]
                    let qty = 1 + n.rand(2)
                    if let i = lines.firstIndex(where: { $0.product == p.id }) {
                        lines[i].qty += qty
                    } else {
                        lines.append(SaleLine(product: p.id, name: p.name, price: p.price, qty: qty))
                    }
                }
                let m = n.rand(10)
                let discounted = n.rand(12) == 0
                let refunded = n.rand(40) == 0
                let time = dayMs + minute * 60000
                if time > nowMs { continue }
                let method: PayMethod = m <= 3 ? .cash : m <= 6 ? .card : m <= 8 ? .qr : .ewallet
                var s = Sale()
                s.time = time
                s.lines = lines
                s.discountType = discounted ? .percent : .none
                s.discountValue = discounted ? 1000 : 0
                let t = Totals.compute(lines: lines.map { ($0.price, $0.qty) }, discountType: s.discountType,
                                       discountValue: s.discountValue, taxRate: doc.business.taxRate,
                                       taxInclusive: doc.business.taxInclusive, cashRoundTo: doc.business.cashRoundTo,
                                       method: method)
                s.subtotal = t.subtotal
                s.discount = t.discount
                s.tax = t.tax
                s.total = t.total
                s.rounding = t.rounding
                s.due = t.due
                s.taxName = doc.business.taxName
                s.taxRate = doc.business.taxRate
                s.taxInclusive = doc.business.taxInclusive
                s.method = method
                s.paid = method == .cash ? Money.nextThousand(t.due) : t.due
                s.change = max(0, s.paid - s.due)
                s.refunded = refunded
                s.refundTime = refunded ? time + 30 * 60000 : 0
                made.append((made.count, s))
            }
        }
        made.sort { $0.sale.time != $1.sale.time ? $0.sale.time < $1.sale.time : $0.order < $1.order }
        return made.enumerated().map { i, item in
            var s = item.sale
            s.number = 1001 + i
            s.id = "s-\(s.number)"
            return s
        }
    }

    /// Puts products 0 × 2, 4 × 1 and 9 × 1 (those that exist) in a cart, for the screenshot switches.
    public static func sampleCart(_ doc: Doc) -> Cart {
        var c = Cart()
        for (i, q) in [(0, 2), (4, 1), (9, 1)] where i < doc.products.count {
            c.add(doc.products[i].id, qty: q)
        }
        return c
    }
}
