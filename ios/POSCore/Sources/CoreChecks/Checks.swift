import Foundation
import POSCore

/// Checks the iPhone rules against the shared files in whitelabel/ (the Android tests read the same files).
public struct CheckResult {
    public var passed = 0
    public var failures: [String] = []
}

public enum CoreChecks {
    /// The repository folder (this file is ios/POSCore/Sources/CoreChecks/Checks.swift).
    public static var root: URL {
        var u = URL(fileURLWithPath: #filePath)
        for _ in 0..<5 { u.deleteLastPathComponent() }
        return u
    }

    static func file(_ path: String) throws -> Data {
        try Data(contentsOf: root.appendingPathComponent(path))
    }

    // MARK: - fixture shapes

    struct MoneyCases: Decodable {
        struct Line: Decodable { var price: Int; var qty: Int }
        struct Expect: Decodable {
            var subtotal: Int, discount: Int, tax: Int, total: Int, rounding: Int, due: Int, paid: Int, change: Int
        }
        struct TotalsCase: Decodable {
            var name: String
            var lines: [Line]
            var discountType: String
            var discountValue: Int
            var taxRate: Int
            var taxInclusive: Bool
            var cashRoundTo: Int
            var method: String
            var paid: Int
            var expect: Expect
        }
        struct FormatCase: Decodable { var amount: Int; var symbol: String; var decimals: Int; var expect: String }
        struct ParseCase: Decodable { var text: String; var decimals: Int; var expect: Int }
        var totals: [TotalsCase]
        var format: [FormatCase]
        var parse: [ParseCase]
    }

    struct BackupChecks: Decodable {
        struct Top: Decodable { var name: String; var qty: Int; var amount: Int }
        struct Expect: Decodable {
            var sales: Int, gross: Int, average: Int, tax: Int, discount: Int, refunds: Int, refunded: Int
            var byMethod: [String: Int]
            var top: [Top]
        }
        struct ReportCheck: Decodable { var from: Int; var to: Int; var expect: Expect }
        var brandName: String
        var design: String
        var products: Int
        var categories: Int
        var salesCount: Int
        var nextNumber: Int
        var report: ReportCheck
    }

    // MARK: - running

    public static func runAll() -> CheckResult {
        var r = CheckResult()
        func check(_ ok: Bool, _ what: @autoclosure () -> String) {
            if ok { r.passed += 1 } else { r.failures.append(what()) }
        }
        func eq<T: Equatable>(_ a: T, _ b: T, _ what: String) {
            check(a == b, "\(what): got \(a), expected \(b)")
        }

        // Money: totals, showing and typing.
        do {
            let cases = try JSONDecoder().decode(MoneyCases.self, from: file("whitelabel/fixtures/money-cases.json"))
            check(cases.totals.count >= 10, "money-cases has its totals")
            for c in cases.totals {
                let method = PayMethod(rawValue: c.method) ?? .cash
                let t = Totals.compute(lines: c.lines.map { ($0.price, $0.qty) },
                                       discountType: DiscountType(rawValue: c.discountType) ?? .none,
                                       discountValue: c.discountValue, taxRate: c.taxRate, taxInclusive: c.taxInclusive,
                                       cashRoundTo: c.cashRoundTo, method: method)
                let paid = method == .cash ? c.paid : t.due
                eq(t.subtotal, c.expect.subtotal, "\(c.name) subtotal")
                eq(t.discount, c.expect.discount, "\(c.name) discount")
                eq(t.tax, c.expect.tax, "\(c.name) tax")
                eq(t.total, c.expect.total, "\(c.name) total")
                eq(t.rounding, c.expect.rounding, "\(c.name) rounding")
                eq(t.due, c.expect.due, "\(c.name) due")
                eq(paid, c.expect.paid, "\(c.name) paid")
                eq(max(0, paid - t.due), c.expect.change, "\(c.name) change")
            }
            for f in cases.format {
                eq(Money.format(f.amount, symbol: f.symbol, decimals: f.decimals), f.expect, "format \(f.amount) \(f.symbol)")
            }
            for p in cases.parse {
                eq(Money.parse(p.text, decimals: p.decimals) ?? -1, p.expect, "parse '\(p.text)'")
            }
        } catch {
            r.failures.append("money-cases.json could not be read: \(error)")
        }

        // Cash quick buttons, tax label and percent text.
        eq(Money.quickCash(due: 2620, decimals: 2), [2700, 3000, 4000, 5000], "quick cash RM 26.20")
        eq(Money.quickCash(due: 1000, decimals: 2), [2000, 5000, 10000], "quick cash RM 10.00")
        eq(Money.quickCash(due: 75480, decimals: 0), [76000, 80000, 100000], "quick cash Rp 75,480")
        eq(Money.percentText(600), "6", "percent 600")
        eq(Money.percentText(650), "6.5", "percent 650")
        eq(Money.percentText(825), "8.25", "percent 825")
        eq(Doc.taxLabel(name: "SST", rate: 600, inclusive: true), "Includes SST 6%", "tax label")

        // Colours and initials.
        eq(Palette.on("#FFFFFF"), "#111111", "text on white")
        eq(Palette.on("#000000"), "#FFFFFF", "text on black")
        eq(Palette.on("#6F4E37"), "#FFFFFF", "text on coffee brown")
        eq(Palette.on("#FCBF49"), "#111111", "text on yellow")
        eq(Palette.soft("#FF0000", page: "#FFFFFF", dark: false), "#FFDBDB", "soft red on white")
        eq(Palette.soft("#FFFFFF", page: "#000000", dark: true), "#383838", "soft white on black")
        eq(Palette.normalized("abc123") ?? "", "#ABC123", "colour typed without #")
        check(Palette.normalized("#12345") == nil, "short colour is refused")
        eq(Monogram.of("Kopi Pagi"), "KP", "monogram Kopi Pagi")
        eq(Monogram.of("Lily & Co"), "LC", "monogram Lily & Co")
        eq(Monogram.of("Segar"), "S", "monogram one word")
        eq(Monogram.of("Sugar (1 kg)"), "SK", "monogram with brackets")
        eq(Monogram.of("  "), "?", "monogram empty")
        eq(Monogram.of("7 Eleven"), "7E", "monogram digit")

        // The shared files in whitelabel/app.
        var config = Config.fallback
        do {
            config = try Config.load(designs: file("whitelabel/app/designs.json"),
                                     catalogs: file("whitelabel/app/catalogs.json"),
                                     currencies: file("whitelabel/app/currencies.json"))
        } catch {
            r.failures.append("whitelabel/app files could not be read: \(error)")
        }
        eq(config.designs.map(\.id), ["fresh", "midnight", "cafe", "street", "atelier"], "the 5 designs")
        let allowed: [String: Set<String>] = [
            "font": ["rounded", "sans", "serif", "condensed", "light"],
            "header": ["plain", "glow", "centered", "bar", "minimal"],
            "products": ["tiles", "glow", "menu", "blocks", "lines"],
            "buttons": ["rounded", "pill", "square"],
            "logoShape": ["circle", "rounded", "ring", "square", "outline"],
            "chips": ["soft", "outline", "pill", "solid", "underline"],
        ]
        for d in config.designs {
            for (k, v) in [("page", d.page), ("surface", d.surface), ("raised", d.raised), ("ink", d.ink), ("muted", d.muted), ("line", d.line)] {
                check(Palette.isValid(v), "\(d.id).\(k) is a colour")
            }
            for (k, v) in [("font", d.font), ("header", d.header), ("products", d.products), ("buttons", d.buttons), ("logoShape", d.logoShape), ("chips", d.chips)] {
                check(allowed[k]?.contains(v) ?? false, "\(d.id).\(k) '\(v)' is known")
            }
            check(d.columns >= 1 && d.radius >= 0, "\(d.id) columns and radius")
            check(d.dark == (Palette.luminance(d.page) < 0.2), "\(d.id) dark matches its page colour")
        }
        eq(config.design("nope").id, "fresh", "unknown design falls back to Fresh")
        eq(config.types.map(\.id), ["cafe", "food", "retail", "grocery", "beauty"], "the 5 business types")
        eq(Set(config.types.map(\.demo.design)), Set(config.designs.map(\.id)), "each demo shop shows a different design")
        for t in config.types {
            check(!t.demo.name.isEmpty && Palette.isValid(t.demo.primary) && Palette.isValid(t.demo.accent), "\(t.id) demo brand")
            check(t.products.count >= 10, "\(t.id) has starter products")
            for p in t.products {
                check(p.price > 0 && p.category >= 0 && p.category < t.categories.count && Palette.isValid(p.color),
                      "\(t.id) product \(p.name)")
            }
        }
        let myr = config.currency("MYR")
        check(myr.symbol == "RM" && myr.decimals == 2 && myr.cashRoundTo == 5 && myr.taxName == "SST", "MYR currency")
        eq(config.currency("IDR").decimals, 0, "IDR has no decimals")
        eq(config.currency("XYZ").code, "MYR", "unknown currency falls back to MYR")

        // The sample backup.
        do {
            let data = try file("whitelabel/fixtures/backup-sample.json")
            let doc = try DocFile.read(data)
            let x = try JSONDecoder().decode(BackupChecks.self, from: file("whitelabel/fixtures/backup-checks.json"))
            eq(doc.brand.name, x.brandName, "backup brand name")
            eq(doc.brand.design, x.design, "backup design")
            eq(doc.products.count, x.products, "backup products")
            eq(doc.categories.count, x.categories, "backup categories")
            eq(doc.sales.count, x.salesCount, "backup sales")
            eq(doc.nextNumber, x.nextNumber, "backup next number")
            check(doc.setupDone, "backup is set up")
            for s in doc.sales {
                let t = Totals.compute(lines: s.lines.map { ($0.price, $0.qty) }, discountType: s.discountType,
                                       discountValue: s.discountValue, taxRate: s.taxRate, taxInclusive: s.taxInclusive,
                                       cashRoundTo: doc.business.cashRoundTo, method: s.method)
                check(t.subtotal == s.subtotal && t.discount == s.discount && t.tax == s.tax && t.total == s.total
                      && t.rounding == s.rounding && t.due == s.due && s.change == max(0, s.paid - s.due),
                      "backup sale #\(s.number) adds up")
            }
            let rep = Report.make(doc.sales, from: x.report.from, to: x.report.to)
            let e = x.report.expect
            eq(rep.sales, e.sales, "report sales")
            eq(rep.gross, e.gross, "report gross")
            eq(rep.average, e.average, "report average")
            eq(rep.tax, e.tax, "report tax")
            eq(rep.discount, e.discount, "report discount")
            eq(rep.refunds, e.refunds, "report refunds")
            eq(rep.refunded, e.refunded, "report refunded")
            eq(rep.byMethod, e.byMethod, "report by method")
            eq(rep.top.map(\.name), e.top.map(\.name), "report top names")
            eq(rep.top.map(\.qty), e.top.map(\.qty), "report top qty")
            eq(rep.top.map(\.amount), e.top.map(\.amount), "report top amounts")

            // Writing and reading back gives the same shop.
            let again = try DocFile.read(DocFile.write(doc))
            eq(again, doc, "backup survives writing and reading")

            // Selling, stock and refunds on the sample shop.
            var shop = doc
            guard let croissant = shop.product("p-11") else { throw DocFileError.notABackup }
            var cart = Cart()
            cart.add("p-11", qty: 2)
            cart.add("p-3")
            check(shop.canAddOne(croissant, cart: cart), "can add a 3rd croissant (6 in stock)")
            var full = Cart()
            full.add("p-11", qty: 6)
            check(!shop.canAddOne(croissant, cart: full), "can't add a 7th croissant")
            let sale = shop.completeSale(cart, method: .cash, paid: 5000, time: 1763700000000)
            eq(sale.number, 1006, "new sale number")
            eq(shop.nextNumber, 1007, "next number moves on")
            eq(sale.subtotal, 2900, "new sale subtotal")
            eq(sale.tax, 174, "new sale tax")
            eq(sale.due, 3075, "new sale due (3074 rounds to 3075)")
            eq(sale.change, 1925, "new sale change")
            eq(shop.product("p-11")?.stock, 4, "stock goes down")
            eq(shop.product("p-3")?.stock, 0, "untracked stock stays")
            shop.refund(sale.id, time: 1763700100000)
            eq(shop.product("p-11")?.stock, 6, "refund puts stock back")
            check(shop.sale(sale.id)?.refunded == true, "sale is refunded")
            shop.refund(sale.id, time: 1763700200000)
            eq(shop.product("p-11")?.stock, 6, "refunding twice does nothing")
            shop.deleteCategory("c-2")
            check(shop.product("p-11")?.category == "" && shop.categories.count == 1, "deleting a category keeps its products")
        } catch {
            r.failures.append("backup-sample.json could not be checked: \(error)")
        }

        // Missing keys get defaults; other files are refused.
        do {
            let d = try DocFile.read(Data(#"{"format":"whitelabel-pos","brand":{"name":"X"},"extra":1}"#.utf8))
            check(!d.setupDone && d.brand.name == "X" && d.brand.design == "fresh" && d.business.currency == "MYR"
                  && d.business.cashRoundTo == 5 && d.nextNumber == 1001 && d.products.isEmpty, "missing keys get defaults")
        } catch {
            r.failures.append("a short backup could not be read: \(error)")
        }
        check((try? DocFile.read(Data(#"{"hello":1}"#.utf8))) == nil, "a file that is not a backup is refused")

        // Starter products.
        if let cafe = config.type("cafe") {
            var d = Doc()
            d.addStarterProducts(cafe, decimals: 2)
            eq(d.products.count, cafe.products.count, "cafe starter products")
            eq(d.categories.map(\.name), cafe.categories, "cafe starter categories")
            eq(d.products.first?.price, 650, "starter price")
            eq(d.products.first?.category, d.categories.first?.id, "starter category id")
            var idr = Doc()
            idr.addStarterProducts(cafe, decimals: 0)
            eq(idr.products.first?.price, 65000, "starter price ×100 with no decimals")
        }
        if let retail = config.type("retail") {
            var d = Doc()
            d.addStarterProducts(retail, decimals: 2)
            check(d.products.allSatisfy(\.trackStock) && d.products.contains { $0.soldOut }, "retail stock is tracked")
        }

        // Demo shops: the same on every run, and every sale adds up.
        var n = Demo.Numbers()
        eq([n.next(), n.next(), n.next()], [1250496027, 1116302264, 1000676753], "the demo number sequence")
        var utc = Calendar(identifier: .gregorian)
        utc.timeZone = TimeZone(identifier: "UTC")!
        let now = Date(timeIntervalSince1970: 1764244800) // 2026-11-27 12:00 UTC
        for t in config.types {
            let a = Demo.shop(t, now: now, calendar: utc)
            let b = Demo.shop(t, now: now, calendar: utc)
            eq(a, b, "\(t.id) demo is the same every time")
            check(a.setupDone && a.brand.name == t.demo.name && a.brand.design == t.demo.design, "\(t.id) demo brand")
            check(!a.sales.isEmpty && a.sales.allSatisfy { $0.time <= 1764244800000 }, "\(t.id) demo sales are in the past")
            check(zip(a.sales, a.sales.dropFirst()).allSatisfy { $0.time <= $1.time }, "\(t.id) demo sales are in time order")
            eq(a.sales.map(\.number), Array(1001..<(1001 + a.sales.count)), "\(t.id) demo numbers")
            eq(a.nextNumber, 1001 + a.sales.count, "\(t.id) demo next number")
            check(a.sales.allSatisfy { s in
                let x = Totals.compute(lines: s.lines.map { ($0.price, $0.qty) }, discountType: s.discountType,
                                       discountValue: s.discountValue, taxRate: 600, taxInclusive: false, cashRoundTo: 5, method: s.method)
                return x.due == s.due && s.paid >= s.due && (s.method != .cash || s.paid % 1000 == 0)
            }, "\(t.id) demo sales add up")
        }
        if let cafe = config.type("cafe") {
            let a = Demo.shop(cafe, now: now, calendar: utc)
            let rep = Report.make(a.sales, from: 0, to: Int.max)
            print("demo cafe @ 2026-11-27 12:00 UTC: \(a.sales.count) sales, \(rep.refunds) refunded, gross \(rep.gross)")
        }

        // Chart buckets.
        if let cafe = config.type("cafe") {
            let a = Demo.shop(cafe, now: now, calendar: utc)
            let hours = ReportChart.hourly(a.sales, day: now, calendar: utc)
            eq(hours.count, 18, "18 hourly bars")
            let todayStart = Int(utc.startOfDay(for: now).timeIntervalSince1970 * 1000)
            let todayGross = Report.make(a.sales, from: todayStart, to: todayStart + 86400000).gross
            eq(hours.reduce(0) { $0 + $1.amount }, todayGross, "hourly bars add up to today")
            let from = utc.date(byAdding: .day, value: -6, to: utc.startOfDay(for: now))!
            let days = ReportChart.daily(a.sales, from: from, days: 7, calendar: utc)
            eq(days.count, 7, "7 daily bars")
            let fromMs = Int(from.timeIntervalSince1970 * 1000)
            eq(days.reduce(0) { $0 + $1.amount }, Report.make(a.sales, from: fromMs, to: fromMs + 7 * 86400000).gross, "daily bars add up")
        }

        return r
    }
}
