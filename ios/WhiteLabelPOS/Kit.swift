import POSCore
import SwiftUI
#if os(iOS)
import UIKit
#else
import AppKit
#endif

/// Text in the design's font. With `caps` it follows the design's capitals and letter spacing.
struct LText: View {
    @Environment(\.look) private var look
    let text: String
    var size: CGFloat
    var weight: Font.Weight
    var color: Color?
    var caps: Bool
    var italic: Bool

    init(_ text: String, _ size: CGFloat = 15, _ weight: Font.Weight = .regular, color: Color? = nil,
         caps: Bool = false, italic: Bool = false) {
        self.text = text
        self.size = size
        self.weight = weight
        self.color = color
        self.caps = caps
        self.italic = italic
    }

    var body: some View {
        Text(caps ? look.caps(text) : text)
            .font(italic ? look.font(size, weight).italic() : look.font(size, weight))
            .tracking(caps ? look.tracking(size) : 0)
            .foregroundStyle(color ?? look.ink)
    }
}

/// Small capitals above a group of things.
struct SectionLabel: View {
    @Environment(\.look) private var look
    let text: String

    init(_ text: String) {
        self.text = text
    }

    var body: some View {
        Text(text.uppercased())
            .font(look.font(12, .semibold))
            .tracking(max(0.6, look.tracking(12)))
            .foregroundStyle(look.muted)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, 18)
            .padding(.bottom, 6)
            .padding(.leading, 4)
    }
}

/// The big button for the main action (Charge, Complete sale, Next…), in the brand colour.
struct MainButton: View {
    @Environment(\.look) private var look
    let title: String
    var icon: String?
    var enabled = true
    let action: () -> Void

    init(_ title: String, icon: String? = nil, enabled: Bool = true, action: @escaping () -> Void) {
        self.title = title
        self.icon = icon
        self.enabled = enabled
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            MainButtonLabel(title: title, icon: icon)
        }
        .buttonStyle(.plain)
        .opacity(enabled ? 1 : 0.4)
        .disabled(!enabled)
    }
}

struct MainButtonLabel: View {
    @Environment(\.look) private var look
    let title: String
    var icon: String?

    var body: some View {
        HStack(spacing: 8) {
            if let icon {
                Image(systemName: icon).font(.system(size: 16, weight: .semibold))
            }
            LText(title, 17, look.buttonWeight, color: look.onPrimary, caps: true)
        }
        .foregroundStyle(look.onPrimary)
        .frame(maxWidth: .infinity, minHeight: 54)
        .padding(.horizontal, 16)
        .background(look.primary, in: look.buttonShape)
        .contentShape(look.buttonShape)
    }
}

/// A quieter button (Back, Restore a backup…).
struct SoftButton: View {
    @Environment(\.look) private var look
    let title: String
    var icon: String?
    var color: Color?
    let action: () -> Void

    init(_ title: String, icon: String? = nil, color: Color? = nil, action: @escaping () -> Void) {
        self.title = title
        self.icon = icon
        self.color = color
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let icon {
                    Image(systemName: icon).font(.system(size: 15, weight: .medium))
                }
                LText(title, 16, .medium, color: color ?? look.ink, caps: true)
            }
            .foregroundStyle(color ?? look.ink)
            .frame(maxWidth: .infinity, minHeight: 50)
            .padding(.horizontal, 12)
            .background(look.raised, in: look.buttonShape)
            .overlay(look.buttonShape.stroke(look.line, lineWidth: look.design.buttons == "square" ? 1 : 0))
            .contentShape(look.buttonShape)
        }
        .buttonStyle(.plain)
    }
}

/// A card on the page.
struct Panel<Content: View>: View {
    @Environment(\.look) private var look
    var padding: CGFloat = 16
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) { content }
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(look.surface, in: RoundedRectangle(cornerRadius: min(look.radius, 20), style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: min(look.radius, 20), style: .continuous).stroke(look.line, lineWidth: 1))
    }
}

/// A category chip in the design's chip style.
struct ChipView: View {
    @Environment(\.look) private var look
    let title: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) { label }
            .buttonStyle(.plain)
    }

    @ViewBuilder private var label: some View {
        switch look.design.chips {
        case "outline":
            LText(title, 14, .medium, color: selected ? look.primary : look.ink, caps: true)
                .padding(.horizontal, 14).padding(.vertical, 8)
                .overlay(Capsule().stroke(selected ? look.primary : look.line, lineWidth: selected ? 1.5 : 1))
                .shadow(color: selected ? look.primary.opacity(0.5) : .clear, radius: 6)
        case "pill":
            LText(title, 15, selected ? .semibold : .regular, color: selected ? look.onPrimary : look.ink, caps: true)
                .padding(.horizontal, 16).padding(.vertical, 8)
                .background(selected ? look.primary : look.raised, in: Capsule())
        case "solid":
            LText(title, 14, .bold, color: selected ? look.onPrimary : look.page, caps: true)
                .padding(.horizontal, 14).padding(.vertical, 9)
                .background(selected ? look.primary : look.ink, in: Rectangle())
        case "underline":
            VStack(spacing: 6) {
                LText(title, 13, .regular, color: selected ? look.ink : look.muted, caps: true)
                Rectangle().fill(selected ? look.primary : Color.clear).frame(height: 2)
            }
            .padding(.horizontal, 6)
            .fixedSize()
        default:
            LText(title, 14, .semibold, color: selected ? look.onPrimary : look.ink, caps: true)
                .padding(.horizontal, 14).padding(.vertical, 8)
                .background(selected ? look.primary : look.raised, in: RoundedRectangle(cornerRadius: look.smallRadius, style: .continuous))
        }
    }
}

