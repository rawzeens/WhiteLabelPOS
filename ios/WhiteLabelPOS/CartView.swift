import POSCore
import SwiftUI

/// The sale being rung up, as its own screen (phones).
struct CartScreen: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look

    var body: some View {
        CartPanel(embedded: false)
            .background(look.page.ignoresSafeArea())
            .navigationTitle("This sale")
            .inlineTitle()
    }
}

/// The lines of the sale, discount, note, totals and the Charge button.
struct CartPanel: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    let embedded: Bool
    @State private var showDiscount = false
    @State private var showNote = false
    @State private var confirmClear = false

    private var totals: Totals { store.doc.totals(store.cart, method: .card) }

    var body: some View {
        VStack(spacing: 0) {
            if embedded {
                HStack {
                    LText("This sale", 20, .bold, caps: true)
                    Spacer()
                    if !store.cart.isEmpty {
                        Button { confirmClear = true } label: { LText("Clear", 15, .medium, color: look.bad, caps: true) }
                            .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 16)
                .padding(.bottom, 8)
            }
            if store.cart.isEmpty {
                Spacer()
                EmptyMessage(icon: "cart", title: "Nothing in this sale",
                             message: "Tap products to add them.")
                Spacer()
            } else {
                ScrollView {
                    VStack(spacing: 0) {
                        Panel(padding: 12) {
                            ForEach(Array(store.doc.saleLines(store.cart).enumerated()), id: \.element.product) { i, line in
                                if i > 0 { Hairline() }
                                CartLineRow(line: line)
                            }
                        }
                        HStack(spacing: 10) {
                            SoftButton(discountTitle, icon: "tag") { showDiscount = true }
                            SoftButton(store.cart.note.isEmpty ? "Add note" : "Edit note", icon: "text.bubble") { showNote = true }
                        }
                        .padding(.top, 12)
                        if !store.cart.note.isEmpty {
                            LText("Note: \(store.cart.note)", 14, color: look.muted)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(.top, 8)
                        }
                        Panel {
                            TotalsBlock(totals: totals)
                        }
                        .padding(.top, 12)
                        if !embedded {
                            Button { confirmClear = true } label: {
                                LText("Clear this sale", 15, .medium, color: look.bad, caps: true)
                            }
                            .buttonStyle(.plain)
                            .padding(.top, 18)
                        }
                    }
                    .padding(16)
                }
                MainButton("Charge \(store.money(totals.total))", icon: "creditcard") {
                    store.sellPath.append(.checkout)
                }
                .padding(16)
            }
        }
        .background(embedded ? look.surface : Color.clear)
        .sheet(isPresented: $showDiscount) { DiscountSheet().environment(\.look, look) }
        .sheet(isPresented: $showNote) { NoteSheet().environment(\.look, look) }
        .confirmationDialog("Clear this sale?", isPresented: $confirmClear, titleVisibility: .visible) {
            Button("Clear sale", role: .destructive) { store.cart.clear() }
        }
    }

    private var discountTitle: String {
        switch store.cart.discountType {
        case .none: return "Discount"
        case .percent: return "\(Money.percentText(store.cart.discountValue))% off"
        case .amount: return "\(store.money(store.cart.discountValue)) off"
        }
    }
}

struct CartLineRow: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    let line: SaleLine

    var body: some View {
        let product = store.doc.product(line.product)
        HStack(spacing: 10) {
            if let product {
                ProductIcon(product: product, size: 38)
            }
            VStack(alignment: .leading, spacing: 2) {
                LText(line.name, 15, .semibold, caps: true).lineLimit(2)
                LText(store.money(line.price), 13, color: look.muted)
            }
            Spacer(minLength: 6)
            HStack(spacing: 0) {
                stepButton("minus") { store.cart.set(line.product, qty: line.qty - 1) }
                LText("\(line.qty)", 16, .bold).frame(minWidth: 30)
                stepButton("plus") {
                    if let p = product, store.doc.canAddOne(p, cart: store.cart) { store.cart.add(p.id) }
                }
                .opacity(product.map { store.doc.canAddOne($0, cart: store.cart) } ?? false ? 1 : 0.35)
            }
            LText(store.money(line.price * line.qty), 15, .semibold)
                .frame(minWidth: 74, alignment: .trailing)
        }
        .padding(.vertical, 8)
    }

    private func stepButton(_ icon: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(look.ink)
                .frame(width: 32, height: 32)
                .background(look.raised, in: RoundedRectangle(cornerRadius: look.design.buttons == "pill" ? 16 : min(look.smallRadius, 8)))
        }
        .buttonStyle(.plain)
    }
}

