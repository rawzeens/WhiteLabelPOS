import POSCore
import SwiftUI

/// Setup on the first launch, then the till.
struct RootView: View {
    @Environment(Store.self) private var store

    var body: some View {
        let look = store.look
        Group {
            if !store.doc.setupDone || store.setupStep != nil {
                SetupFlow(start: store.doc, step: store.setupStep ?? 0)
                    .id(store.setupKey)
            } else {
                MainTabs()
            }
        }
        .environment(\.look, look)
    }
}

struct MainTabs: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look

    var body: some View {
        @Bindable var store = store
        TabView(selection: $store.tab) {
            NavigationStack(path: $store.sellPath) {
                SellView()
                    .navigationDestination(for: SellRoute.self) { route in
                        switch route {
                        case .cart: CartScreen()
                        case .checkout: CheckoutView()
                        case .receipt(let id): ReceiptScreen(saleId: id, afterSale: true)
                        }
                    }
            }
            .barsLook(look)
            .tabItem { Label("Sell", systemImage: "cart") }
            .tag(MainTab.sell)

            NavigationStack(path: $store.orderPath) {
                OrdersView()
                    .navigationDestination(for: String.self) { id in
                        ReceiptScreen(saleId: id, afterSale: false)
                    }
            }
            .barsLook(look)
            .tabItem { Label("Orders", systemImage: "list.bullet.rectangle") }
            .tag(MainTab.orders)

            NavigationStack(path: $store.productsPath) {
                PinGate(reason: "Products are locked") {
                    ProductsView()
                }
                .navigationDestination(for: ProductRoute.self) { route in
                    switch route {
                    case .edit(let id): EditProductView(initial: store.doc.product(id))
                    case .new: EditProductView(initial: nil)
                    case .categories: CategoriesView()
                    }
                }
            }
            .barsLook(look)
            .tabItem { Label("Products", systemImage: "square.grid.2x2") }
            .tag(MainTab.products)

            NavigationStack {
                ReportsView()
            }
            .barsLook(look)
            .tabItem { Label("Reports", systemImage: "chart.bar") }
            .tag(MainTab.reports)

            NavigationStack(path: $store.settingsPath) {
                PinGate(reason: "Settings are locked") {
                    SettingsView()
                }
                .navigationDestination(for: SettingsRoute.self) { route in
                    switch route {
                    case .step(let n): EditStepScreen(step: n, doc: store.doc)
                    case .pin: PinSettingsView()
                    }
                }
            }
            .barsLook(look)
            .tabItem { Label("Settings", systemImage: "gearshape") }
            .tag(MainTab.settings)
        }
        .tint(look.primary)
        .preferredColorScheme(look.scheme)
    }
}
