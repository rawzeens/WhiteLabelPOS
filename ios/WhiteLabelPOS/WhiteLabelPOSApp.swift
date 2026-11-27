import SwiftUI

@main
struct WhiteLabelPOSApp: App {
    @State private var store = Store()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(store)
        }
    }
}
