import POSCore
import SwiftUI
import UniformTypeIdentifiers

/// The backup file, written when it is shared.
struct BackupFile: Transferable {
    let doc: Doc
    let name: String

    static var transferRepresentation: some TransferRepresentation {
        FileRepresentation(exportedContentType: .json) { file in
            let url = FileManager.default.temporaryDirectory.appendingPathComponent(file.name)
            try DocFile.write(file.doc).write(to: url, options: .atomic)
            return SentTransferredFile(url)
        }
    }
}

struct SettingsView: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    @State private var importing = false
    @State private var pending: Doc?
    @State private var problem: String?
    @State private var pickDemo = false
    @State private var demoType: String?
    @State private var confirmStartOver = false

    private var appName: String {
        (Bundle.main.object(forInfoDictionaryKey: "CFBundleDisplayName") as? String)
            ?? (Bundle.main.object(forInfoDictionaryKey: "CFBundleName") as? String) ?? "POS"
    }

    private var version: String {
        (Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String) ?? "1.0"
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Panel(padding: 0) {
                    ShopHeader()
                        .padding(.vertical, 6)
                }
                .clipShape(RoundedRectangle(cornerRadius: min(look.radius, 20), style: .continuous))

                SectionLabel("Your shop")
                Panel(padding: 6) {
                    link(.step(1), "building.2", "Business details", detail: store.config.type(store.doc.business.type)?.name)
                    Hairline().padding(.leading, 56)
                    link(.step(2), "paintpalette", "Logo & colours")
                    Hairline().padding(.leading, 56)
                    link(.step(3), "rectangle.3.group", "Design", detail: store.config.design(store.doc.brand.design).name)
                    Hairline().padding(.leading, 56)
                    link(.step(4), "banknote", "Money & receipt", detail: store.currency.code)
                }

                SectionLabel("Safety")
                Panel(padding: 6) {
                    link(.pin, "lock", "Owner PIN", detail: store.doc.pin.isEmpty ? "Off" : "On")
                }

                SectionLabel("Your data")
                Panel(padding: 6) {
                    ShareLink(item: BackupFile(doc: store.doc, name: store.backupName), preview: SharePreview(store.backupName)) {
                        NavRow(icon: "square.and.arrow.up", title: "Back up", detail: "Save a copy")
                            .padding(.horizontal, 10)
                    }
                    .buttonStyle(.plain)
                    Hairline().padding(.leading, 56)
                    Button { importing = true } label: {
                        NavRow(icon: "square.and.arrow.down", title: "Restore a backup").padding(.horizontal, 10)
                    }
                    .buttonStyle(.plain)
                    Hairline().padding(.leading, 56)
                    Button { pickDemo = true } label: {
                        NavRow(icon: "sparkles", title: "Try a demo shop").padding(.horizontal, 10)
                    }
                    .buttonStyle(.plain)
                    Hairline().padding(.leading, 56)
                    Button { confirmStartOver = true } label: {
                        NavRow(icon: "arrow.counterclockwise", title: "Start over", color: look.bad).padding(.horizontal, 10)
                    }
                    .buttonStyle(.plain)
                }

                LText("\(appName) \(version)", 13, color: look.muted)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 24)
            }
            .padding(16)
        }
        .background(look.page.ignoresSafeArea())
        .navigationTitle("Settings")
        .fileImporter(isPresented: $importing, allowedContentTypes: [.json]) { result in
            read(result)
        }
        .confirmationDialog("Replace everything with this backup?", isPresented: Binding(get: { pending != nil }, set: { if !$0 { pending = nil } }), titleVisibility: .visible) {
            Button("Replace with \(pending?.brand.name ?? "backup")", role: .destructive) {
                if let d = pending { store.replace(d) }
                pending = nil
            }
        } message: {
            Text("Your products, sales and settings on this device are replaced by the ones in the backup.")
        }
        .alert("Can't open that file", isPresented: Binding(get: { problem != nil }, set: { if !$0 { problem = nil } })) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(problem ?? "")
        }
        .confirmationDialog("Which demo shop?", isPresented: $pickDemo, titleVisibility: .visible) {
            ForEach(store.config.types) { t in
                Button("\(t.icon) \(t.name)") { demoType = t.id }
            }
        }
        .confirmationDialog("Replace your shop with a demo shop?", isPresented: Binding(get: { demoType != nil }, set: { if !$0 { demoType = nil } }), titleVisibility: .visible) {
            Button("Use the demo shop", role: .destructive) {
                if let t = demoType { store.loadDemo(t) }
                demoType = nil
            }
        } message: {
            Text("Your products, sales and settings on this device are replaced. Back up first if you want to keep them.")
        }
        .confirmationDialog("Start over?", isPresented: $confirmStartOver, titleVisibility: .visible) {
            Button("Erase everything", role: .destructive) { store.startOver() }
        } message: {
            Text("All products, sales and settings on this device are erased and setup starts again.")
        }
    }

    private func link(_ route: SettingsRoute, _ icon: String, _ title: String, detail: String? = nil) -> some View {
        NavigationLink(value: route) {
            NavRow(icon: icon, title: title, detail: detail).padding(.horizontal, 10)
        }
        .buttonStyle(.plain)
    }

    private func read(_ result: Result<URL, Error>) {
        guard case .success(let url) = result else { return }
        let scoped = url.startAccessingSecurityScopedResource()
        defer { if scoped { url.stopAccessingSecurityScopedResource() } }
        guard let data = try? Data(contentsOf: url), let doc = try? DocFile.read(data) else {
            problem = "This file is not a backup from this app."
            return
        }
        pending = doc
    }
}

/// One setup step on its own, opened from Settings, with Save.
struct EditStepScreen: View {
    @Environment(Store.self) private var store
    @Environment(\.dismiss) private var dismiss
    let step: Int
    @State private var draft: Doc

    init(step: Int, doc: Doc) {
        self.step = step
        _draft = State(initialValue: doc)
    }

    private var title: String {
        switch step {
        case 1: return "Business details"
        case 2: return "Logo & colours"
        case 3: return "Design"
        default: return "Money & receipt"
        }
    }

    var body: some View {
        let look = Look(design: store.config.design(draft.brand.design), brand: draft.brand)
        ScrollView {
            Group {
                switch step {
                case 1: BusinessStep(draft: $draft)
                case 2: BrandStep(draft: $draft)
                case 3: DesignStep(draft: $draft)
                default: MoneyStep(draft: $draft)
                }
            }
            .padding(16)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(look.page.ignoresSafeArea())
        .safeAreaInset(edge: .bottom) {
            MainButton("Save", icon: "checkmark", enabled: !draft.brand.name.trimmingCharacters(in: .whitespaces).isEmpty) {
                let d = draft
                store.update { doc in
                    doc.brand = d.brand
                    doc.brand.name = d.brand.name.trimmingCharacters(in: .whitespaces)
                    doc.business = d.business
                }
                dismiss()
            }
            .padding(16)
            .background(look.page.opacity(0.96))
        }
        .navigationTitle(title)
        .inlineTitle()
        .environment(\.look, look)
        .preferredColorScheme(look.scheme)
    }
}
