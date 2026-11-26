package com.r4xn.whitelabelpos.ui;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.r4xn.whitelabelpos.Store;
import com.r4xn.whitelabelpos.core.Colors;
import com.r4xn.whitelabelpos.core.Doc;
import com.r4xn.whitelabelpos.core.Reports;
import com.r4xn.whitelabelpos.core.Text;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Takings for today, 7 days or 30 days. */
final class ReportsTab {
    private ReportsTab() {
    }

    static View build(MainActivity a) {
        Look l = a.look;
        Store s = a.store;
        LinearLayout body = Kit.vbox(a);
        body.setPadding(l.dp(20), l.dp(16), l.dp(20), l.dp(24));
        body.addView(Kit.title(l, "Reports"));

        List<View> chips = new ArrayList<>();
        int[] days = {1, 7, 30};
        String[] names = {"Today", "7 days", "30 days"};
        for (int i = 0; i < days.length; i++) {
            int d = days[i];
            chips.add(Kit.chip(l, names[i], a.reportDays == d, () -> {
                a.reportDays = d;
                a.open(MainActivity.REPORTS);
            }));
        }
        View chipRow = Kit.chipRow(l, chips);
        ((LinearLayout) ((android.widget.HorizontalScrollView) chipRow).getChildAt(0)).setPadding(0, l.dp(12), 0, l.dp(4));
        body.addView(chipRow, Kit.match());

        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        long from = today.minusDays(a.reportDays - 1).atStartOfDay(zone).toInstant().toEpochMilli();
        long to = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        Reports.Report r = Reports.report(s.doc.sales, from, to);

        List<View> stats = new ArrayList<>();
        stats.add(stat(l, "Sales", s.money(r.gross), true));
        stats.add(stat(l, "Orders", String.valueOf(r.sales), false));
        stats.add(stat(l, "Average sale", s.money(r.average), false));
        stats.add(stat(l, "Tax collected", s.money(r.tax), false));
        LinearLayout.LayoutParams gp = Kit.match();
        gp.topMargin = l.dp(10);
        body.addView(Kit.grid(l, stats, 2, 10), gp);

        body.addView(Kit.label(l, a.reportDays == 1 ? "By hour" : "By day"));
        LinearLayout chartCard = Kit.card(l);
        List<Reports.Bar> bars = a.reportDays == 1
                ? Reports.hourly(s.doc.sales, from, zone)
                : Reports.daily(s.doc.sales, today, a.reportDays, zone);
        BarChart chart = new BarChart(l, bars, a.reportDays == 1 ? 3 : a.reportDays == 7 ? 1 : 5);
        chartCard.addView(chart, new LinearLayout.LayoutParams(-1, l.dp(160)));
        body.addView(chartCard, Kit.match());

        body.addView(Kit.label(l, "Payments"));
        LinearLayout pay = Kit.card(l);
        if (r.byMethod.isEmpty()) pay.addView(Kit.muted(l, "No sales in this time."));
        boolean first = true;
        for (String m : Doc.METHODS) {
            Long amount = r.byMethod.get(m);
            if (amount == null) continue;
            if (!first) pay.addView(Kit.gap(l, 10));
            first = false;
            pay.addView(share(l, Text.methodName(m), s.money(amount), r.gross == 0 ? 0 : (float) amount / r.gross));
        }
        body.addView(pay, Kit.match());

        body.addView(Kit.label(l, "Best sellers"));
        LinearLayout top = Kit.card(l);
        if (r.top.isEmpty()) top.addView(Kit.muted(l, "Nothing sold yet."));
        for (int i = 0; i < Math.min(5, r.top.size()); i++) {
            Reports.Top t = r.top.get(i);
            if (i > 0) top.addView(Kit.divider(l));
            LinearLayout row = Kit.hbox(a);
            row.setPadding(0, l.dp(9), 0, l.dp(9));
            TextView rank = Kit.text(l, String.valueOf(i + 1), 14, l.muted, l.bold);
            rank.setMinWidth(l.dp(24));
            row.addView(rank);
            row.addView(Kit.body(l, t.name), Kit.weight(1));
            row.addView(Kit.muted(l, t.qty + " sold   "));
            row.addView(Kit.text(l, s.money(t.amount), 15, l.ink, l.medium));
            top.addView(row, Kit.match());
        }
        body.addView(top, Kit.match());

        if (r.refunds > 0 || r.discount > 0) {
            body.addView(Kit.label(l, "Money given back"));
            LinearLayout back = Kit.card(l);
            if (r.refunds > 0) back.addView(line(l, r.refunds == 1 ? "1 refund" : r.refunds + " refunds", s.money(r.refunded)));
            if (r.discount > 0) back.addView(line(l, "Discounts given", s.money(r.discount)));
            body.addView(back, Kit.match());
        }
        return Kit.scroll(l, body);
    }

    private static View stat(Look l, String label, String value, boolean brand) {
        LinearLayout c = Kit.card(l);
        if (brand) c.setBackground(Kit.round(l.primary, l.radius()));
        TextView a = Kit.text(l, l.t(label), 12, brand ? Colors.withAlpha(l.onPrimary, 0xCC) : l.muted, l.medium);
        a.setLetterSpacing(l.tracking);
        c.addView(a);
        TextView b = Kit.text(l, value, 21, brand ? l.onPrimary : l.ink, l.is("blocks") ? l.heavy : l.bold);
        b.setPadding(0, l.dp(4), 0, 0);
        b.setMaxLines(1);
        b.setAutoSizeTextTypeUniformWithConfiguration(12, 21, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        c.addView(b);
        return c;
    }

    private static View share(Look l, String name, String amount, float part) {
        LinearLayout v = Kit.vbox(l.c);
        LinearLayout row = Kit.hbox(l.c);
        row.addView(Kit.body(l, name), Kit.weight(1));
        row.addView(Kit.text(l, amount, 15, l.ink, l.medium));
        v.addView(row, Kit.match());
        LinearLayout track = Kit.hbox(l.c);
        track.setBackground(Kit.round(l.raised, l.d.radius == 0 ? 0 : l.dp(99)));
        View fill = new View(l.c);
        fill.setBackground(Kit.round(l.primary, l.d.radius == 0 ? 0 : l.dp(99)));
        track.addView(fill, new LinearLayout.LayoutParams(0, l.dp(8), Math.max(0.001f, part)));
        track.addView(new View(l.c), new LinearLayout.LayoutParams(0, l.dp(8), Math.max(0.001f, 1 - part)));
        LinearLayout.LayoutParams tp = Kit.match();
        tp.topMargin = l.dp(6);
        v.addView(track, tp);
        return v;
    }

    private static View line(Look l, String a, String b) {
        LinearLayout row = Kit.hbox(l.c);
        row.setPadding(0, l.dp(6), 0, l.dp(6));
        row.addView(Kit.body(l, a), Kit.weight(1));
        row.addView(Kit.text(l, b, 15, l.ink, l.medium));
        return row;
    }
}
