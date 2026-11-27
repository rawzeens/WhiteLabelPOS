import PhotosUI
import POSCore
import SwiftUI
import UniformTypeIdentifiers

/// First launch: welcome, then business, logo & colours, design, money & receipt, finish. Every step shows the till
/// in the chosen design with the shop's own name and colours.
struct SetupFlow: View {
    @Environment(Store.self) private var store
    @State private var draft: Doc
    @State private var step: Int
    @State private var addStarter = true
    @State private var pin1 = ""
    @State private var pin2 = ""
    @State private var importing = false
    @State private var problem: String?
    @State private var pickDemo = false

    init(start: Doc, step: Int) {
        _draft = State(initialValue: start)
        _step = State(initialValue: step)
    }

    private var look: Look { Look(design: store.config.design(draft.brand.design), brand: draft.brand) }
    private var pinOK: Bool { pin1.isEmpty && pin2.isEmpty || (pin1.count == 4 && pin1 == pin2) }
    private var nameOK: Bool { !draft.brand.name.trimmingCharacters(in: .whitespaces).isEmpty }

    var body: some View {
        let look = self.look
        VStack(spacing: 0) {
            progress
            ScrollView {
                Group {
                    switch step {
                    case 1: BusinessStep(draft: $draft)
                    case 2: BrandStep(draft: $draft)
                    case 3: DesignStep(draft: $draft)
                    case 4: MoneyStep(draft: $draft)
                    case 5: FinishStep(draft: $draft, addStarter: $addStarter, pin1: $pin1, pin2: $pin2)
                    default: welcome
                    }
                }
                .padding(20)
                .frame(maxWidth: 760)
                .frame(maxWidth: .infinity)
            }
            .scrollDismissesKeyboard(.interactively)
            if step > 0 {
                HStack(spacing: 12) {
                    SoftButton("Back", icon: "chevron.left") { withAnimation { step -= 1 } }
                        .frame(maxWidth: 140)
                    if step < 5 {
                        MainButton("Next", icon: "chevron.right", enabled: step != 1 || nameOK) { withAnimation { step += 1 } }
                    } else {
                        MainButton("Open my POS", icon: "checkmark", enabled: pinOK && nameOK) { finish() }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
                .frame(maxWidth: 760)
                .frame(maxWidth: .infinity)
                .background(look.page)
            }
        }
        .background(look.page.ignoresSafeArea())
        .environment(\.look, look)
        .preferredColorScheme(look.scheme)
        .fileImporter(isPresented: $importing, allowedContentTypes: [.json]) { result in
            guard case .success(let url) = result else { return }
            let scoped = url.startAccessingSecurityScopedResource()
            defer { if scoped { url.stopAccessingSecurityScopedResource() } }
            if let data = try? Data(contentsOf: url), let doc = try? DocFile.read(data) {
                store.replace(doc)
            } else {
                problem = "This file is not a backup from this app."
            }
        }
        .alert("Can't open that file", isPresented: Binding(get: { problem != nil }, set: { if !$0 { problem = nil } })) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(problem ?? "")
        }
        .confirmationDialog("Which demo shop?", isPresented: $pickDemo, titleVisibility: .visible) {
            ForEach(store.config.types) { t in
                Button("\(t.icon) \(t.name)") { store.loadDemo(t.id) }
            }
        }
    }

    private var progress: some View {
        HStack(spacing: 6) {
            ForEach(0..<6) { i in
                Capsule()
                    .fill(i <= step ? look.primary : look.line)
                    .frame(height: 4)
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 10)
        .padding(.bottom, 6)
    }

    private var welcome: some View {
        VStack(spacing: 18) {
            Image(systemName: "storefront")
                .font(.system(size: 40, weight: .medium))
                .foregroundStyle(look.onPrimary)
                .frame(width: 96, height: 96)
                .background(look.primary, in: RoundedRectangle(cornerRadius: 28, style: .continuous))
                .padding(.top, 40)
            LText("Your shop. Your brand. Your till.", 32, .bold)
                .multilineTextAlignment(.center)
            LText("Put in your name, logo and colours, pick one of five designs, and start selling. You can change everything later.",
                  17, color: look.muted)
                .multilineTextAlignment(.center)
            VStack(spacing: 12) {
                MainButton("Set up my shop", icon: "arrow.right") { withAnimation { step = 1 } }
                SoftButton("Restore a backup", icon: "square.and.arrow.down") { importing = true }
                SoftButton("Try a demo shop", icon: "sparkles") { pickDemo = true }
            }
            .padding(.top, 16)
            .frame(maxWidth: 420)
        }
        .frame(maxWidth: .infinity)
    }

    private func finish() {
        var d = draft
        d.brand.name = d.brand.name.trimmingCharacters(in: .whitespaces)
        if d.products.isEmpty && addStarter, let t = store.config.type(d.business.type) {
            d.addStarterProducts(t, decimals: store.config.currency(d.business.currency).decimals)
        }
        if pin1.count == 4 && pin1 == pin2 { d.pin = pin1 }
        d.format = "whitelabel-pos"
        d.setupDone = true
        store.replace(d)
        store.unlocked = true
    }
}

/// The heading of a setup step.
struct StepTitle: View {
    @Environment(\.look) private var look
    let title: String
    let text: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            LText(title, 28, .bold, caps: true)
            LText(text, 16, color: look.muted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.bottom, 16)
    }
}

/// A small till in a design: header, four products and the Charge button.
struct MiniPreview: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    let draft: Doc
    var height: CGFloat = 330

