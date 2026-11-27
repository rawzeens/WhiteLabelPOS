import Foundation

/// Showing, typing and adding up money (SPEC section 3). Amounts are whole numbers in the smallest unit.
public enum Money {
    /// Round-half-up of a / b for a >= 0.
    public static func roundDiv(_ a: Int, _ b: Int) -> Int {
        (2 * a + b) / (2 * b)
    }

    static func pow10(_ n: Int) -> Int {
        var v = 1
        for _ in 0..<max(0, n) { v *= 10 }
        return v
    }

    /// "RM 123.45", "$1,000.00", "Rp 1,234,567", "-RM 2.50".
    public static func format(_ amount: Int, symbol: String, decimals: Int) -> String {
        let negative = amount < 0
        let a = abs(amount)
        let p = pow10(decimals)
        let whole = String(a / p)
        var grouped = ""
        for (i, ch) in whole.enumerated() {
            if i > 0 && (whole.count - i) % 3 == 0 { grouped.append(",") }
            grouped.append(ch)
        }
        if decimals > 0 {
            var frac = String(a % p)
            while frac.count < decimals { frac = "0" + frac }
            grouped += "." + frac
        }
        let space = (symbol.last?.isLetter ?? false) ? " " : ""
        return (negative ? "-" : "") + symbol + space + grouped
    }

    /// The amount typed by a person ("12.50", "RM 1,234.56"), or nil when it is not a number.
    public static func parse(_ text: String, decimals: Int) -> Int? {
        let kept = text.filter { $0.isASCII && ($0.isNumber || $0 == ".") }
        let parts = kept.split(separator: ".", omittingEmptySubsequences: false)
        guard parts.count <= 2 else { return nil }
        let intPart = String(parts.first ?? "")
        let fracPart = parts.count > 1 ? String(parts[1]) : ""
        guard !(intPart.isEmpty && fracPart.isEmpty) else { return nil }
        guard intPart.count <= 13 else { return nil }
        var value = (Int(intPart.isEmpty ? "0" : intPart) ?? 0) * pow10(decimals)
        let digits = Array(fracPart)
        for i in 0..<decimals {
            let d = i < digits.count ? Int(String(digits[i])) ?? 0 : 0
            value += d * pow10(decimals - 1 - i)
        }
        if digits.count > decimals, let next = Int(String(digits[decimals])), next >= 5 {
            value += 1
        }
        return value
    }

    /// The quick buttons after *Exact* on the cash checkout.
    public static func quickCash(due: Int, decimals: Int) -> [Int] {
        let unit = decimals == 0 ? 1000 : pow10(decimals)
        var out: [Int] = []
        for step in [1, 5, 10, 20, 50, 100] {
            let s = step * unit
            let m = ((due + s - 1) / s) * s
            if m == due || out.contains(m) { continue }
            out.append(m)
            if out.count == 4 { break }
        }
        return out
    }

    /// 600 → "6", 650 → "6.5", 825 → "8.25".
    public static func percentText(_ basisPoints: Int) -> String {
        let whole = basisPoints / 100
        let frac = basisPoints % 100
        if frac == 0 { return String(whole) }
        if frac % 10 == 0 { return "\(whole).\(frac / 10)" }
        return String(format: "%d.%02d", whole, frac)
    }

    /// The next multiple of 1000 that is >= amount (what the demo and screenshots pay in cash).
    public static func nextThousand(_ amount: Int) -> Int {
        ((amount + 999) / 1000) * 1000
    }
}

public struct Totals: Equatable, Sendable {
    public var subtotal = 0
    public var discount = 0
    public var tax = 0
    public var total = 0
    public var rounding = 0
    public var due = 0

    public init() {}

    /// The totals for a sale (SPEC section 3).
    public static func compute(lines: [(price: Int, qty: Int)], discountType: DiscountType, discountValue: Int,
                               taxRate: Int, taxInclusive: Bool, cashRoundTo: Int, method: PayMethod) -> Totals {
        var t = Totals()
        t.subtotal = lines.reduce(0) { $0 + $1.price * $1.qty }
        switch discountType {
        case .amount: t.discount = min(max(discountValue, 0), t.subtotal)
        case .percent: t.discount = min(t.subtotal, Money.roundDiv(t.subtotal * min(max(discountValue, 0), 10000), 10000))
        case .none: t.discount = 0
        }
        let net = t.subtotal - t.discount
        if taxRate <= 0 {
            t.tax = 0
            t.total = net
        } else if taxInclusive {
            t.tax = net - Money.roundDiv(net * 10000, 10000 + taxRate)
            t.total = net
        } else {
            t.tax = Money.roundDiv(net * taxRate, 10000)
            t.total = net + t.tax
        }
        if method == .cash && cashRoundTo > 0 {
            t.rounding = ((t.total + cashRoundTo / 2) / cashRoundTo) * cashRoundTo - t.total
        }
        t.due = t.total + t.rounding
        return t
    }
}
