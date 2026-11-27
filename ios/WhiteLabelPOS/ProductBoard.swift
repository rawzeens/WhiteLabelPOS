import POSCore
import SwiftUI

/// The products on the Sell screen in the design's layout: tiles, glow, menu, blocks or lines.
struct ProductBoard: View {
    @Environment(\.look) private var look
    let doc: Doc
    let products: [Product]
    let width: CGFloat
    let money: (Int) -> String
    let qty: (String) -> Int
    let tap: (Product) -> Void

    private var gap: CGFloat {
        switch look.design.products {
        case "blocks": return 6
        case "glow": return 12
        default: return 10
        }
    }

    private var columns: Int {
        let base = look.design.columns
        return width >= 600 ? max(base, Int(width / 150)) : base
    }

    var body: some View {
        switch look.design.products {
        case "menu", "lines": lists
        default: grid
        }
    }

    private var grid: some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: gap), count: columns), spacing: gap) {
            ForEach(products) { p in
                Button { tap(p) } label: { cell(p) }
                    .buttonStyle(.plain)
            }
        }
    }

    @ViewBuilder private func cell(_ p: Product) -> some View {
        switch look.design.products {
        case "glow": GlowCell(product: p, price: money(p.price), qty: qty(p.id))
        case "blocks": BlockCell(product: p, price: money(p.price), qty: qty(p.id))
        default: TileCell(product: p, price: money(p.price), qty: qty(p.id))
        }
    }

    /// Menu and lines: one list, or two side by side on wide screens.
    private var lists: some View {
        let groups = doc.groupedProducts(products)
        return Group {
            if width > 900 {
                let halves = split(groups)
                HStack(alignment: .top, spacing: 16) {
                    list(halves.0)
                    list(halves.1)
                }
            } else {
                list(groups)
            }
        }
    }

    private func split(_ groups: [(category: ProductCategory?, products: [Product])]) -> ([(category: ProductCategory?, products: [Product])], [(category: ProductCategory?, products: [Product])]) {
        let total = groups.reduce(0) { $0 + $1.products.count }
        var left: [(category: ProductCategory?, products: [Product])] = []
        var right: [(category: ProductCategory?, products: [Product])] = []
        var count = 0
        for g in groups {
            if count < (total + 1) / 2 {
                left.append(g)
                count += g.products.count
            } else {
                right.append(g)
            }
        }
        return (left, right)
    }

    @ViewBuilder private func list(_ groups: [(category: ProductCategory?, products: [Product])]) -> some View {
        if look.design.products == "menu" {
            MenuList(groups: groups, money: money, qty: qty, tap: tap)
        } else {
            LinesList(doc: doc, groups: groups, money: money, qty: qty, tap: tap)
        }
    }
}

/// Fresh: rounded white tiles, emoji in a soft circle, price in the brand colour.
struct TileCell: View {
    @Environment(\.look) private var look
    let product: Product
    let price: String
    let qty: Int

    var body: some View {
        VStack(spacing: 8) {
            ProductIcon(product: product, size: 54)
            LText(product.name, 14, .semibold, caps: true)
                .lineLimit(2)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity, minHeight: 36, alignment: .top)
            LText(price, 15, .bold, color: look.primary)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity)
        .background(look.surface, in: RoundedRectangle(cornerRadius: look.radius, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: look.radius, style: .continuous)
            .stroke(qty > 0 ? look.primary : look.line, lineWidth: qty > 0 ? 2 : 1))
        .overlay(alignment: .topTrailing) { QtyBadge(qty: qty).padding(6) }
        .soldOut(product.soldOut)
        .contentShape(Rectangle())
    }
}

/// Midnight: dark cards with a stripe of the product colour and a glow when in the sale.
struct GlowCell: View {
    @Environment(\.look) private var look
    let product: Product
    let price: String
    let qty: Int

    var body: some View {
        HStack(spacing: 0) {
            Rectangle().fill(look.color(product.color)).frame(width: 4)
            VStack(alignment: .leading, spacing: 8) {
                HStack(alignment: .top) {
                    ProductIcon(product: product, size: 34, circle: product.icon.isEmpty)
                    Spacer(minLength: 0)
                    QtyBadge(qty: qty)
                }
                LText(product.name, 15, .semibold, caps: true)
                    .lineLimit(2)
                    .frame(maxWidth: .infinity, minHeight: 38, alignment: .topLeading)
                LText(price, 19, .bold, color: look.primary)
            }
            .padding(12)
        }
        .background(look.surface)
        .clipShape(RoundedRectangle(cornerRadius: look.radius, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: look.radius, style: .continuous)
            .stroke(qty > 0 ? look.primary : look.line, lineWidth: qty > 0 ? 1.5 : 1))
        .background(
            // the glow sits on a shape behind the card, so text and emoji stay clean
            RoundedRectangle(cornerRadius: look.radius, style: .continuous)
                .fill(look.surface)
                .shadow(color: qty > 0 ? look.primary.opacity(0.6) : .clear, radius: 12)
        )
        .soldOut(product.soldOut)
        .contentShape(Rectangle())
    }
}

/// Street: big square blocks filled with the product colour.
struct BlockCell: View {
    @Environment(\.look) private var look
    let product: Product
    let price: String
    let qty: Int

