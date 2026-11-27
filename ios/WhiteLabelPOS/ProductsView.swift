import POSCore
import SwiftUI

/// What the shop sells, grouped by category.
struct ProductsView: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    @State private var search = ""

    var body: some View {
        let q = search.trimmingCharacters(in: .whitespaces).lowercased()
        let list = store.doc.products.filter { q.isEmpty || $0.name.lowercased().contains(q) || $0.sku.lowercased().contains(q) }
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 8) {
                    Image(systemName: "magnifyingglass").foregroundStyle(look.muted)
                    TextField("Search products", text: $search).font(look.font(16))
                }
                .fieldLook()
                HStack(spacing: 10) {
                    MainButton("Add product", icon: "plus") { store.productsPath.append(.new) }
                    SoftButton("Categories", icon: "folder") { store.productsPath.append(.categories) }
                }
                .padding(.top, 12)
                if store.doc.products.isEmpty {
                    EmptyMessage(icon: "square.grid.2x2", title: "No products yet", message: "Add the things you sell. They show up on the till straight away.")
                }
                ForEach(Array(store.doc.groupedProducts(list).enumerated()), id: \.offset) { _, g in
                    SectionLabel(g.category?.name ?? "No category")
                    Panel(padding: 0) {
                        ForEach(Array(g.products.enumerated()), id: \.element.id) { i, p in
                            if i > 0 { Hairline().padding(.leading, 66) }
                            NavigationLink(value: ProductRoute.edit(p.id)) {
                                ProductRow(product: p)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
            .padding(16)
        }
        .background(look.page.ignoresSafeArea())
        .navigationTitle("Products")
    }
}

struct ProductRow: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    let product: Product

    var body: some View {
        HStack(spacing: 12) {
            ProductIcon(product: product, size: 40)
            VStack(alignment: .leading, spacing: 2) {
                LText(product.name, 16, .medium, caps: true).lineLimit(1)
                if !product.sku.isEmpty {
                    LText(product.sku, 12, color: look.muted)
                }
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                LText(store.money(product.price), 15, .semibold)
                if product.trackStock {
                    LText(product.stock <= 0 ? "Sold out" : "\(product.stock) left", 12, .medium,
                          color: product.stock <= 0 ? look.bad : (product.stock <= 3 ? look.accent : look.muted))
                }
            }
            Image(systemName: "chevron.right").font(.system(size: 12, weight: .semibold)).foregroundStyle(look.muted)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .contentShape(Rectangle())
    }
}

/// Adding or changing one product.
struct EditProductView: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    @Environment(\.dismiss) private var dismiss
    let initial: Product?
    @State private var draft: Product
    @State private var priceText: String
    @State private var stockText: String
    @State private var newCategory = false
    @State private var newCategoryName = ""
    @State private var confirmDelete = false
    @State private var loaded = false

    static let colors = ["#E63946", "#F77F00", "#E9A23B", "#2A9D8F", "#15803D", "#457B9D", "#2563EB", "#7B2CBF",
                         "#E0457B", "#6F4E37", "#64748B", "#1F1F1F"]
    static let emoji = ["☕", "🍵", "🥤", "🍔", "🍟", "🍰", "🍞", "🍜", "🛒", "👕", "💅", "✂️", "🎁", "⭐"]

    init(initial: Product?) {
        self.initial = initial
        _draft = State(initialValue: initial ?? Product(color: "#2563EB"))
        _priceText = State(initialValue: "")
        _stockText = State(initialValue: initial.map { String($0.stock) } ?? "0")
    }

    private var price: Int? { Money.parse(priceText, decimals: store.currency.decimals) }
    private var canSave: Bool { !draft.name.trimmingCharacters(in: .whitespaces).isEmpty && price != nil }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                preview
                SectionLabel("Product")
                Panel {
                    Labeled("Name") {
                        TextField("Iced latte", text: $draft.name).wordsCapitalized().fieldLook()
                    }
                    Labeled("Price (\(store.currency.symbol))") {
                        TextField(store.plain(0), text: $priceText).decimalKeyboard().fieldLook()
                    }
                    Labeled("Category") {
                        Menu {
                            Button("No category") { draft.category = "" }
                            ForEach(store.doc.categories) { c in
                                Button(c.name) { draft.category = c.id }
                            }
                            Divider()
                            Button("New category…") { newCategory = true }
                        } label: {
                            HStack {
                                LText(store.doc.categoryName(draft.category) ?? "No category", 16)
                                Spacer()
                                Image(systemName: "chevron.up.chevron.down").foregroundStyle(look.muted)
                            }
                            .fieldLook()
                        }
                    }
                    Labeled("Code / SKU", hint: "Optional. Shown on some designs and helps searching.") {
                        TextField("ABC-001", text: $draft.sku).noAutocorrect().fieldLook()
                    }
                }
                SectionLabel("Picture")
                Panel {
                    Labeled("Emoji", hint: "Leave empty to show the first letters of the name.") {
                        TextField("☕", text: $draft.icon).fieldLook()
                            .onChange(of: draft.icon) { _, v in
                                if v.count > 1 { draft.icon = String(v.suffix(1)) }
                            }
                    }
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 6) {
                            ForEach(Self.emoji, id: \.self) { e in
                                Button { draft.icon = e } label: {
                                    Text(e).font(.system(size: 24)).frame(width: 40, height: 40)
                                        .background(draft.icon == e ? look.primarySoft : look.raised, in: RoundedRectangle(cornerRadius: 10))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    .padding(.bottom, 14)
                    LText("Colour", 13, .medium, color: look.muted, caps: true).padding(.bottom, 6)
                    SwatchRow(colors: Self.colors, selected: draft.color, size: 34) { draft.color = $0 }
                }
                SectionLabel("Stock")
                Panel {
                    Toggle(isOn: $draft.trackStock) {
                        VStack(alignment: .leading, spacing: 2) {
                            LText("Count stock", 16, .medium)
                            LText("Shows Sold out when none are left.", 13, color: look.muted)
                        }
                    }
                    .tint(look.primary)
                    if draft.trackStock {
                        Labeled("In stock now") {
                            TextField("0", text: $stockText).numberKeyboard().fieldLook()
                        }
                        .padding(.top, 12)
                    }
                }
                if initial != nil {
                    SoftButton("Delete product", icon: "trash", color: look.bad) { confirmDelete = true }
                        .padding(.top, 20)
                }
            }
            .padding(16)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(look.page.ignoresSafeArea())
        .navigationTitle(initial == nil ? "New product" : "Edit product")
        .inlineTitle()
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") { save() }.disabled(!canSave)
            }
        }
        .alert("New category", isPresented: $newCategory) {
            TextField("Name", text: $newCategoryName)
            Button("Add") { addCategory() }
            Button("Cancel", role: .cancel) {}
        }
        .confirmationDialog("Delete \(draft.name)?", isPresented: $confirmDelete, titleVisibility: .visible) {
            Button("Delete product", role: .destructive) {
                store.update { d in d.products.removeAll { $0.id == initial?.id } }
                dismiss()
            }
        } message: {
            Text("Old receipts keep it. It goes off the till.")
        }
        .onAppear {
            guard !loaded else { return }
            loaded = true
            if let p = initial { priceText = store.plain(p.price) }
        }
    }

    private var preview: some View {
        var shown = draft
        shown.price = price ?? 0
        shown.stock = Int(stockText) ?? 0
        if shown.name.isEmpty { shown.name = "New product" }
        return HStack {
            Spacer()
            ProductBoard(doc: store.doc, products: [shown], width: 200, money: store.money, qty: { _ in 0 }, tap: { _ in })
                .frame(width: look.design.columns == 1 ? 320 : 170)
                .allowsHitTesting(false)
            Spacer()
        }
        .padding(.vertical, 8)
    }

    private func addCategory() {
        let name = newCategoryName.trimmingCharacters(in: .whitespaces)
        newCategoryName = ""
        guard !name.isEmpty else { return }
        var id = ""
        store.update { d in
            id = d.makeId("c")
            d.categories.append(ProductCategory(id: id, name: name))
        }
        draft.category = id
    }

    private func save() {
        guard let price else { return }
        var p = draft
        p.name = p.name.trimmingCharacters(in: .whitespaces)
        p.price = price
        p.sku = p.sku.trimmingCharacters(in: .whitespaces)
        p.stock = max(0, Int(stockText.filter(\.isNumber)) ?? 0)
        if !Palette.isValid(p.color) { p.color = "#64748B" }
        store.update { d in
            if let i = d.products.firstIndex(where: { $0.id == p.id && !p.id.isEmpty }) {
                d.products[i] = p
            } else {
                p.id = d.makeId("p")
                d.products.append(p)
            }
        }
        dismiss()
    }
}

