package com.r4xn.whitelabelpos.ui;

import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.r4xn.whitelabelpos.R;
import com.r4xn.whitelabelpos.Store;
import com.r4xn.whitelabelpos.core.Colors;
import com.r4xn.whitelabelpos.core.Doc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Everything the shop sells, grouped by category. */
final class ProductsTab {
    private ProductsTab() {
    }

    static View build(MainActivity a) {
        Look l = a.look;
        Store s = a.store;
        LinearLayout v = Kit.vbox(a);
        LinearLayout head = Kit.vbox(a);
        head.setPadding(l.dp(20), l.dp(16), l.dp(20), l.dp(4));
        LinearLayout titleRow = Kit.hbox(a);
        titleRow.addView(Kit.title(l, "Products"), Kit.weight(1));
        titleRow.addView(Kit.link(l, "Categories", () -> a.startActivity(new Intent(a, CategoriesActivity.class))));
        head.addView(titleRow, Kit.match());
        EditText q = Kit.field(l, l.t("Search products"), "");
        LinearLayout.LayoutParams qp = Kit.match();
        qp.topMargin = l.dp(10);
        head.addView(q, qp);
        v.addView(head, Kit.match());
        FrameLayout holder = new FrameLayout(a);
        v.addView(holder, Kit.fill());
        Runnable[] paint = new Runnable[1];
        paint[0] = () -> {
            holder.removeAllViews();
            holder.addView(Kit.scroll(l, list(a, l, s, q.getText().toString())), new FrameLayout.LayoutParams(-1, -1));
        };
        Kit.onText(q, t -> paint[0].run());
        paint[0].run();
        LinearLayout foot = Kit.vbox(a);
        foot.setPadding(l.dp(20), l.dp(6), l.dp(20), l.dp(10));
        foot.addView(Kit.primary(l, "Add product", () -> a.startActivity(new Intent(a, EditProductActivity.class))), Kit.match());
        v.addView(foot, Kit.match());
        return v;
    }

    private static View list(MainActivity a, Look l, Store s, String query) {
        LinearLayout list = Kit.vbox(a);
        list.setPadding(l.dp(20), 0, l.dp(20), l.dp(20));
        String q = query.trim().toLowerCase(Locale.getDefault());
        List<Doc.Product> found = new ArrayList<>();
        for (Doc.Product p : s.doc.products) {
            if (q.isEmpty() || p.name.toLowerCase(Locale.getDefault()).contains(q) || p.sku.toLowerCase(Locale.getDefault()).contains(q)) found.add(p);
        }
        if (found.isEmpty()) {
            TextView t = Kit.muted(l, s.doc.products.isEmpty() ? "No products yet. Tap Add product to start." : "Nothing matches.");
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, l.dp(48), 0, 0);
            list.addView(t, Kit.match());
            return list;
        }
        for (Map.Entry<String, List<Doc.Product>> e : Styles.group(s.doc, found).entrySet()) {
            list.addView(Kit.label(l, (e.getKey().isEmpty() ? "No category" : e.getKey()) + "  ·  " + e.getValue().size()));
            LinearLayout card = Kit.card(l);
            card.setPadding(l.dp(12), l.dp(2), l.dp(12), l.dp(2));
            boolean first = true;
            for (Doc.Product p : e.getValue()) {
                if (!first) card.addView(Kit.divider(l));
                first = false;
                card.addView(row(a, l, s, p), Kit.match());
            }
            list.addView(card, Kit.match());
        }
        return list;
    }

    private static View row(MainActivity a, Look l, Store s, Doc.Product p) {
        LinearLayout row = Kit.hbox(a);
        row.setPadding(0, l.dp(10), 0, l.dp(10));
        int color = Colors.parse(p.color, l.primary);
        TextView icon = Kit.productIcon(l, p, 20, color);
        icon.setBackground(Kit.round(Colors.soft(color, l.surface, l.d.dark), l.d.radius == 0 ? 0 : l.dp(12)));
        row.addView(icon, new LinearLayout.LayoutParams(l.dp(42), l.dp(42)));
        LinearLayout texts = Kit.vbox(a);
        texts.setPadding(l.dp(12), 0, l.dp(8), 0);
        texts.addView(Kit.text(l, p.name, 15, l.ink, l.medium));
        String sub = s.money(p.price) + (p.sku.isEmpty() ? "" : "  ·  " + p.sku);
        texts.addView(Kit.muted(l, sub));
        row.addView(texts, Kit.weight(1));
        if (p.trackStock) {
            if (p.stock <= 0) row.addView(Kit.pill(l, "Sold out", l.bad, l.d.dark ? 0x33DC2626 : 0xFFFEE2E2));
            else row.addView(Kit.pill(l, p.stock + " left", p.stock <= 5 ? 0xFFB45309 : l.muted, p.stock <= 5 ? (l.d.dark ? 0x33F59E0B : 0xFFFEF3C7) : l.raised));
        }
        row.addView(Kit.icon(l, R.drawable.ic_chevron, l.muted, 18));
        row.setOnClickListener(v -> {
            Intent i = new Intent(a, EditProductActivity.class);
            i.putExtra("id", p.id);
            a.startActivity(i);
        });
        return row;
    }
}
