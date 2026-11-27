import POSCore
import SwiftUI

/// Every sale, newest first, grouped by day.
struct OrdersView: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    @State private var search = ""

    private struct Day: Identifiable {
        var id: Date
        var title: String
        var sales: [Sale]
        var total: Int
    }

    private var days: [Day] {
        let cal = Calendar.current
        let q = search.filter(\.isNumber)
        let list = store.doc.sales
            .filter { q.isEmpty || String($0.number).contains(q) }
            .sorted { $0.time > $1.time }
        var out: [Day] = []
        for s in list {
            let d = cal.startOfDay(for: s.date)
            if out.last?.id != d {
                out.append(Day(id: d, title: Self.dayTitle(d, cal), sales: [], total: 0))
            }
            out[out.count - 1].sales.append(s)
            if !s.refunded { out[out.count - 1].total += s.due }
        }
        return out
    }

    static func dayTitle(_ d: Date, _ cal: Calendar) -> String {
        if cal.isDateInToday(d) { return "Today" }
        if cal.isDateInYesterday(d) { return "Yesterday" }
        let f = DateFormatter()
        f.dateFormat = "EEEE, d MMM"
        return f.string(from: d)
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 8) {
                    Image(systemName: "magnifyingglass").foregroundStyle(look.muted)
                    TextField("Find a receipt number", text: $search)
                        .numberKeyboard()
                        .font(look.font(16))
                }
                .fieldLook()
                let all = days
                if all.isEmpty {
                    EmptyMessage(icon: "list.bullet.rectangle", title: store.doc.sales.isEmpty ? "No sales yet" : "Nothing found",
                                 message: store.doc.sales.isEmpty ? "Sales show up here as soon as you charge." : "No receipt has that number.")
                }
                ForEach(all) { day in
                    HStack(alignment: .lastTextBaseline) {
                        SectionLabel(day.title)
                        LText(store.money(day.total), 13, .semibold, color: look.muted)
                            .padding(.trailing, 4)
                            .fixedSize()
                    }
                    Panel(padding: 0) {
                        ForEach(Array(day.sales.enumerated()), id: \.element.id) { i, s in
                            if i > 0 { Hairline().padding(.leading, 16) }
                            NavigationLink(value: s.id) {
                                OrderRow(sale: s)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
            .padding(16)
        }
        .background(look.page.ignoresSafeArea())
        .navigationTitle("Orders")
    }
}

struct OrderRow: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    let sale: Sale

    private static let time: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "h:mm a"
        return f
    }()

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 8) {
                    LText("#\(sale.number)", 16, .bold)
                    LText(Self.time.string(from: sale.date), 14, color: look.muted)
                }
                LText("\(sale.itemCount) \(sale.itemCount == 1 ? "item" : "items") · \(sale.method.label)", 13, color: look.muted)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 3) {
                LText(store.money(sale.due), 16, .semibold, color: sale.refunded ? look.muted : look.ink)
                    .strikethrough(sale.refunded)
                if sale.refunded {
                    LText("Refunded", 12, .bold, color: look.bad, caps: true)
                }
            }
            Image(systemName: "chevron.right").font(.system(size: 12, weight: .semibold)).foregroundStyle(look.muted)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }
}
