// Draws the neutral app icon (a receipt on a dark tile, no shop name) as a 1024×1024 PNG.
// Usage: swift scripts/make-icon.swift WhiteLabelPOS/Assets.xcassets/AppIcon.appiconset/icon-1024.png
// Resellers can replace the PNG with their own icon.
import CoreGraphics
import Foundation
import ImageIO
import UniformTypeIdentifiers

let out = CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "icon-1024.png"
let size = 1024
let cs = CGColorSpaceCreateDeviceRGB()

func rgb(_ hex: Int, _ a: CGFloat = 1) -> CGColor {
    CGColor(colorSpace: cs, components: [CGFloat((hex >> 16) & 0xff) / 255, CGFloat((hex >> 8) & 0xff) / 255, CGFloat(hex & 0xff) / 255, a])!
}

guard let ctx = CGContext(data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: 0, space: cs,
                          bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue) else { fatalError("no context") }

// Background.
let bg = CGGradient(colorsSpace: cs, colors: [rgb(0x334155), rgb(0x0F172A)] as CFArray, locations: [0, 1])!
ctx.drawLinearGradient(bg, start: CGPoint(x: 0, y: 1024), end: CGPoint(x: 1024, y: 0), options: [])

// The receipt: rounded top, zig-zag bottom.
let left: CGFloat = 272, right: CGFloat = 752, top: CGFloat = 862, bottom: CGFloat = 196, r: CGFloat = 44
let teeth = 8
let tooth = (right - left) / CGFloat(teeth)
let paper = CGMutablePath()
paper.move(to: CGPoint(x: left, y: bottom))
for i in 0..<teeth {
    let x0 = left + CGFloat(i) * tooth
    paper.addLine(to: CGPoint(x: x0 + tooth / 2, y: bottom - 34))
    paper.addLine(to: CGPoint(x: x0 + tooth, y: bottom))
}
paper.addLine(to: CGPoint(x: right, y: top - r))
paper.addArc(tangent1End: CGPoint(x: right, y: top), tangent2End: CGPoint(x: right - r, y: top), radius: r)
paper.addLine(to: CGPoint(x: left + r, y: top))
paper.addArc(tangent1End: CGPoint(x: left, y: top), tangent2End: CGPoint(x: left, y: top - r), radius: r)
paper.closeSubpath()

ctx.saveGState()
ctx.setShadow(offset: CGSize(width: 0, height: -18), blur: 46, color: rgb(0x000000, 0.45))
ctx.addPath(paper)
ctx.setFillColor(rgb(0xF8FAFC))
ctx.fillPath()
ctx.restoreGState()

func bar(_ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ h: CGFloat, _ color: CGColor) {
    ctx.addPath(CGPath(roundedRect: CGRect(x: x, y: y, width: w, height: h), cornerWidth: h / 2, cornerHeight: h / 2, transform: nil))
    ctx.setFillColor(color)
    ctx.fillPath()
}

// Shop name line, item lines with amounts, a dashed rule and the total.
bar(372, 760, 280, 40, rgb(0x2563EB))
for (i, w) in [CGFloat(200), 160, 230].enumerated() {
    let y = 640 - CGFloat(i) * 72
    bar(336, y, w, 26, rgb(0xCBD5E1))
    bar(600, y, 88, 26, rgb(0xCBD5E1))
}
for i in 0..<9 {
    bar(336 + CGFloat(i) * 40, 418, 22, 8, rgb(0x94A3B8))
}
bar(336, 330, 150, 42, rgb(0x0F172A))
bar(560, 330, 128, 42, rgb(0x0F172A))

guard let image = ctx.makeImage(),
      let dest = CGImageDestinationCreateWithURL(URL(fileURLWithPath: out) as CFURL, UTType.png.identifier as CFString, 1, nil)
else { fatalError("can't write \(out)") }
CGImageDestinationAddImage(dest, image, nil)
guard CGImageDestinationFinalize(dest) else { fatalError("can't write \(out)") }
print("wrote \(out)")
