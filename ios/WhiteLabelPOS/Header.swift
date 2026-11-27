import POSCore
import SwiftUI

/// The shop's logo, or its initials, in the design's logo shape.
struct LogoBadge: View {
    @Environment(\.look) private var look
    var size: CGFloat = 44
    /// Light on dark: for a logo sitting on the brand colour (the Street header bar).
    var inverted = false

    private var shape: AnyShape {
        switch look.design.logoShape {
        case "rounded": return AnyShape(RoundedRectangle(cornerRadius: size * 0.26, style: .continuous))
        case "square", "outline": return AnyShape(Rectangle())
        default: return AnyShape(Circle())
        }
    }

    var body: some View {
        let outline = look.design.logoShape == "outline"
        ZStack {
            if let img = LogoCache.image(look.brand.logo) {
                img.resizable().scaledToFill()
                    .frame(width: size, height: size)
                    .background(Color.white)
                    .clipShape(shape)
            } else {
                shape.fill(outline ? Color.clear : (inverted ? look.onPrimary : look.primary))
                    .frame(width: size, height: size)
                Text(Monogram.of(look.brand.name))
                    .font(look.font(size * 0.38, .bold))
                    .tracking(outline ? size * 0.04 : 0)
                    .foregroundStyle(outline ? look.ink : (inverted ? look.primary : look.onPrimary))
                    .minimumScaleFactor(0.5)
                    .lineLimit(1)
                    .padding(size * 0.08)
            }
        }
        .frame(width: size, height: size)
        .overlay {
            if outline {
                Rectangle().stroke(look.ink, lineWidth: 1)
            }
        }
        .overlay {
            if look.design.logoShape == "ring" {
                Circle().stroke(look.primary, lineWidth: 2).padding(-5)
            }
        }
        .padding(look.design.logoShape == "ring" ? 5 : 0)
    }
}

/// The shop's name at the top of the till, in the design's header style.
struct ShopHeader: View {
    @Environment(\.look) private var look
    var compact = false

    private var name: String { look.brand.name.isEmpty ? "Your shop" : look.brand.name }
    private var tagline: String { look.brand.tagline }

    var body: some View {
        switch look.design.header {
        case "glow": glow
        case "centered": centered
        case "bar": bar
        case "minimal": minimal
        default: plain
        }
    }

    private var plainRow: some View {
        HStack(spacing: 12) {
            LogoBadge(size: compact ? 36 : 44)
            VStack(alignment: .leading, spacing: 2) {
                LText(name, compact ? 17 : 21, .bold, caps: true).lineLimit(1)
                if !tagline.isEmpty {
                    LText(tagline, compact ? 12 : 13, color: look.muted).lineLimit(1)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, compact ? 10 : 12)
    }

    private var plain: some View {
        plainRow
    }

    private var glow: some View {
        VStack(spacing: 0) {
            plainRow
            LinearGradient(colors: [look.primary, look.accent.opacity(0.7), look.primary.opacity(0)],
                           startPoint: .leading, endPoint: .trailing)
                .frame(height: 2)
                .shadow(color: look.primary.opacity(0.9), radius: 6)
        }
    }

    private var centered: some View {
        VStack(spacing: 6) {
            LogoBadge(size: compact ? 44 : 64)
            LText(name, compact ? 19 : 25, .bold).lineLimit(1)
            if !tagline.isEmpty {
                LText(tagline, compact ? 12 : 14, color: look.muted, italic: true).lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 16)
        .padding(.vertical, compact ? 10 : 14)
    }

    private var bar: some View {
        HStack(spacing: 12) {
            LogoBadge(size: compact ? 34 : 42, inverted: true)
            VStack(alignment: .leading, spacing: 1) {
                LText(name, compact ? 18 : 23, .heavy, color: look.onPrimary, caps: true).lineLimit(1)
                if !tagline.isEmpty {
                    LText(tagline, compact ? 11 : 13, .medium, color: look.onPrimary.opacity(0.85), caps: true).lineLimit(1)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, compact ? 10 : 14)
        .background(look.primary.ignoresSafeArea(edges: .top))
    }

    private var minimal: some View {
        VStack(spacing: compact ? 8 : 12) {
            HStack(spacing: 10) {
                LogoBadge(size: compact ? 24 : 30)
                LText(name, compact ? 14 : 16, .regular, caps: true).lineLimit(1)
            }
            .frame(maxWidth: .infinity)
            if !tagline.isEmpty && !compact {
                LText(tagline, 11, color: look.muted, caps: true).lineLimit(1)
            }
            Rectangle().fill(look.line).frame(height: 1)
        }
        .padding(.horizontal, 16)
        .padding(.top, compact ? 10 : 14)
    }
}

/// A product's emoji, or its initials in its colour.
struct ProductIcon: View {
    @Environment(\.look) private var look
    let product: Product
    var size: CGFloat = 40
    /// Draw the circle behind the emoji.
    var circle = true

    var body: some View {
        if !product.icon.isEmpty {
            Text(product.icon)
                .font(.system(size: size * (circle ? 0.5 : 0.8)))
                .frame(width: size, height: size)
                .background(circle ? look.soft(product.color) : Color.clear, in: Circle())
        } else {
            Text(Monogram.of(product.name))
                .font(look.font(size * 0.36, .bold))
                .foregroundStyle(look.on(product.color))
                .frame(width: size, height: size)
                .background(look.color(product.color), in: Circle())
        }
    }
}

/// How many of a product are already in the sale, in the accent colour.
struct QtyBadge: View {
    @Environment(\.look) private var look
    let qty: Int

    var body: some View {
        if qty > 0 {
            Text("\(qty)")
                .font(look.font(13, .bold))
                .foregroundStyle(look.onAccent)
                .padding(.horizontal, 7)
                .frame(minWidth: 24, minHeight: 24)
                .background(look.accent, in: Capsule())
                .overlay(Capsule().stroke(look.surface, lineWidth: 2))
        }
    }
}
