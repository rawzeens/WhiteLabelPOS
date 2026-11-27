import POSCore
import SwiftUI

extension Color {
    /// "#RRGGBB"; anything else gives grey.
    init(hex: String) {
        let c = Palette.rgb(hex) ?? (100, 116, 139)
        self.init(red: Double(c.r) / 255, green: Double(c.g) / 255, blue: Double(c.b) / 255)
    }
}

/// The look of every screen: one of the 5 design environments with the shop's own colours and logo on top.
struct Look {
    let design: Design
    let brand: Brand
    let primaryHex: String
    let accentHex: String

    init(design: Design, brand: Brand) {
        self.design = design
        self.brand = brand
        primaryHex = Palette.normalized(brand.primary) ?? "#2563EB"
        accentHex = Palette.normalized(brand.accent) ?? "#F59E0B"
    }

    static let plain = Look(design: .fallback, brand: Brand())

    var primary: Color { Color(hex: primaryHex) }
    var accent: Color { Color(hex: accentHex) }
    var onPrimary: Color { Color(hex: Palette.on(primaryHex)) }
    var onAccent: Color { Color(hex: Palette.on(accentHex)) }
    var primarySoft: Color { soft(primaryHex) }

    var page: Color { Color(hex: design.page) }
    var surface: Color { Color(hex: design.surface) }
    var raised: Color { Color(hex: design.raised) }
    var ink: Color { Color(hex: design.ink) }
    var muted: Color { Color(hex: design.muted) }
    var line: Color { Color(hex: design.line) }
    var dark: Bool { design.dark }
    var good: Color { Color(hex: dark ? "#4ADE80" : "#15803D") }
    var bad: Color { Color(hex: dark ? "#F87171" : "#DC2626") }

    /// Corner radius of cards and tiles.
    var radius: CGFloat { CGFloat(design.radius) }
    /// Corner radius of smaller things (fields, rows, chips).
    var smallRadius: CGFloat { min(CGFloat(design.radius), 12) }

    var buttonRadius: CGFloat {
        switch design.buttons {
        case "pill": return 999
        case "square": return 0
        default: return min(CGFloat(design.radius), 16)
        }
    }

    /// The shape of main buttons.
    var buttonShape: RoundedRectangle {
        RoundedRectangle(cornerRadius: buttonRadius, style: .continuous)
    }

    func color(_ hex: String) -> Color { Color(hex: hex) }
    func on(_ hex: String) -> Color { Color(hex: Palette.on(hex)) }
    func soft(_ hex: String) -> Color { Color(hex: Palette.soft(hex, page: design.page, dark: design.dark)) }

    /// The design's font at this size.
    func font(_ size: CGFloat, _ weight: Font.Weight = .regular) -> Font {
        switch design.font {
        case "rounded":
            return .system(size: size, weight: weight, design: .rounded)
        case "serif":
            return .system(size: size, weight: weight, design: .serif)
        case "condensed":
            return .system(size: size, weight: Look.heavier(weight)).width(.condensed)
        case "light":
            return .system(size: size, weight: Look.lighter(weight))
        default:
            return .system(size: size, weight: weight)
        }
    }

    static func heavier(_ w: Font.Weight) -> Font.Weight {
        switch w {
        case .bold, .heavy, .black: return .heavy
        case .semibold: return .bold
        case .medium: return .semibold
        default: return .medium
        }
    }

    static func lighter(_ w: Font.Weight) -> Font.Weight {
        switch w {
        case .black, .heavy: return .semibold
        case .bold: return .medium
        case .semibold, .medium: return .regular
        default: return .light
        }
    }

    /// Capitals when the design asks for them (headings, buttons, labels, product names).
    func caps(_ s: String) -> String { design.caps ? s.uppercased() : s }

    /// Letter spacing in points for text of this size.
    func tracking(_ size: CGFloat) -> CGFloat { CGFloat(design.tracking) * size }

    /// Weight of main button text.
    var buttonWeight: Font.Weight {
        switch design.font {
        case "condensed": return .heavy
        case "light": return .regular
        default: return .semibold
        }
    }

    var scheme: ColorScheme { dark ? .dark : .light }
}

private struct LookKey: EnvironmentKey {
    static let defaultValue = Look.plain
}

extension EnvironmentValues {
    var look: Look {
        get { self[LookKey.self] }
        set { self[LookKey.self] = newValue }
    }
}