    private var doc: Doc {
        if !draft.products.isEmpty { return draft }
        var d = draft
        if let t = store.config.type(d.business.type) {
            d.addStarterProducts(t, decimals: store.config.currency(d.business.currency).decimals)
        }
        return d
    }

    var body: some View {
        let d = doc
        let currency = store.config.currency(d.business.currency)
        let products = Array(d.products.prefix(4))
        let first = products.first?.id ?? ""
        let money: (Int) -> String = { Money.format($0, symbol: currency.symbol, decimals: currency.decimals) }
        let total = (products.first?.price ?? 0) * 2
        VStack(spacing: 0) {
            ShopHeader(compact: true)
            ProductBoard(doc: d, products: products, width: 300, money: money, qty: { $0 == first ? 2 : 0 }, tap: { _ in })
                .padding(12)
                .allowsHitTesting(false)
            Spacer(minLength: 0)
        }
        .frame(height: height, alignment: .top)
        .overlay(alignment: .bottom) {
            MainButtonLabel(title: "Charge \(money(total))", icon: "creditcard")
                .padding(12)
                .background(LinearGradient(colors: [look.page.opacity(0), look.page], startPoint: .top, endPoint: .center))
        }
        .frame(maxWidth: .infinity)
        .background(look.page)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).stroke(look.line, lineWidth: 1))
        .environment(\.colorScheme, look.scheme)
    }
}

// MARK: - steps

struct BusinessStep: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    @Binding var draft: Doc

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            StepTitle(title: "Your business", text: "This name goes on the till and on every receipt.")
            Panel {
                Labeled("Business name") {
                    TextField("e.g. Kopi Pagi", text: $draft.brand.name).wordsCapitalized().fieldLook()
                }
                Labeled("Tagline", hint: "Optional. A short line under your name.") {
                    TextField("Coffee, toast & good mornings", text: $draft.brand.tagline).fieldLook()
                }
            }
            SectionLabel("What do you sell?")
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 150), spacing: 10)], spacing: 10) {
                ForEach(store.config.types) { t in
                    let on = draft.business.type == t.id
                    Button { draft.business.type = t.id } label: {
                        HStack(spacing: 10) {
                            Text(t.icon).font(.system(size: 26))
                            LText(t.name, 15, .semibold, color: on ? look.onPrimary : look.ink).multilineTextAlignment(.leading)
                            Spacer(minLength: 0)
                        }
                        .padding(12)
                        .frame(maxWidth: .infinity, minHeight: 64, alignment: .leading)
                        .background(on ? look.primary : look.surface, in: RoundedRectangle(cornerRadius: min(look.radius, 16), style: .continuous))
                        .overlay(RoundedRectangle(cornerRadius: min(look.radius, 16), style: .continuous).stroke(on ? look.primary : look.line))
                    }
                    .buttonStyle(.plain)
                }
            }
            SectionLabel("On the receipt")
            Panel {
                Labeled("Phone") {
                    TextField("012-345 6789", text: $draft.business.phone).phoneKeyboard().fieldLook()
                }
                Labeled("Address") {
                    TextField("Street, town", text: $draft.business.address, axis: .vertical).lineLimit(1...3).fieldLook()
                }
                Labeled("Registration number", hint: "Optional. Company or tax registration.") {
                    TextField("SSM 2024…", text: $draft.business.regNo).fieldLook()
                }
            }
            SectionLabel("Preview")
            MiniPreview(draft: draft)
        }
    }
}

struct BrandStep: View {
    @Environment(\.look) private var look
    @Binding var draft: Doc
    @State private var pick: PhotosPickerItem?
    @State private var working = false

