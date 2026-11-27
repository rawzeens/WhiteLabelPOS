import POSCore
import SwiftUI

/// Four dots and a number pad.
struct PinPad: View {
    @Environment(\.look) private var look
    let title: String
    var message: String?
    var messageIsError = false
    let done: (String) -> Void
    @State private var entered = ""

    var body: some View {
        VStack(spacing: 22) {
            Image(systemName: "lock.fill")
                .font(.system(size: 26))
                .foregroundStyle(look.primary)
                .frame(width: 64, height: 64)
                .background(look.primarySoft, in: Circle())
            LText(title, 20, .bold, caps: true).multilineTextAlignment(.center)
            HStack(spacing: 18) {
                ForEach(0..<4) { i in
                    Circle()
                        .fill(i < entered.count ? look.primary : Color.clear)
                        .overlay(Circle().stroke(i < entered.count ? look.primary : look.muted, lineWidth: 1.5))
                        .frame(width: 16, height: 16)
                }
            }
            if let message {
                LText(message, 14, .medium, color: messageIsError ? look.bad : look.muted).multilineTextAlignment(.center)
            }
            LazyVGrid(columns: Array(repeating: GridItem(.fixed(78), spacing: 18), count: 3), spacing: 14) {
                ForEach(["1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫"], id: \.self) { k in
                    key(k)
                }
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity)
    }

    @ViewBuilder private func key(_ k: String) -> some View {
        if k.isEmpty {
            Color.clear.frame(width: 78, height: 64)
        } else {
            Button {
                if k == "⌫" {
                    if !entered.isEmpty { entered.removeLast() }
                } else if entered.count < 4 {
                    entered.append(k)
                    if entered.count == 4 {
                        let pin = entered
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                            entered = ""
                            done(pin)
                        }
                    }
                }
            } label: {
                Group {
                    if k == "⌫" {
                        Image(systemName: "delete.left").font(.system(size: 22))
                    } else {
                        Text(k).font(look.font(28, .medium))
                    }
                }
                .foregroundStyle(look.ink)
                .frame(width: 78, height: 64)
                .background(k == "⌫" ? Color.clear : look.raised, in: RoundedRectangle(cornerRadius: look.design.buttons == "pill" ? 32 : min(look.radius, 16), style: .continuous))
            }
            .buttonStyle(.plain)
        }
    }
}

/// Shows `content` once the owner PIN has been typed (or straight away when there is no PIN).
struct PinGate<Content: View>: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    let reason: String
    @ViewBuilder var content: Content
    @State private var wrong = false

    var body: some View {
        if store.doc.pin.isEmpty || store.unlocked {
            content
        } else {
            ScrollView {
                PinPad(title: "Owner PIN", message: wrong ? "That PIN is not right. Try again." : reason, messageIsError: wrong) { pin in
                    if pin == store.doc.pin {
                        wrong = false
                        store.unlocked = true
                    } else {
                        wrong = true
                    }
                }
                .padding(.top, 30)
            }
            .background(look.page.ignoresSafeArea())
        }
    }
}

/// Asks for the owner PIN before something like a refund.
struct PinCheckSheet: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    @Environment(\.dismiss) private var dismiss
    let title: String
    let ok: () -> Void
    @State private var wrong = false

    var body: some View {
        NavigationStack {
            ScrollView {
                PinPad(title: title, message: wrong ? "That PIN is not right. Try again." : nil, messageIsError: wrong) { pin in
                    if pin == store.doc.pin { ok() } else { wrong = true }
                }
            }
            .background(look.page.ignoresSafeArea())
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
            }
        }
        .presentationDetents([.large])
    }
}

/// Turning the owner PIN on, changing it or turning it off.
struct PinSettingsView: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    @Environment(\.dismiss) private var dismiss
    @State private var first: String?
    @State private var mismatch = false
    @State private var choosing = false
    @State private var confirmOff = false

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                if choosing {
                    PinPad(title: first == nil ? "Choose a 4-digit PIN" : "Type it again",
                           message: mismatch ? "The two PINs were different. Start again." : "It locks Settings, Products and refunds.",
                           messageIsError: mismatch) { pin in
                        if let f = first {
                            if f == pin {
                                store.update { $0.pin = pin }
                                store.unlocked = true
                                dismiss()
                            } else {
                                first = nil
                                mismatch = true
                            }
                        } else {
                            first = pin
                            mismatch = false
                        }
                    }
                } else {
                    Panel {
                        HStack(spacing: 12) {
                            Image(systemName: store.doc.pin.isEmpty ? "lock.open" : "lock.fill")
                                .font(.system(size: 22)).foregroundStyle(look.primary)
                            VStack(alignment: .leading, spacing: 3) {
                                LText(store.doc.pin.isEmpty ? "No PIN" : "PIN is on", 17, .bold, caps: true)
                                LText(store.doc.pin.isEmpty ? "Anyone with the device can open Settings, change products and refund sales."
                                    : "Settings, Products and refunds ask for the PIN.", 14, color: look.muted)
                            }
                        }
                    }
                    MainButton(store.doc.pin.isEmpty ? "Set a PIN" : "Change PIN", icon: "lock") {
                        first = nil
                        mismatch = false
                        choosing = true
                    }
                    if !store.doc.pin.isEmpty {
                        SoftButton("Turn off PIN", icon: "lock.open", color: look.bad) { confirmOff = true }
                    }
                }
            }
            .padding(16)
        }
        .background(look.page.ignoresSafeArea())
        .navigationTitle("Owner PIN")
        .inlineTitle()
        .confirmationDialog("Turn off the PIN?", isPresented: $confirmOff, titleVisibility: .visible) {
            Button("Turn off", role: .destructive) { store.update { $0.pin = "" } }
        }
    }
}
