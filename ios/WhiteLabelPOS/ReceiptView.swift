import POSCore
import SwiftUI

/// A receipt as printed: always dark text on white, in the design's font, with the shop's logo and details.
struct ReceiptPaper: View {
    @Environment(\.look) private var look
    let sale: Sale
    let doc: Doc
    let currency: Currency

    private let ink = Color(hex: "#111111")
    private let grey = Color(hex: "#5B5B5B")

    private func money(_ v: Int) -> String {
        Money.format(v, symbol: currency.symbol, decimals: currency.decimals)
    }

    var body: some View {
        VStack(spacing: 0) {
            VStack(spacing: 4) {
                LogoBadge(size: 54).padding(.bottom, 6)
                LText(doc.brand.name.isEmpty ? "Your shop" : doc.brand.name, 20, .bold, color: ink, caps: true)
                    .multilineTextAlignment(.center)
                ForEach([doc.brand.tagline, doc.business.address, doc.business.phone, doc.business.regNo].filter { !$0.isEmpty }, id: \.self) { s in
                    LText(s, 12, color: grey).multilineTextAlignment(.center)
                }
            }
            .frame(maxWidth: .infinity)
            dashes
            HStack {
                LText("Receipt #\(sale.number)", 13, .semibold, color: ink)
                Spacer()
                LText(Self.dateText(sale.date), 12, color: grey)
            }
            dashes
            VStack(spacing: 6) {
                ForEach(Array(sale.lines.enumerated()), id: \.offset) { _, l in
                    HStack(alignment: .firstTextBaseline) {
                        LText("\(l.qty) × \(l.name)", 14, color: ink).multilineTextAlignment(.leading)
                        Spacer(minLength: 8)
                        LText(money(l.price * l.qty), 14, color: ink)
                    }
                }
            }
            dashes
            VStack(spacing: 4) {
                row("Subtotal", money(sale.subtotal))
                if sale.discount > 0 {
                    row(sale.discountType == .percent ? "Discount \(Money.percentText(sale.discountValue))%" : "Discount", "-" + money(sale.discount))
                }
                if sale.taxRate > 0 {
                    row(Doc.taxLabel(name: sale.taxName, rate: sale.taxRate, inclusive: sale.taxInclusive), money(sale.tax))
                }
                if sale.rounding != 0 {
                    row("Rounding", (sale.rounding > 0 ? "+" : "") + money(sale.rounding))
                }
                HStack {
                    LText("Total", 18, .bold, color: ink, caps: true)
                    Spacer()
                    LText(money(sale.due), 18, .bold, color: ink)
                }
                .padding(.top, 4)
                row("Paid by", sale.method.label)
                if sale.method == .cash {
                    row("Cash", money(sale.paid))
                    row("Change", money(sale.change))
                }
            }
            if !sale.note.isEmpty {
                LText("Note: \(sale.note)", 13, color: grey)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.top, 10)
            }
            dashes
            if !doc.business.footer.isEmpty {
                LText(doc.business.footer, 13, .medium, color: ink, italic: look.design.font == "serif")
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
            }
        }
        .padding(22)
        .frame(maxWidth: 380)
        .background(Color.white)
        .overlay {
            if sale.refunded {
                Text("REFUNDED")
                    .font(.system(size: 34, weight: .heavy))
                    .foregroundStyle(Color(hex: "#DC2626").opacity(0.75))
                    .padding(.horizontal, 12)
                    .overlay(Rectangle().stroke(Color(hex: "#DC2626").opacity(0.75), lineWidth: 3))
                    .rotationEffect(.degrees(-14))
            }
        }
        .environment(\.colorScheme, .light)
    }

    private var dashes: some View {
        DashLine().stroke(grey.opacity(0.6), style: StrokeStyle(lineWidth: 1, dash: [4, 3]))
            .frame(height: 1)
            .padding(.vertical, 12)
    }

    private func row(_ label: String, _ value: String) -> some View {
        HStack {
            LText(label, 13, color: grey)
            Spacer()
            LText(value, 13, color: ink)
        }
    }

    static func dateText(_ d: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "d MMM yyyy, h:mm a"
        return f.string(from: d)
    }
}

