// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "POSCore",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "POSCore", targets: ["POSCore"]),
    ],
    targets: [
        // The shop's data and the rules for totals, tax, cash rounding, stock, refunds and reports
        // (the same rules as the Android app, see whitelabel/SPEC.md).
        .target(name: "POSCore"),
        // Checks against the shared files in whitelabel/fixtures and whitelabel/app, used by the tests and a small runner.
        .target(name: "CoreChecks", dependencies: ["POSCore"]),
        .executableTarget(name: "core-check", dependencies: ["CoreChecks"]),
        .testTarget(name: "POSCoreTests", dependencies: ["CoreChecks"]),
    ]
)
