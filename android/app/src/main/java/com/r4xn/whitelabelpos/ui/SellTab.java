package com.r4xn.whitelabelpos.ui;

import android.content.Intent;
import android.text.InputType;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.r4xn.whitelabelpos.R;
import com.r4xn.whitelabelpos.Store;
import com.r4xn.whitelabelpos.core.Doc;
import com.r4xn.whitelabelpos.core.Text;
import com.r4xn.whitelabelpos.core.Totals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The selling screen: the shop's header, search, categories and products in the design's layout. */
final class SellTab {
    final View view;
    private final MainActivity a;
    private final Look l;
    private final Store s;
    private final boolean wide;
    private final FrameLayout productsHolder;
    private final FrameLayout cartHolder;
    private final Map<String, Styles.Cell> cells = new HashMap<>();
    private LinearLayout chipsHolder;
    private CartPanel panel;

    SellTab(MainActivity a) {
        this.a = a;
        this.l = a.look;
        this.s = a.store;
        float widthDp = a.getResources().getConfiguration().screenWidthDp;
        wide = widthDp >= 720;

        LinearLayout left = Kit.vbox(a);
        left.addView(Styles.header(l, s.doc.brand), Kit.match());
        left.addView(searchBar(), Kit.match());
        chipsHolder = Kit.vbox(a);
        left.addView(chipsHolder, Kit.match());
        productsHolder = new FrameLayout(a);
        left.addView(productsHolder, Kit.fill());
        cartHolder = new FrameLayout(a);

        if (wide) {
            LinearLayout row = Kit.hbox(a);
            row.setGravity(Gravity.TOP);
            row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
            row.addView(cartHolder, new LinearLayout.LayoutParams(a.look.dp(380), ViewGroup.LayoutParams.MATCH_PARENT));
            view = row;
        } else {
            left.addView(cartHolder, Kit.match());
            view = left;
        }
        paintChips();
        paintProducts();
        refreshCart();
    }

    private View searchBar() {
        LinearLayout bar = Kit.hbox(a);
        LinearLayout.LayoutParams lp = Kit.match();
        bar.setPadding(l.dp(16), l.dp(8), l.dp(16), l.dp(8));
        LinearLayout box = Kit.hbox(a);
        box.setPadding(l.dp(12), 0, l.dp(6), 0);
        if (l.is("lines") || l.is("blocks")) {
            box.setBackground(Kit.outline(l.surface, l.is("blocks") ? l.ink : l.line, l.is("blocks") ? l.dp(2) : Math.max(1, l.dp(1)), l.buttonRadius()));
        } else {
            box.setBackground(Kit.round(l.is("glow") ? l.surface : l.d.page == l.d.surface ? l.raised : l.surface, l.is("menu") ? l.dp(99) : Math.min(l.radius(), l.dp(14))));
        }
        box.addView(Kit.icon(l, R.drawable.ic_search, l.muted, 20));
        EditText q = Kit.field(l, l.t("Search products"), a.sellQuery);
        q.setBackground(null);
        q.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        q.setMinHeight(l.dp(44));
        q.setPadding(l.dp(8), l.dp(8), l.dp(8), l.dp(8));
        q.setLetterSpacing(l.caps ? l.tracking : 0);
        Kit.onText(q, text -> {
            a.sellQuery = text;
            paintProducts();
        });
        box.addView(q, Kit.weight(1));
        bar.addView(box, Kit.weight(1));
        bar.setLayoutParams(lp);
        return bar;
    }

    private void paintChips() {
        chipsHolder.removeAllViews();
        if (s.doc.categories.isEmpty()) return;
        List<View> chips = new ArrayList<>();
        chips.add(Kit.chip(l, "All", a.sellCategory.isEmpty(), () -> pick("")));
        for (Doc.Category c : s.doc.categories) chips.add(Kit.chip(l, c.name, a.sellCategory.equals(c.id), () -> pick(c.id)));
        View row = Kit.chipRow(l, chips);
        row.setPadding(0, 0, 0, l.dp(8));
        chipsHolder.addView(row, Kit.match());
    }

    private void pick(String categoryId) {
        a.sellCategory = categoryId;
        paintChips();
        paintProducts();
    }