    static let brandColors = ["#2563EB", "#0F766E", "#15803D", "#65A30D", "#CA8A04", "#EA580C", "#D62828",
                              "#E0457B", "#9333EA", "#4F46E5", "#0891B2", "#6F4E37", "#1F1F1F", "#475569"]
    static let accentColors = ["#F59E0B", "#FCBF49", "#E0A43B", "#FFB3C6", "#22C55E", "#38BDF8", "#B08D57", "#F43F5E"]

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            StepTitle(title: "Logo & colours", text: "Your logo and colours are used on every screen and receipt.")
            MiniPreview(draft: draft)
            SectionLabel("Logo")
            Panel {
                HStack(spacing: 16) {
                    LogoBadge(size: 76)
                    VStack(alignment: .leading, spacing: 10) {
                        PhotosPicker(selection: $pick, matching: .images) {
                            HStack(spacing: 6) {
                                Image(systemName: "photo")
                                LText(draft.brand.logo.isEmpty ? "Choose a picture" : "Change picture", 15, .semibold, color: look.onPrimary, caps: true)
                            }
                            .foregroundStyle(look.onPrimary)
                            .padding(.horizontal, 14).padding(.vertical, 10)
                            .background(look.primary, in: look.buttonShape)
                        }
                        .buttonStyle(.plain)
                        if !draft.brand.logo.isEmpty {
                            Button { draft.brand.logo = "" } label: {
                                LText("Remove picture", 14, .medium, color: look.bad, caps: true)
                            }
                            .buttonStyle(.plain)
                        } else {
                            LText(working ? "Getting the picture…" : "No picture? Your initials are used.", 13, color: look.muted)
                        }
                    }
                }
            }
            SectionLabel("Brand colour")
            Panel {
                SwatchRow(colors: Self.brandColors, selected: draft.brand.primary) { draft.brand.primary = $0 }
                    .padding(.bottom, 12)
                HexField(value: draft.brand.primary) { draft.brand.primary = $0 }
            }
            SectionLabel("Accent colour")
            Panel {
                SwatchRow(colors: Self.accentColors, selected: draft.brand.accent) { draft.brand.accent = $0 }
                    .padding(.bottom, 12)
                HexField(value: draft.brand.accent) { draft.brand.accent = $0 }
                LText("Used for the number badges on products in the sale.", 13, color: look.muted).padding(.top, 8)
            }
        }
        .onChange(of: pick) { _, item in
            guard let item else { return }
            working = true
            Task {
                let data = try? await item.loadTransferable(type: Data.self)
                await MainActor.run {
                    if let data, let png = scaledLogoPNG(data) {
                        draft.brand.logo = png.base64EncodedString()
                    }
                    working = false
                    pick = nil
                }
            }
        }
    }
}

struct DesignStep: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    @Binding var draft: Doc

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            StepTitle(title: "Choose a design", text: "Five ready-made looks. Your name, logo and colours are used in all of them. You can switch any time.")
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 300), spacing: 14)], spacing: 14) {
                ForEach(store.config.designs) { d in
                    DesignCard(design: d, draft: draft, selected: draft.brand.design == d.id) {
                        withAnimation { draft.brand.design = d.id }
                    }
                }
            }
        }
    }
}

struct DesignCard: View {
    @Environment(\.look) private var look
    let design: Design
    let draft: Doc
    let selected: Bool
    let pick: () -> Void

    var body: some View {
        Button(action: pick) {
            VStack(alignment: .leading, spacing: 10) {
                MiniPreview(draft: draft, height: 300)
                    .environment(\.look, Look(design: design, brand: draft.brand))
                    .allowsHitTesting(false)
                HStack {
                    LText(design.name, 19, .bold)
                    Spacer()
                    Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                        .font(.system(size: 22))
                        .foregroundStyle(selected ? look.primary : look.muted)
                }
                LText(design.about, 14, color: look.muted)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(10)
            .background(look.surface, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(selected ? look.primary : look.line, lineWidth: selected ? 3 : 1))
        }
        .buttonStyle(.plain)
    }
}

struct MoneyStep: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    @Binding var draft: Doc
    @State private var rateText = ""
    @State private var loaded = false

