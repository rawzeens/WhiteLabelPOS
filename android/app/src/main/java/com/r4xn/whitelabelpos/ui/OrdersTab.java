package com.r4xn.whitelabelpos.ui;

import android.content.Intent;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.r4xn.whitelabelpos.Store;
import com.r4xn.whitelabelpos.core.Doc;
import com.r4xn.whitelabelpos.core.Text;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Past sales, newest first, grouped by day. */
final class OrdersTab {
    private OrdersTab() {
    }

    static View build(MainActivity a) {
        Look l = a.look;
        Store s = a.store;
        LinearLayout v = Kit.vbox(a);
        LinearLayout head = Kit.vbox(a);
        head.setPadding(l.dp(20), l.dp(16), l.dp(20), l.dp(8));
        head.addView(Kit.title(l, "Orders"));
        EditText q = Kit.field(l, l.t("Find a receipt number"), a.ordersQuery);
        q.setInputType(InputType.TYPE_CLASS_NUMBER);
        LinearLayout.LayoutParams qp = Kit.match();
        qp.topMargin = l.dp(12);
        head.addView(q, qp);
        v.addView(head, Kit.match());
        FrameLayout listHolder = new FrameLayout(a);
        v.addView(listHolder, Kit.fill());
        int[] shown = {120};
        Runnable[] paint = new Runnable[1];
        paint[0] = () -> {
            listHolder.removeAllViews();
            listHolder.addView(Kit.scroll(l, list(a, l, s, shown, paint[0])), new FrameLayout.LayoutParams(-1, -1));
        };
        Kit.onText(q, text -> {
            a.ordersQuery = text.trim();
            paint[0].run();
        });
        paint[0].run();
        return v;
    }

    private static View list(MainActivity a, Look l, Store s, int[] shown, Runnable repaint) {
        LinearLayout list = Kit.vbox(a);
        list.setPadding(l.dp(20), 0, l.dp(20), l.dp(24));
        List<Doc.Sale> sales = new ArrayList<>();
        for (int i = s.doc.sales.size() - 1; i >= 0; i--) {
            Doc.Sale sale = s.doc.sales.get(i);
            if (!a.ordersQuery.isEmpty() && !String.valueOf(sale.number).contains(a.ordersQuery)) continue;
            sales.add(sale);
        }
        sales.sort((x, y) -> Long.compare(y.time, x.time));
        if (sales.isEmpty()) {
            TextView t = Kit.muted(l, a.ordersQuery.isEmpty() ? "No sales yet. They appear here as soon as you complete one." : "No receipt with that number.");
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, l.dp(48), 0, 0);
            list.addView(t, Kit.match());
            return list;
        }
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.getDefault());
        DateTimeFormatter timeFmt = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault());
        LocalDate lastDay = null;
        LinearLayout card = null;
        long dayTotal = 0;
        TextView dayTotalView = null;
        int count = 0;
        for (Doc.Sale sale : sales) {
            if (count++ >= shown[0]) {
                list.addView(Kit.secondary(l, "Show more", () -> {
                    shown[0] += 200;
                    repaint.run();
                }), Kit.match());
                break;
            }
            var at = Instant.ofEpochMilli(sale.time).atZone(zone);
            LocalDate day = at.toLocalDate();
            if (!day.equals(lastDay)) {
                if (dayTotalView != null) dayTotalView.setText(s.money(dayTotal));
                lastDay = day;
                dayTotal = 0;
                LinearLayout dh = Kit.hbox(a);
                String name = day.equals(today) ? "Today" : day.equals(today.minusDays(1)) ? "Yesterday" : dayFmt.format(day);
                TextView label = Kit.label(l, name);
                dh.addView(label, Kit.weight(1));
                dayTotalView = Kit.text(l, "", 12, l.muted, l.medium);
                dayTotalView.setPadding(0, l.dp(18), l.dp(2), l.dp(8));
                dh.addView(dayTotalView);
                list.addView(dh, Kit.match());
                card = Kit.card(l);
                card.setPadding(l.dp(14), l.dp(4), l.dp(14), l.dp(4));
                list.addView(card, Kit.match());
            } else {
                card.addView(Kit.divider(l));
            }
            if (!sale.refunded) dayTotal += sale.due;
            LinearLayout row = Kit.hbox(a);
            row.setPadding(0, l.dp(12), 0, l.dp(12));
            LinearLayout texts = Kit.vbox(a);
            TextView num = Kit.text(l, "#" + sale.number + "  ·  " + timeFmt.format(at), 15, l.ink, l.medium);
            texts.addView(num);
            texts.addView(Kit.muted(l, Text.items(sale.itemCount()) + " · " + Text.methodName(sale.method)));
            row.addView(texts, Kit.weight(1));
            if (sale.refunded) {
                row.addView(Kit.pill(l, "Refunded", l.bad, l.d.dark ? 0x33DC2626 : 0xFFFEE2E2));
                row.addView(Kit.gap(l, 8));
            }
            TextView amt = Kit.text(l, s.money(sale.due), 16, sale.refunded ? l.muted : l.ink, l.bold);
            if (sale.refunded) amt.setPaintFlags(amt.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            row.addView(amt);
            row.setOnClickListener(x -> {
                Intent i = new Intent(a, ReceiptActivity.class);
                i.putExtra("sale", sale.id);
                a.startActivity(i);
            });
            card.addView(row, Kit.match());
        }
        if (dayTotalView != null) dayTotalView.setText(s.money(dayTotal));
        return list;
    }
}