/// Text field look: raised box in the design's font.
struct FieldLook: ViewModifier {
    @Environment(\.look) private var look

    func body(content: Content) -> some View {
        content
            .font(look.font(16))
            .foregroundStyle(look.ink)
            .padding(.horizontal, 12)
            .padding(.vertical, 12)
            .background(look.raised, in: RoundedRectangle(cornerRadius: look.smallRadius, style: .continuous))
    }
}

extension View {
    func fieldLook() -> some View { modifier(FieldLook()) }
}

/// A label above a text field.
struct Labeled<Content: View>: View {
    @Environment(\.look) private var look
    let label: String
    var hint: String?
    @ViewBuilder var content: Content

    init(_ label: String, hint: String? = nil, @ViewBuilder content: () -> Content) {
        self.label = label
        self.hint = hint
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            LText(label, 13, .medium, color: look.muted, caps: true)
            content
            if let hint {
                LText(hint, 12, color: look.muted)
            }
        }
        .padding(.bottom, 14)
    }
}

/// A label on the left and an amount on the right.
struct AmountRow: View {
    @Environment(\.look) private var look
    let label: String
    let value: String
    var bold = false
    var color: Color?
    var size: CGFloat = 15

    init(_ label: String, _ value: String, bold: Bool = false, color: Color? = nil, size: CGFloat = 15) {
        self.label = label
        self.value = value
        self.bold = bold
        self.color = color
        self.size = size
    }

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            LText(label, size, bold ? .bold : .regular, color: color ?? (bold ? look.ink : look.muted), caps: bold)
            Spacer(minLength: 8)
            LText(value, size, bold ? .bold : .medium, color: color ?? look.ink)
        }
        .padding(.vertical, 3)
    }
}

/// A row in a settings-style list that opens something.
struct NavRow: View {
    @Environment(\.look) private var look
    let icon: String
    let title: String
    var detail: String?
    var color: Color?

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .medium))
                .foregroundStyle(color ?? look.primary)
                .frame(width: 34, height: 34)
                .background((color ?? look.primary).opacity(look.dark ? 0.2 : 0.1), in: RoundedRectangle(cornerRadius: min(look.smallRadius, 9)))
            LText(title, 16, .medium, color: color ?? look.ink)
            Spacer()
            if let detail {
                LText(detail, 14, color: look.muted)
            }
            Image(systemName: "chevron.right").font(.system(size: 13, weight: .semibold)).foregroundStyle(look.muted)
        }
        .padding(.vertical, 10)
        .contentShape(Rectangle())
    }
}

/// A thin line between rows.
struct Hairline: View {
    @Environment(\.look) private var look

    var body: some View {
        Rectangle().fill(look.line).frame(height: 1)
    }
}

/// A dashed line (receipts, menu leaders).
struct DashLine: Shape {
    var dash: [CGFloat] = [4, 3]

    func path(in rect: CGRect) -> Path {
        var p = Path()
        p.move(to: CGPoint(x: 0, y: rect.midY))
        p.addLine(to: CGPoint(x: rect.maxX, y: rect.midY))
        return p
    }
}

/// A big friendly message when a list is empty.
struct EmptyMessage: View {
    @Environment(\.look) private var look
    let icon: String
    let title: String
    let message: String

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title)
                .foregroundStyle(look.primary)
                .frame(width: 64, height: 64)
                .background(look.primarySoft, in: Circle())
            LText(title, 17, .semibold, caps: true).padding(.top, 6)
            LText(message, 14, color: look.muted).multilineTextAlignment(.center)
        }
        .padding(32)
        .frame(maxWidth: .infinity)
    }
}

/// A short message that floats at the bottom for a moment.
struct Toast: View {
    @Environment(\.look) private var look
    let text: String

    var body: some View {
        LText(text, 14, .medium, color: look.page)
            .padding(.horizontal, 16).padding(.vertical, 10)
            .background(look.ink.opacity(0.92), in: Capsule())
            .padding(.bottom, 90)
    }
}

/// The colour picker rows in setup and the product editor.
struct SwatchRow: View {
    @Environment(\.look) private var look
    let colors: [String]
    let selected: String
    var size: CGFloat = 36
    let pick: (String) -> Void

    var body: some View {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: size + 8), spacing: 8)], spacing: 8) {
            ForEach(colors, id: \.self) { hex in
                Button { pick(hex) } label: {
                    Circle().fill(Color(hex: hex))
                        .frame(width: size, height: size)
                        .overlay(Circle().stroke(look.line, lineWidth: 1))
                        .overlay {
                            if hex.uppercased() == selected.uppercased() {
                                Image(systemName: "checkmark").font(.system(size: 14, weight: .bold)).foregroundStyle(Color(hex: Palette.on(hex)))
                            }
                        }
                }
                .buttonStyle(.plain)
            }
        }
    }
}