/// Subtotal, discount, tax and total.
struct TotalsBlock: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    let totals: Totals
    var showRounding = false

    var body: some View {
        VStack(spacing: 2) {
            AmountRow("Subtotal", store.money(totals.subtotal))
            if totals.discount > 0 {
                AmountRow(discountLabel, "-" + store.money(totals.discount), color: look.good)
            }
            if store.doc.business.taxRate > 0 {
                AmountRow(store.doc.taxLabel(), store.money(totals.tax))
            }
            if showRounding && totals.rounding != 0 {
                AmountRow("Cash rounding", (totals.rounding > 0 ? "+" : "") + store.money(totals.rounding))
            }
            Hairline().padding(.vertical, 6)
            AmountRow("Total", store.money(showRounding ? totals.due : totals.total), bold: true, size: 19)
        }
    }

    private var discountLabel: String {
        store.cart.discountType == .percent ? "Discount \(Money.percentText(store.cart.discountValue))%" : "Discount"
    }
}

/// Money off the whole sale, as an amount or a percentage.
struct DiscountSheet: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    @Environment(\.dismiss) private var dismiss
    @State private var percent = true
    @State private var text = ""

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                HStack(spacing: 8) {
                    ChipView(title: "Percent", selected: percent) { percent = true }
                    ChipView(title: "Amount", selected: !percent) { percent = false }
                }
                Labeled(percent ? "Percent off" : "Amount off (\(store.currency.symbol))") {
                    TextField(percent ? "10" : "5.00", text: $text)
                        .decimalKeyboard()
                        .fieldLook()
                }
                if percent {
                    HStack(spacing: 8) {
                        ForEach(["5", "10", "15", "20", "50"], id: \.self) { p in
                            ChipView(title: "\(p)%", selected: text == p) { text = p }
                        }
                    }
                }
                MainButton("Apply discount", enabled: value != nil) {
                    guard let v = value else { return }
                    store.cart.discountType = percent ? .percent : .amount
                    store.cart.discountValue = v
                    dismiss()
                }
                if store.cart.discountType != .none {
                    SoftButton("Remove discount", color: look.bad) {
                        store.cart.discountType = .none
                        store.cart.discountValue = 0
                        dismiss()
                    }
                }
                Spacer()
            }
            .padding(20)
            .background(look.page.ignoresSafeArea())
            .navigationTitle("Discount")
            .inlineTitle()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
            }
        }
        .presentationDetents([.medium, .large])
        .onAppear {
            switch store.cart.discountType {
            case .percent:
                percent = true
                text = Money.percentText(store.cart.discountValue)
            case .amount:
                percent = false
                text = store.plain(store.cart.discountValue)
            case .none:
                break
            }
        }
    }

    private var value: Int? {
        if percent {
            guard let v = Money.parse(text, decimals: 2), v > 0, v <= 10000 else { return nil }
            return v
        }
        guard let v = Money.parse(text, decimals: store.currency.decimals), v > 0 else { return nil }
        return v
    }
}

struct NoteSheet: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    @Environment(\.dismiss) private var dismiss
    @State private var text = ""

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                Labeled("Note on the receipt") {
                    TextField("Table 4, no sugar…", text: $text, axis: .vertical)
                        .lineLimit(2...5)
                        .fieldLook()
                }
                MainButton("Save note") {
                    store.cart.note = text.trimmingCharacters(in: .whitespacesAndNewlines)
                    dismiss()
                }
                Spacer()
            }
            .padding(20)
            .background(look.page.ignoresSafeArea())
            .navigationTitle("Note")
            .inlineTitle()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
            }
        }
        .presentationDetents([.medium])
        .onAppear { text = store.cart.note }
    }
}