    var body: some View {
        let hex = Palette.isValid(product.color) ? product.color : look.primaryHex
        let fg = look.on(hex)
        ZStack(alignment: .topTrailing) {
            look.color(hex)
            Text(product.icon.isEmpty ? Monogram.of(product.name) : product.icon)
                .font(product.icon.isEmpty ? look.font(46, .heavy) : .system(size: 50))
                .foregroundStyle(fg.opacity(0.3))
                .opacity(product.icon.isEmpty ? 1 : 0.4)
                .offset(x: 6, y: -4)
                .padding(6)
            VStack(alignment: .leading, spacing: 4) {
                QtyBadge(qty: qty)
                Spacer(minLength: 0)
                LText(product.name, 18, .heavy, color: fg, caps: true)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                LText(price, 16, .bold, color: fg.opacity(0.92))
            }
            .padding(12)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        }
        .frame(height: 124)
        .clipShape(RoundedRectangle(cornerRadius: look.radius))
        .overlay(RoundedRectangle(cornerRadius: look.radius).stroke(look.ink, lineWidth: qty > 0 ? 4 : 0))
        .soldOut(product.soldOut)
        .contentShape(Rectangle())
    }
}

/// Café: a printed menu, grouped by category, with dotted leaders to the price.
struct MenuList: View {
    @Environment(\.look) private var look
    let groups: [(category: ProductCategory?, products: [Product])]
    let money: (Int) -> String
    let qty: (String) -> Int
    let tap: (Product) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(groups.enumerated()), id: \.offset) { i, g in
                LText(g.category?.name ?? "Other", 21, .bold, color: look.primary, italic: true)
                    .padding(.top, i == 0 ? 4 : 18)
                    .padding(.bottom, 4)
                ForEach(g.products) { p in
                    Button { tap(p) } label: { MenuRow(product: p, price: money(p.price), qty: qty(p.id)) }
                        .buttonStyle(.plain)
                }
            }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(look.surface, in: RoundedRectangle(cornerRadius: look.radius, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: look.radius, style: .continuous).stroke(look.line))
    }
}

struct MenuRow: View {
    @Environment(\.look) private var look
    let product: Product
    let price: String
    let qty: Int

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Text(product.icon.isEmpty ? "•" : product.icon).font(.system(size: 17))
            LText(product.name, 17, .medium).lineLimit(1).layoutPriority(1)
            if qty > 0 {
                LText("×\(qty)", 13, .bold, color: look.onAccent)
                    .padding(.horizontal, 7).padding(.vertical, 2)
                    .background(look.accent, in: Capsule())
                    .layoutPriority(1)
            }
            DashLine().stroke(look.muted.opacity(0.7), style: StrokeStyle(lineWidth: 1.2, lineCap: .round, dash: [0.5, 5]))
                .frame(height: 2)
                .frame(minWidth: 12)
            LText(price, 17, .semibold).layoutPriority(1)
        }
        .padding(.vertical, 9)
        .soldOut(product.soldOut)
        .contentShape(Rectangle())
    }
}

/// Atelier: quiet rows split by hairlines, names in spaced capitals.
struct LinesList: View {
    @Environment(\.look) private var look
    let doc: Doc
    let groups: [(category: ProductCategory?, products: [Product])]
    let money: (Int) -> String
    let qty: (String) -> Int
    let tap: (Product) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(groups.enumerated()), id: \.offset) { _, g in
                ForEach(g.products) { p in
                    Button { tap(p) } label: {
                        LinesRow(product: p, detail: p.sku.isEmpty ? (g.category?.name ?? "") : p.sku,
                                 price: money(p.price), qty: qty(p.id))
                    }
                    .buttonStyle(.plain)
                    Hairline()
                }
            }
        }
        .overlay(alignment: .top) { Hairline() }
    }
}

struct LinesRow: View {
    @Environment(\.look) private var look
    let product: Product
    let detail: String
    let price: String
    let qty: Int

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                LText(product.name, 14, .regular, caps: true).lineLimit(1)
                if !detail.isEmpty {
                    LText(detail, 11, color: look.muted, caps: true).lineLimit(1)
                }
            }
            Spacer(minLength: 8)
            if qty > 0 {
                LText("× \(qty)", 12, .semibold, color: look.onAccent)
                    .padding(.horizontal, 8).padding(.vertical, 3)
                    .background(look.accent, in: Rectangle())
            }
            if product.soldOut {
                LText("Sold out", 11, color: look.muted, caps: true)
            }
            LText(price, 15, .regular)
        }
        .padding(.vertical, 15)
        .padding(.horizontal, 4)
        .opacity(product.soldOut ? 0.45 : 1)
        .contentShape(Rectangle())
    }
}

private struct SoldOutLook: ViewModifier {
    @Environment(\.look) private var look
    let soldOut: Bool

    func body(content: Content) -> some View {
        if soldOut {
            content
                .opacity(0.45)
                .overlay {
                    LText("Sold out", 12, .bold, color: look.page, caps: true)
                        .padding(.horizontal, 10).padding(.vertical, 4)
                        .background(look.ink, in: Capsule())
                }
        } else {
            content
        }
    }
}

extension View {
    func soldOut(_ yes: Bool) -> some View { modifier(SoldOutLook(soldOut: yes)) }
}