/// The receipt after a sale (Share, New sale) or from Orders (Share, Refund).
struct ReceiptScreen: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    let saleId: String
    let afterSale: Bool
    @State private var picture: Image?
    @State private var askPin = false
    @State private var confirmRefund = false

    var body: some View {
        Group {
            if let sale = store.doc.sale(saleId) {
                content(sale)
            } else {
                EmptyMessage(icon: "doc.text", title: "Receipt not found", message: "This sale is no longer here.")
            }
        }
        .background(look.page.ignoresSafeArea())
        .navigationTitle(afterSale ? "Sale complete" : "Receipt")
        .inlineTitle()
        .navigationBarBackButtonHiddenIfAfterSale(afterSale)
        .hideTabBar()
    }

    @ViewBuilder private func shareButton(_ sale: Sale) -> some View {
        if let picture {
            ShareLink(item: picture, preview: SharePreview("Receipt #\(sale.number)", image: picture)) {
                HStack(spacing: 8) {
                    Image(systemName: "square.and.arrow.up")
                    LText("Share receipt", 16, .medium, caps: true)
                }
                .foregroundStyle(look.ink)
                .frame(maxWidth: .infinity, minHeight: 50)
                .background(look.raised, in: look.buttonShape)
            }
            .buttonStyle(.plain)
        }
    }

    private func content(_ sale: Sale) -> some View {
        ScrollView {
            VStack(spacing: 16) {
                if afterSale {
                    HStack(spacing: 10) {
                        Image(systemName: "checkmark.circle.fill").font(.system(size: 26)).foregroundStyle(look.good)
                        VStack(alignment: .leading, spacing: 2) {
                            LText("Paid \(store.money(sale.due))", 18, .bold, caps: true)
                            if sale.method == .cash && sale.change > 0 {
                                LText("Give \(store.money(sale.change)) change", 15, .semibold, color: look.good)
                            } else {
                                LText("by \(sale.method.label)", 14, color: look.muted)
                            }
                        }
                        Spacer()
                    }
                }
                ReceiptPaper(sale: sale, doc: store.doc, currency: store.currency)
                    .shadow(color: .black.opacity(0.12), radius: 10, y: 4)
                // After a sale, Share and New sale sit at the bottom of the screen instead.
                if !afterSale {
                    shareButton(sale)
                    if !sale.refunded {
                        SoftButton("Refund this sale", icon: "arrow.uturn.backward", color: look.bad) {
                            if store.doc.pin.isEmpty { confirmRefund = true } else { askPin = true }
                        }
                    } else {
                        LText("Refunded \(Self.refundText(sale))", 14, color: look.bad)
                    }
                }
            }
            .padding(16)
            .frame(maxWidth: 520)
            .frame(maxWidth: .infinity)
        }
        .safeAreaInset(edge: .bottom) {
            if afterSale {
                HStack(spacing: 10) {
                    shareButton(sale)
                    MainButton("New sale", icon: "plus") { store.sellPath = [] }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .frame(maxWidth: 520)
                .frame(maxWidth: .infinity)
                .background(look.page.opacity(0.96))
            }
        }
        .onAppear { draw(sale) }
        .onChange(of: sale) { _, s in draw(s) }
        .sheet(isPresented: $askPin) {
            PinCheckSheet(title: "Owner PIN to refund") {
                askPin = false
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) { confirmRefund = true }
            }
            .environment(\.look, look)
        }
        .confirmationDialog("Refund \(store.money(sale.due))?", isPresented: $confirmRefund, titleVisibility: .visible) {
            Button("Refund sale", role: .destructive) { store.refund(sale.id) }
        } message: {
            Text("Give the money back to the customer. Stock goes back on the shelf.")
        }
    }

    private func draw(_ sale: Sale) {
        picture = renderImage(
            ReceiptPaper(sale: sale, doc: store.doc, currency: store.currency)
                .frame(width: 380)
                .environment(\.look, look)
        )
    }

    static func refundText(_ s: Sale) -> String {
        s.refundTime > 0 ? ReceiptPaper.dateText(Date(timeIntervalSince1970: Double(s.refundTime) / 1000)) : ""
    }
}

extension View {
    /// After a sale the receipt replaces the till; going back would show the empty checkout.
    func navigationBarBackButtonHiddenIfAfterSale(_ yes: Bool) -> some View {
        navigationBarBackButtonHidden(yes)
    }
}
