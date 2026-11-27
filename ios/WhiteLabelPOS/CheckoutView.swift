import POSCore
import SwiftUI

/// Taking the money: how the customer pays, cash received and change. The app records the payment; it does not move
/// money itself.
struct CheckoutView: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    @State private var method = PayMethod.cash
    @State private var received = ""

    private var totals: Totals { store.doc.totals(store.cart, method: method) }
    private var paid: Int { method == .cash ? (Money.parse(received, decimals: store.currency.decimals) ?? 0) : totals.due }
    private var canComplete: Bool { !store.cart.isEmpty && (method != .cash || paid >= totals.due) }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                VStack(spacing: 6) {
                    LText("Amount due", 13, .medium, color: look.muted, caps: true)
                    LText(store.money(totals.due), 46, .bold)
                        .contentTransition(.numericText())
                    if totals.rounding != 0 {
                        LText("Cash rounded from \(store.money(totals.total))", 13, color: look.muted)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 18)
                .background(look.surface, in: RoundedRectangle(cornerRadius: min(look.radius, 20), style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: min(look.radius, 20), style: .continuous).stroke(look.line))

                LazyVGrid(columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)], spacing: 10) {
                    ForEach(PayMethod.allCases) { m in
                        MethodButton(method: m, selected: method == m) {
                            withAnimation(.snappy) { method = m }
                        }
                    }
                }

                if method == .cash {
                    cashPart
                } else {
                    Panel {
                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: "info.circle").foregroundStyle(look.primary)
                            LText(otherHint, 15, color: look.ink)
                        }
                    }
                }

                Panel {
                    TotalsBlock(totals: totals, showRounding: true)
                }
            }
            .padding(16)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(look.page.ignoresSafeArea())
        .safeAreaInset(edge: .bottom) {
            MainButton(method == .cash ? "Complete sale" : "Mark as paid", icon: "checkmark", enabled: canComplete) {
                complete()
            }
            .padding(16)
            .background(look.page.opacity(0.96))
        }
        .navigationTitle("Charge")
        .inlineTitle()
        .hideTabBar()
    }

    private var cashPart: some View {
        Panel {
            Labeled("Cash received") {
                TextField(store.plain(totals.due), text: $received)
                    .decimalKeyboard()
                    .font(look.font(22, .semibold))
                    .fieldLook()
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ChipView(title: "Exact", selected: paid == totals.due && !received.isEmpty) { received = store.plain(totals.due) }
                    ForEach(Money.quickCash(due: totals.due, decimals: store.currency.decimals), id: \.self) { v in
                        ChipView(title: store.money(v), selected: paid == v && v != totals.due) { received = store.plain(v) }
                    }
                }
                .padding(.vertical, 2)
            }
            Hairline().padding(.vertical, 12)
            HStack {
                LText("Change", 17, .semibold, caps: true)
                Spacer()
                if received.isEmpty || paid >= totals.due {
                    LText(store.money(max(0, paid - totals.due)), 28, .bold, color: look.good)
                } else {
                    LText("\(store.money(totals.due - paid)) short", 17, .semibold, color: look.bad)
                }
            }
        }
    }

    private var otherHint: String {
        switch method {
        case .card: return "Take the payment on your card machine, then tap Mark as paid."
        case .qr: return "Let the customer scan your shop's QR code. When the payment shows, tap Mark as paid."
        case .ewallet: return "Take the payment in the customer's e-wallet, then tap Mark as paid."
        case .cash: return ""
        }
    }

    private func complete() {
        guard canComplete else { return }
        let sale = store.completeSale(method: method, paid: paid)
        store.sellPath = [.receipt(sale.id)]
    }
}

struct MethodButton: View {
    @Environment(\.look) private var look
    let method: PayMethod
    let selected: Bool
    let action: () -> Void

    private var icon: String {
        switch method {
        case .cash: return "banknote"
        case .card: return "creditcard"
        case .qr: return "qrcode"
        case .ewallet: return "iphone"
        }
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: icon).font(.system(size: 18, weight: .semibold))
                LText(method.label, 16, .semibold, color: selected ? look.onPrimary : look.ink, caps: true)
                Spacer(minLength: 0)
                if selected { Image(systemName: "checkmark.circle.fill") }
            }
            .foregroundStyle(selected ? look.onPrimary : look.ink)
            .padding(.horizontal, 14)
            .frame(minHeight: 56)
            .background(selected ? look.primary : look.surface, in: look.buttonShape)
            .overlay(look.buttonShape.stroke(selected ? look.primary : look.line, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}
