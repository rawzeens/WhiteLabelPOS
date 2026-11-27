import POSCore
import SwiftUI

/// The till: the shop's header, search, categories and products. Phones show a bar with the sale at the bottom;
/// wide screens keep the sale open on the right.
struct SellView: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    @State private var search = ""
    @State private var category = ""
    @State private var toast: String?

    var body: some View {
        GeometryReader { geo in
            let wide = geo.size.width >= 700
            let cartWidth = min(400, geo.size.width * 0.38)
            HStack(spacing: 0) {
                VStack(spacing: 0) {
                    ShopHeader()
                    controls
                    ScrollView {
                        board(width: (wide ? geo.size.width - cartWidth : geo.size.width) - 32)
                            .padding(16)
                            .padding(.bottom, wide ? 0 : 80)
                    }
                    .scrollDismissesKeyboard(.immediately)
                }
                .overlay(alignment: .bottom) {
                    if !wide { CartBar().padding(12) }
                }
                if wide {
                    Rectangle().fill(look.line).frame(width: 1)
                    CartPanel(embedded: true)
                        .frame(width: cartWidth)
                }
            }
        }
        .background(look.page.ignoresSafeArea())
        .overlay(alignment: .bottom) {
            if let toast { Toast(text: toast) }
        }
        .hideNavBar()
    }

    private var controls: some View {
        VStack(spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass").foregroundStyle(look.muted)
                TextField("Search products", text: $search)
                    .font(look.font(16))
                    .foregroundStyle(look.ink)
                if !search.isEmpty {
                    Button { search = "" } label: {
                        Image(systemName: "xmark.circle.fill").foregroundStyle(look.muted)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(look.raised, in: RoundedRectangle(cornerRadius: look.design.chips == "pill" ? 999 : look.smallRadius, style: .continuous))
            if !store.doc.categories.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: look.design.chips == "underline" ? 14 : 8) {
                        ChipView(title: "All", selected: category.isEmpty) { category = "" }
                        ForEach(store.doc.categories) { c in
                            ChipView(title: c.name, selected: category == c.id) { category = c.id }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 2)
                }
                .padding(.horizontal, -16)
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 12)
        .padding(.bottom, 4)
    }

    private var shown: [Product] {
        let q = search.trimmingCharacters(in: .whitespaces).lowercased()
        return store.doc.products.filter { p in
            (category.isEmpty || p.category == category)
                && (q.isEmpty || p.name.lowercased().contains(q) || p.sku.lowercased().contains(q))
        }
    }

    @ViewBuilder private func board(width: CGFloat) -> some View {
        let list = shown
        if store.doc.products.isEmpty {
            EmptyMessage(icon: "square.grid.2x2", title: "No products yet",
                         message: "Add what you sell in the Products tab.")
        } else if list.isEmpty {
            EmptyMessage(icon: "magnifyingglass", title: "Nothing found", message: "Try another word or category.")
        } else {
            ProductBoard(doc: store.doc, products: list, width: width, money: store.money,
                         qty: { store.cart.qty(of: $0) }, tap: add)
        }
    }

    private func add(_ p: Product) {
        if !store.addToCart(p) {
            show(p.soldOut ? "\(p.name) is sold out" : "Only \(p.stock) \(p.name) left")
        }
    }

    private func show(_ text: String) {
        withAnimation { toast = text }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) {
            withAnimation { if toast == text { toast = nil } }
        }
    }
}

/// The bar at the bottom of the phone till: how many items, the total, and Charge.
struct CartBar: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look

    var body: some View {
        if store.cart.isEmpty {
            LText("Tap a product to start a sale", 14, .medium, color: look.muted)
                .frame(maxWidth: .infinity, minHeight: 50)
                .background(look.surface, in: look.buttonShape)
                .overlay(look.buttonShape.stroke(look.line))
        } else {
            Button { store.sellPath.append(.cart) } label: {
                HStack(spacing: 10) {
                    Text("\(store.cart.count)")
                        .font(look.font(14, .bold))
                        .foregroundStyle(look.onAccent)
                        .frame(minWidth: 28, minHeight: 28)
                        .background(look.accent, in: Circle())
                    LText(store.cart.count == 1 ? "1 item" : "\(store.cart.count) items", 15, .medium, color: look.onPrimary, caps: true)
                    Spacer(minLength: 4)
                    LText(store.money(store.doc.totals(store.cart, method: .card).total), 18, .bold, color: look.onPrimary)
                    LText("Charge", 15, look.buttonWeight, color: look.onPrimary, caps: true)
                    Image(systemName: "chevron.right").font(.system(size: 14, weight: .bold)).foregroundStyle(look.onPrimary)
                }
                .padding(.horizontal, 14)
                .frame(minHeight: 58)
                .background(look.primary, in: look.buttonShape)
                .shadow(color: look.primary.opacity(look.dark ? 0.6 : 0.3), radius: 10, y: 3)
            }
            .buttonStyle(.plain)
        }
    }
}