    private var currency: Currency { store.config.currency(draft.business.currency) }
    private var roundStep: Int { draft.business.cashRoundTo > 0 ? draft.business.cashRoundTo : currency.cashRoundTo }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            StepTitle(title: "Money & receipt", text: "Your currency, sales tax and the last line on every receipt.")
            Panel {
                Labeled("Currency") {
                    Menu {
                        ForEach(store.config.currencies) { c in
                            Button("\(c.symbol)  \(c.name)") { setCurrency(c) }
                        }
                    } label: {
                        HStack {
                            LText("\(currency.symbol)  \(currency.name)", 16)
                            Spacer()
                            Image(systemName: "chevron.up.chevron.down").foregroundStyle(look.muted)
                        }
                        .fieldLook()
                    }
                }
                HStack(alignment: .top, spacing: 12) {
                    Labeled("Tax name") {
                        TextField("SST", text: $draft.business.taxName).fieldLook()
                    }
                    Labeled("Tax rate (%)", hint: "0 for no tax") {
                        TextField("0", text: $rateText)
                            .decimalKeyboard()
                            .fieldLook()
                            .onChange(of: rateText) { _, v in
                                draft.business.taxRate = min(10000, Money.parse(v, decimals: 2) ?? 0)
                            }
                    }
                }
                Toggle(isOn: $draft.business.taxInclusive) {
                    VStack(alignment: .leading, spacing: 2) {
                        LText("Prices already include tax", 16, .medium)
                        LText("Off: tax is added on top of your prices.", 13, color: look.muted)
                    }
                }
                .tint(look.primary)
                .padding(.bottom, 12)
                if currency.cashRoundTo > 0 || draft.business.cashRoundTo > 0 {
                    Toggle(isOn: Binding(get: { draft.business.cashRoundTo > 0 },
                                         set: { draft.business.cashRoundTo = $0 ? roundStep : 0 })) {
                        VStack(alignment: .leading, spacing: 2) {
                            LText("Round cash to the nearest \(Money.format(roundStep, symbol: currency.symbol, decimals: currency.decimals))", 16, .medium)
                            LText("Only for cash. Cards and QR are charged exactly.", 13, color: look.muted)
                        }
                    }
                    .tint(look.primary)
                    .padding(.bottom, 12)
                }
                Labeled("Receipt message", hint: "The last line of every receipt.") {
                    TextField("Thank you!", text: $draft.business.footer).fieldLook()
                }
            }
            SectionLabel("Receipt preview")
            ReceiptPaper(sale: sample, doc: draft, currency: currency)
                .shadow(color: .black.opacity(0.12), radius: 8, y: 3)
                .frame(maxWidth: .infinity)
        }
        .onAppear {
            guard !loaded else { return }
            loaded = true
            rateText = draft.business.taxRate > 0 ? Money.percentText(draft.business.taxRate) : ""
        }
    }

    private func setCurrency(_ c: Currency) {
        draft.business.currency = c.code
        draft.business.taxName = c.taxName
        draft.business.cashRoundTo = c.cashRoundTo
    }

    /// A made-up sale with the shop's own settings, to show the receipt.
    private var sample: Sale {
        var d = draft
        if d.products.isEmpty, let t = store.config.type(d.business.type) {
            d.addStarterProducts(t, decimals: currency.decimals)
        }
        var cart = Cart()
        if d.products.count > 0 { cart.add(d.products[0].id, qty: 2) }
        if d.products.count > 1 { cart.add(d.products[1].id) }
        let due = d.totals(cart, method: .cash).due
        return d.completeSale(cart, method: .cash, paid: Money.nextThousand(due), time: Store.nowMs())
    }
}

struct FinishStep: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    @Binding var draft: Doc
    @Binding var addStarter: Bool
    @Binding var pin1: String
    @Binding var pin2: String

    var body: some View {
        let type = store.config.type(draft.business.type)
        VStack(alignment: .leading, spacing: 0) {
            StepTitle(title: "Almost done", text: "Here is your till. Choose how to start and, if you like, lock it with a PIN.")
            MiniPreview(draft: draft)
            if draft.products.isEmpty, let type {
                SectionLabel("Products")
                Panel {
                    Toggle(isOn: $addStarter) {
                        VStack(alignment: .leading, spacing: 2) {
                            LText("Add the starter products for \(type.name)", 16, .medium)
                            LText("\(type.products.count) products in \(type.categories.count) categories. Change prices and names any time.", 13, color: look.muted)
                        }
                    }
                    .tint(look.primary)
                    if !addStarter {
                        LText("You start with an empty list and add your own products.", 13, color: look.muted).padding(.top, 8)
                    }
                }
            }
            SectionLabel("Owner PIN (optional)")
            Panel {
                LText("A 4-digit PIN locks Settings, Products and refunds, so staff can only sell.", 14, color: look.muted)
                    .padding(.bottom, 12)
                HStack(spacing: 12) {
                    SecureField("PIN", text: $pin1).numberKeyboard().fieldLook()
                        .onChange(of: pin1) { _, v in pin1 = String(v.filter(\.isNumber).prefix(4)) }
                    SecureField("Again", text: $pin2).numberKeyboard().fieldLook()
                        .onChange(of: pin2) { _, v in pin2 = String(v.filter(\.isNumber).prefix(4)) }
                }
                if !pin1.isEmpty || !pin2.isEmpty {
                    let ok = pin1.count == 4 && pin1 == pin2
                    LText(ok ? "The PIN is set." : (pin1.count < 4 ? "Use 4 digits." : "Type the same PIN twice."), 13, .medium,
                          color: ok ? look.good : look.bad)
                        .padding(.top, 8)
                }
            }
        }
    }
}