    private void paintProducts() {
        productsHolder.removeAllViews();
        cells.clear();
        String q = a.sellQuery.trim().toLowerCase(Locale.getDefault());
        if (!a.sellCategory.isEmpty() && s.doc.category(a.sellCategory) == null) a.sellCategory = "";
        List<Doc.Product> list = new ArrayList<>();
        for (Doc.Product p : s.doc.products) {
            if (!a.sellCategory.isEmpty() && !p.category.equals(a.sellCategory)) continue;
            if (!q.isEmpty() && !p.name.toLowerCase(Locale.getDefault()).contains(q) && !p.sku.toLowerCase(Locale.getDefault()).contains(q)) continue;
            list.add(p);
        }
        LinearLayout inner = Kit.vbox(a);
        inner.setPadding(l.dp(l.is("blocks") ? 10 : 16), l.dp(4), l.dp(l.is("blocks") ? 10 : 16), l.dp(16));
        if (s.doc.products.isEmpty()) {
            TextView t = Kit.body(l, "No products yet. Add what you sell and it shows up here.");
            t.setGravity(Gravity.CENTER);
            t.setPadding(l.dp(20), l.dp(40), l.dp(20), l.dp(16));
            inner.addView(t, Kit.match());
            inner.addView(Kit.primary(l, "Add a product", () -> a.open(MainActivity.PRODUCTS)), Kit.match());
        } else if (list.isEmpty()) {
            TextView t = Kit.muted(l, "Nothing matches “" + a.sellQuery.trim() + "”.");
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, l.dp(40), 0, 0);
            inner.addView(t, Kit.match());
        } else {
            int width = a.getResources().getDisplayMetrics().widthPixels - (wide ? l.dp(380) : 0) - inner.getPaddingLeft() - inner.getPaddingRight();
            inner.addView(Styles.products(l, s, s.doc, s.currency(), list, width, cells, this::tap), Kit.match());
        }
        ScrollView sv = Kit.scroll(l, inner);
        productsHolder.addView(sv, new FrameLayout.LayoutParams(-1, -1));
    }

    private void tap(Doc.Product p) {
        if (p.soldOut()) {
            Kit.toast(a, p.name + " is sold out");
            return;
        }
        if (!s.cart.add(p)) {
            Kit.toast(a, "Only " + p.stock + " in stock");
            return;
        }
        Styles.Cell c = cells.get(p.id);
        if (c != null) {
            c.setQty(s.cart.qty(p.id));
            c.view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        refreshCart();
    }

    /** Updates the cart bar / side panel and every product's in-cart count. */
    void refreshCart() {
        for (Styles.Cell c : cells.values()) c.setQty(s.cart.qty(c.p.id));
        if (wide) {
            if (panel == null) {
                panel = new CartPanel(a, l, s, true, new CartPanel.Listener() {
                    @Override
                    public void changed() {
                        for (Styles.Cell c : cells.values()) c.setQty(s.cart.qty(c.p.id));
                    }

                    @Override
                    public void charge() {
                        a.startActivity(new Intent(a, CheckoutActivity.class));
                    }
                });
                cartHolder.addView(panel.view, new FrameLayout.LayoutParams(-1, -1));
            } else {
                panel.refresh();
            }
            return;
        }
        cartHolder.removeAllViews();
        if (s.cart.isEmpty()) return;
        Totals t = s.cart.totals(s.doc.business, Doc.CARD);
        LinearLayout bar = Kit.hbox(a);
        bar.setPadding(l.dp(14), l.dp(10), l.dp(18), l.dp(10));
        bar.setMinimumHeight(l.dp(58));
        bar.setBackground(Kit.round(l.primary, l.buttonRadius()));
        TextView count = Kit.text(l, String.valueOf(s.cart.count()), 14, l.onAccent, l.bold);
        count.setGravity(Gravity.CENTER);
        count.setMinWidth(l.dp(30));
        count.setMinHeight(l.dp(30));
        count.setBackground(Kit.round(l.accent, l.d.radius == 0 ? 0 : l.dp(99)));
        bar.addView(count);
        TextView label = Kit.text(l, l.t("View sale · " + Text.items(s.cart.count())), 15, l.onPrimary, l.medium);
        label.setLetterSpacing(l.tracking);
        label.setPadding(l.dp(10), 0, 0, 0);
        bar.addView(label, Kit.weight(1));
        TextView total = Kit.text(l, s.money(t.total), 18, l.onPrimary, l.is("blocks") ? l.heavy : l.bold);
        bar.addView(total);
        bar.setOnClickListener(v -> a.startActivity(new Intent(a, CartActivity.class)));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2);
        lp.setMargins(l.dp(l.is("blocks") ? 10 : 16), l.dp(4), l.dp(l.is("blocks") ? 10 : 16), l.dp(10));
        cartHolder.addView(bar, lp);
    }
}
