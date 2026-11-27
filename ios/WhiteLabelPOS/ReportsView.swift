import Charts
import POSCore
import SwiftUI

/// How the shop is doing: today, the last 7 days or the last 30 days.
struct ReportsView: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    private var period: Int { store.reportPeriod }

    private static let periods = ["Today", "7 days", "30 days"]

    var body: some View {
        let cal = Calendar.current
        let today = cal.startOfDay(for: Date())
        let days = period == 0 ? 1 : (period == 1 ? 7 : 30)
        let from = cal.date(byAdding: .day, value: -(days - 1), to: today) ?? today
        let to = cal.date(byAdding: .day, value: 1, to: today) ?? today
        let fromMs = Int(from.timeIntervalSince1970 * 1000)
        let toMs = Int(to.timeIntervalSince1970 * 1000)
        let report = Report.make(store.doc.sales, from: fromMs, to: toMs)
        let bars = period == 0
            ? ReportChart.hourly(store.doc.sales, day: today, calendar: cal)
            : ReportChart.daily(store.doc.sales, from: from, days: days, calendar: cal)
        return ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: look.design.chips == "underline" ? 14 : 8) {
                    ForEach(0..<3) { i in
                        ChipView(title: Self.periods[i], selected: period == i) { store.reportPeriod = i }
                    }
                }
                LazyVGrid(columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)], spacing: 10) {
                    StatCard(label: "Sales", value: store.money(report.gross), big: true)
                    StatCard(label: "Orders", value: "\(report.sales)")
                    StatCard(label: "Average sale", value: store.money(report.average))
                    StatCard(label: "\(store.doc.business.taxName.isEmpty ? "Tax" : store.doc.business.taxName) collected", value: store.money(report.tax))
                }
                .padding(.top, 14)

                SectionLabel(period == 0 ? "Sales by hour" : "Sales by day")
                Panel {
                    chart(bars, hourly: period == 0)
                        .frame(height: 190)
                }

                SectionLabel("How customers paid")
                Panel {
                    if report.methods.isEmpty {
                        LText("No sales yet.", 15, color: look.muted)
                    }
                    ForEach(report.methods, id: \.method) { m in
                        MethodBar(label: m.method.label, amount: m.amount, gross: report.gross)
                    }
                }

                SectionLabel("Best sellers")
                Panel(padding: 0) {
                    if report.top.isEmpty {
                        LText("No sales yet.", 15, color: look.muted).padding(16)
                    }
                    ForEach(Array(report.top.prefix(5).enumerated()), id: \.offset) { i, t in
                        if i > 0 { Hairline().padding(.leading, 16) }
                        HStack(spacing: 12) {
                            LText("\(i + 1)", 15, .bold, color: look.primary).frame(width: 22)
                            LText(t.name, 15, .medium, caps: true).lineLimit(1)
                            Spacer()
                            LText("\(t.qty) sold", 13, color: look.muted)
                            LText(store.money(t.amount), 15, .semibold).frame(minWidth: 80, alignment: .trailing)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 11)
                    }
                }

                if report.refunds > 0 || report.discount > 0 {
                    SectionLabel("Money given back")
                    Panel {
                        if report.discount > 0 {
                            AmountRow("Discounts", store.money(report.discount))
                        }
                        if report.refunds > 0 {
                            AmountRow(report.refunds == 1 ? "1 refund" : "\(report.refunds) refunds", store.money(report.refunded), color: look.bad)
                        }
                    }
                }
            }
            .padding(16)
        }
        .background(look.page.ignoresSafeArea())
        .navigationTitle("Reports")
    }

    private func chart(_ bars: [Bucket], hourly: Bool) -> some View {
        let unit = Double(pow(10, Double(store.currency.decimals)))
        return Chart(bars) { b in
            BarMark(x: .value(hourly ? "Hour" : "Day", b.start, unit: hourly ? .hour : .day),
                    y: .value("Sales", Double(b.amount) / unit))
                .foregroundStyle(look.primary.gradient)
                .cornerRadius(min(look.radius / 4, 4))
        }
        .chartYAxis {
            AxisMarks(position: .leading) { _ in
                AxisGridLine().foregroundStyle(look.line)
                AxisValueLabel().foregroundStyle(look.muted)
            }
        }
        .chartXAxis {
            AxisMarks(values: .automatic(desiredCount: hourly ? 6 : (bars.count > 7 ? 6 : 7))) { _ in
                AxisValueLabel(format: hourly ? .dateTime.hour() : (bars.count > 7 ? .dateTime.day() : .dateTime.weekday(.abbreviated)))
                    .foregroundStyle(look.muted)
            }
        }
    }
}

struct StatCard: View {
    @Environment(\.look) private var look
    let label: String
    let value: String
    var big = false

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            LText(label, 12, .medium, color: big ? look.onPrimary.opacity(0.85) : look.muted, caps: true)
            LText(value, 22, .bold, color: big ? look.onPrimary : look.ink)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(big ? look.primary : look.surface, in: RoundedRectangle(cornerRadius: min(look.radius, 18), style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: min(look.radius, 18), style: .continuous).stroke(big ? Color.clear : look.line))
    }
}

struct MethodBar: View {
    @Environment(Store.self) private var store
    @Environment(\.look) private var look
    let label: String
    let amount: Int
    let gross: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                LText(label, 15, .medium, caps: true)
                Spacer()
                LText(store.money(amount), 15, .semibold)
            }
            GeometryReader { g in
                ZStack(alignment: .leading) {
                    Capsule().fill(look.raised)
                    Capsule().fill(look.primary)
                        .frame(width: max(6, g.size.width * CGFloat(gross > 0 ? Double(amount) / Double(gross) : 0)))
                }
            }
            .frame(height: 8)
        }
        .padding(.vertical, 6)
    }
}
