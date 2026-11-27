import Foundation

/// Colour helpers shared with the Android app (SPEC section 5). Colours are "#RRGGBB".
public enum Palette {
    public static func rgb(_ hex: String) -> (r: Int, g: Int, b: Int)? {
        var s = hex.trimmingCharacters(in: .whitespaces)
        if s.hasPrefix("#") { s.removeFirst() }
        guard s.count == 6, let v = Int(s, radix: 16) else { return nil }
        return ((v >> 16) & 0xff, (v >> 8) & 0xff, v & 0xff)
    }

    public static func hex(_ r: Int, _ g: Int, _ b: Int) -> String {
        String(format: "#%02X%02X%02X", min(max(r, 0), 255), min(max(g, 0), 255), min(max(b, 0), 255))
    }

    /// "#abc123" / "abc123" → "#ABC123"; anything else → nil.
    public static func normalized(_ text: String) -> String? {
        guard let c = rgb(text) else { return nil }
        return hex(c.r, c.g, c.b)
    }

    public static func isValid(_ text: String) -> Bool { rgb(text) != nil }

    /// Relative luminance (sRGB, linearised).
    public static func luminance(_ hex: String) -> Double {
        guard let c = rgb(hex) else { return 0 }
        func lin(_ v: Int) -> Double {
            let x = Double(v) / 255
            return x <= 0.03928 ? x / 12.92 : pow((x + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * lin(c.r) + 0.7152 * lin(c.g) + 0.0722 * lin(c.b)
    }

    /// Text colour to use on top of `hex`: white or near-black, whichever reads better.
    public static func on(_ hex: String) -> String {
        let l = luminance(hex)
        return 1.05 / (l + 0.05) >= (l + 0.05) / 0.05 ? "#FFFFFF" : "#111111"
    }

    /// `hex` laid at `amount` (0…1) over `base`.
    public static func mix(_ hex: String, over base: String, amount: Double) -> String {
        guard let a = rgb(hex), let b = rgb(base) else { return hex }
        func m(_ x: Int, _ y: Int) -> Int { Int((Double(x) * amount + Double(y) * (1 - amount)).rounded()) }
        return self.hex(m(a.r, b.r), m(a.g, b.g), m(a.b, b.b))
    }

    /// The colour at 14 % over the page (22 % on dark designs).
    public static func soft(_ hex: String, page: String, dark: Bool) -> String {
        mix(hex, over: page, amount: dark ? 0.22 : 0.14)
    }
}

/// Initials for a shop or a product without a picture (SPEC section 5).
public enum Monogram {
    public static func of(_ name: String) -> String {
        let words = name.split(whereSeparator: { $0.isWhitespace }).filter { w in
            guard let f = w.first else { return false }
            return f.isLetter || f.isNumber
        }
        guard let first = words.first?.first else { return "?" }
        if words.count >= 2, let last = words.last?.first {
            return (String(first) + String(last)).uppercased()
        }
        return String(first).uppercased()
    }
}