/// Adding, renaming and removing categories.
struct CategoriesView: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    @State private var editing: ProductCategory?
    @State private var name = ""
    @State private var adding = false
    @State private var deleting: ProductCategory?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                MainButton("Add category", icon: "plus") {
                    name = ""
                    adding = true
                }
                .padding(.bottom, 8)
                if store.doc.categories.isEmpty {
                    EmptyMessage(icon: "folder", title: "No categories", message: "Categories make the till quicker to use.")
                } else {
                    Panel(padding: 0) {
                        ForEach(Array(store.doc.categories.enumerated()), id: \.element.id) { i, c in
                            if i > 0 { Hairline().padding(.leading, 16) }
                            row(c, index: i)
                        }
                    }
                    .padding(.top, 12)
                }
            }
            .padding(16)
        }
        .background(look.page.ignoresSafeArea())
        .navigationTitle("Categories")
        .inlineTitle()
        .alert("New category", isPresented: $adding) {
            TextField("Name", text: $name)
            Button("Add") {
                let n = name.trimmingCharacters(in: .whitespaces)
                guard !n.isEmpty else { return }
                store.update { d in d.categories.append(ProductCategory(id: d.makeId("c"), name: n)) }
            }
            Button("Cancel", role: .cancel) {}
        }
        .alert("Rename category", isPresented: Binding(get: { editing != nil }, set: { if !$0 { editing = nil } })) {
            TextField("Name", text: $name)
            Button("Save") {
                let n = name.trimmingCharacters(in: .whitespaces)
                if let c = editing, !n.isEmpty {
                    store.update { d in
                        if let i = d.categories.firstIndex(where: { $0.id == c.id }) { d.categories[i].name = n }
                    }
                }
                editing = nil
            }
            Button("Cancel", role: .cancel) { editing = nil }
        }
        .confirmationDialog("Delete \(deleting?.name ?? "")?", isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } }), titleVisibility: .visible) {
            Button("Delete category", role: .destructive) {
                if let c = deleting { store.update { $0.deleteCategory(c.id) } }
                deleting = nil
            }
        } message: {
            Text("Its products stay, with no category.")
        }
    }

    private func row(_ c: ProductCategory, index: Int) -> some View {
        let count = store.doc.products.filter { $0.category == c.id }.count
        return HStack(spacing: 10) {
            VStack(alignment: .leading, spacing: 2) {
                LText(c.name, 16, .medium, caps: true)
                LText(count == 1 ? "1 product" : "\(count) products", 13, color: look.muted)
            }
            Spacer()
            iconButton("arrow.up", enabled: index > 0) { move(index, by: -1) }
            iconButton("arrow.down", enabled: index < store.doc.categories.count - 1) { move(index, by: 1) }
            iconButton("pencil", enabled: true) {
                name = c.name
                editing = c
            }
            iconButton("trash", enabled: true, color: look.bad) { deleting = c }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }

    private func iconButton(_ icon: String, enabled: Bool, color: Color? = nil, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(color ?? look.ink)
                .frame(width: 34, height: 34)
                .background(look.raised, in: RoundedRectangle(cornerRadius: min(look.smallRadius, 8)))
        }
        .buttonStyle(.plain)
        .opacity(enabled ? 1 : 0.3)
        .disabled(!enabled)
    }

    private func move(_ i: Int, by d: Int) {
        store.update { doc in
            let j = i + d
            guard j >= 0 && j < doc.categories.count else { return }
            doc.categories.swapAt(i, j)
        }
    }
}