/// A colour typed as #RRGGBB, with a dot showing it.
struct HexField: View {
    @Environment(\.look) private var look
    let value: String
    let set: (String) -> Void
    @State private var text = ""

    var body: some View {
        HStack(spacing: 10) {
            Circle().fill(Color(hex: value)).frame(width: 26, height: 26).overlay(Circle().stroke(look.line))
            TextField("#RRGGBB", text: $text)
                .noAutocorrect()
                .onSubmit(apply)
                .onChange(of: text) { _, _ in
                    if let n = Palette.normalized(text), n != value { set(n) }
                }
        }
        .fieldLook()
        .onAppear { text = value }
        .onChange(of: value) { _, v in
            if Palette.normalized(text) != v { text = v }
        }
    }

    private func apply() {
        if let n = Palette.normalized(text) { set(n) } else { text = value }
    }
}

// MARK: - pictures

/// Keeps the last decoded logo so screens don't decode it on every redraw.
enum LogoCache {
    private static var lastKey = ""
    private static var lastImage: Image?

    static func image(_ base64: String) -> Image? {
        guard !base64.isEmpty else { return nil }
        if base64 == lastKey { return lastImage }
        let img = Data(base64Encoded: base64).flatMap(imageFromData)
        lastKey = base64
        lastImage = img
        return img
    }
}

func imageFromData(_ data: Data) -> Image? {
    #if os(iOS)
    return UIImage(data: data).map { Image(uiImage: $0) }
    #else
    return NSImage(data: data).map { Image(nsImage: $0) }
    #endif
}

/// The picture scaled to at most 512 px on its longest side, as PNG.
func scaledLogoPNG(_ data: Data, maxSide: CGFloat = 512) -> Data? {
    #if os(iOS)
    guard let img = UIImage(data: data), img.size.width > 0, img.size.height > 0 else { return nil }
    let s = min(1, maxSide / max(img.size.width * img.scale, img.size.height * img.scale))
    let size = CGSize(width: max(1, floor(img.size.width * img.scale * s)), height: max(1, floor(img.size.height * img.scale * s)))
    let format = UIGraphicsImageRendererFormat()
    format.scale = 1
    let out = UIGraphicsImageRenderer(size: size, format: format).image { _ in
        img.draw(in: CGRect(origin: .zero, size: size))
    }
    return out.pngData()
    #else
    guard let img = NSImage(data: data), let tiff = img.tiffRepresentation else { return nil }
    return NSBitmapImageRep(data: tiff)?.representation(using: .png, properties: [:])
    #endif
}

/// A picture of a view (for sharing receipts).
@MainActor
func renderImage<V: View>(_ view: V, scale: CGFloat = 3) -> Image? {
    let r = ImageRenderer(content: view)
    r.scale = scale
    #if os(iOS)
    return r.uiImage.map { Image(uiImage: $0) }
    #else
    return r.nsImage.map { Image(nsImage: $0) }
    #endif
}

// MARK: - iPhone-only modifiers (so the screens also type-check for the Mac)

extension View {
    func decimalKeyboard() -> some View {
        #if os(iOS)
        return keyboardType(.decimalPad)
        #else
        return self
        #endif
    }

    func numberKeyboard() -> some View {
        #if os(iOS)
        return keyboardType(.numberPad)
        #else
        return self
        #endif
    }

    func phoneKeyboard() -> some View {
        #if os(iOS)
        return keyboardType(.phonePad)
        #else
        return self
        #endif
    }

    func wordsCapitalized() -> some View {
        #if os(iOS)
        return textInputAutocapitalization(.words)
        #else
        return self
        #endif
    }

    func noAutocorrect() -> some View {
        #if os(iOS)
        return textInputAutocapitalization(.characters).autocorrectionDisabled()
        #else
        return autocorrectionDisabled()
        #endif
    }

    func inlineTitle() -> some View {
        #if os(iOS)
        return navigationBarTitleDisplayMode(.inline)
        #else
        return self
        #endif
    }

    /// Cart, payment and receipt get the whole screen, so the main buttons are never under the tab bar.
    func hideTabBar() -> some View {
        #if os(iOS)
        return toolbar(.hidden, for: .tabBar)
        #else
        return self
        #endif
    }

    func hideNavBar() -> some View {
        #if os(iOS)
        return toolbar(.hidden, for: .navigationBar)
        #else
        return self
        #endif
    }

    /// Navigation bar and tab bar in the design's colours.
    func barsLook(_ look: Look) -> some View {
        #if os(iOS)
        return toolbarBackground(look.page, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbarColorScheme(look.scheme, for: .navigationBar)
            .toolbarBackground(look.surface, for: .tabBar)
            .toolbarBackground(.visible, for: .tabBar)
            .toolbarColorScheme(look.scheme, for: .tabBar)
        #else
        return self
        #endif
    }
}
