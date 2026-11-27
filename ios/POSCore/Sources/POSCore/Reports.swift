import Foundation

public struct TopItem: Equatable, Sendable {
    public var name: String
    public var qty: Int
    public var amount: Int
}

/// What a time range adds up to (SPEC section 4). Uses the stored sale values; old sales are never recomputed.
public struct Report: Equatable, Sendable {
    public var sales = 0
    public var gross = 0
    public var average = 0
    public var tax = 0
    public var discount = 0
    public var refunds = 0
    public var refunded = 0
    /// Σ due per method, only methods that appear.
    public var byMethod: [String: Int] = [:]
    /// All products sold, best first (screens show 5).
    public var top: [TopItem] = []

    public init() {}

    /// Sales with from <= time < to (milliseconds since 1970).
    public static func make(_ all: [Sale], from: Int, to: Int) -> Report {
        var r = Report()
        var top: [String: (qty: Int, amount: Int)] = [:]
        for s in all where s.time >= from && s.time < to {
            if s.refunded {
                r.refunds += 1
                r.refunded += s.due
                continue
            }
            r.sales += 1
            r.gross += s.due
            r.tax += s.tax
            r.discount += s.discount
            r.byMethod[s.method.rawValue, default: 0] += s.due
            for l in s.lines {
                let old = top[l.name] ?? (0, 0)
                top[l.name] = (old.qty + l.qty, old.amount + l.qty * l.price)
            }
        }
        r.average = r.sales > 0 ? Money.roundDiv(r.gross, r.sales) : 0
        r.top = top.map { TopItem(name: $0.key, qty: $0.value.qty, amount: $0.value.amount) }
            .sorted { a, b in
                if a.qty != b.qty { return a.qty > b.qty }
                if a.amount != b.amount { return a.amount > b.amount }
                return a.name < b.name
            }
        return r
    }

    /// Payment methods in the usual order, with their amounts.
    public var methods: [(method: PayMethod, amount: Int)] {
        PayMethod.allCases.compactMap { m in byMethod[m.rawValue].map { (m, $0) } }
    }
}

public struct Bucket: Identifiable, Equatable, Sendable {
    public var id: Int
    public var start: Date
    public var amount: Int
}

public enum ReportChart {
    static func ms(_ d: Date) -> Int { Int((d.timeIntervalSince1970 * 1000).rounded()) }

    /// One bar per hour, 6:00 to 23:00, for the day that starts at `day`. Sales before 6:00 count in the 6:00 bar.
    public static func hourly(_ sales: [Sale], day: Date, calendar: Calendar) -> [Bucket] {
        let start = calendar.startOfDay(for: day)
        guard let end = calendar.date(byAdding: .day, value: 1, to: start) else { return [] }
        var amounts = Array(repeating: 0, count: 18)
        let from = ms(start), to = ms(end)
        for s in sales where !s.refunded && s.time >= from && s.time < to {
            let h = calendar.component(.hour, from: s.date)
            amounts[min(max(h, 6), 23) - 6] += s.due
        }
        return (0..<18).map { i in
            Bucket(id: i, start: calendar.date(byAdding: .hour, value: 6 + i, to: start) ?? start, amount: amounts[i])
        }
    }

    /// One bar per local day for `days` days starting at `from`.
    public static func daily(_ sales: [Sale], from: Date, days: Int, calendar: Calendar) -> [Bucket] {
        let first = calendar.startOfDay(for: from)
        var starts: [Date] = []
        for i in 0...days {
            starts.append(calendar.date(byAdding: .day, value: i, to: first) ?? first)
        }
        var amounts = Array(repeating: 0, count: days)
        let edges = starts.map(ms)
        for s in sales where !s.refunded && s.time >= edges[0] && s.time < edges[days] {
            var i = 0
            while i < days - 1 && s.time >= edges[i + 1] { i += 1 }
            amounts[i] += s.due
        }
        return (0..<days).map { Bucket(id: $0, start: starts[$0], amount: amounts[$0]) }
    }
}
