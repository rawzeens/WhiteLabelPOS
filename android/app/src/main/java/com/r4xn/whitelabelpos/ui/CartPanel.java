package com.r4xn.whitelabelpos.ui;

import android.app.Dialog;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.r4xn.whitelabelpos.R;
import com.r4xn.whitelabelpos.Store;
import com.r4xn.whitelabelpos.core.Cart;
import com.r4xn.whitelabelpos.core.Doc;
import com.r4xn.whitelabelpos.core.Money;
import com.r4xn.whitelabelpos.core.Text;
import com.r4xn.whitelabelpos.core.Totals;

import java.util.ArrayList;

/** The sale being rung up: lines, discount, note, totals and the Charge button. */
final class CartPanel {
    interface Listener {
        void changed();

        void charge();
    }

    final FrameLayout view;
    private final BaseActivity a;
    private final Look l;
    private final Store s;
    private final Listener listener;
    private final boolean embedded;

    CartPanel(BaseActivity a, Look l, Store s, boolean embedded, Listener listener) {
        this.a = a;
        this.l = l;
        this.s = s;
        this.embedded = embedded;
        this.listener = listener;
        view = new FrameLayout(a);
        refresh();
    }

    void refresh() {
        view.removeAllViews();
        Cart cart = s.cart;
        LinearLayout col = Kit.vbox(a);
        if (embedded) {
            col.setBackground(Kit.outline(l.surface, l.line, Math.max(1, l.dp(1)), 0));
        }
        LinearLayout head = Kit.hbox(a);
        head.setPadding(l.dp(16), l.dp(embedded ? 16 : 4), l.dp(8), l.dp(4));
        if (embedded) head.addView(Kit.heading(l, "This sale"), Kit.weight(1));
        else head.addView(Kit.muted(l, Text.items(cart.count())), Kit.weight(1));
        if (!cart.isEmpty()) head.addView(Kit.link(l, "Clear", () -> {
            cart.clear();
            changed();
        }));
        col.addView(head, Kit.match());

        LinearLayout lines = Kit.vbox(a);
        lines.setPadding(l.dp(16), 0, l.dp(16), l.dp(8));
        if (cart.isEmpty()) {
            TextView empty = Kit.muted(l, "Tap a product to add it to this sale.");
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, l.dp(40), 0, l.dp(40));
            lines.addView(empty, Kit.match());
        }
        for (Cart.Item it : new ArrayList<>(cart.items)) {
            lines.addView(line(it), Kit.match());
            lines.addView(Kit.divider(l));
        }
        if (!cart.isEmpty()) {
            LinearLayout extras = Kit.hbox(a);
            extras.addView(Kit.link(l, cart.discountType.equals("none") ? "Add discount" : "Change discount", this::discount));
            extras.addView(Kit.link(l, cart.note.isEmpty() ? "Add note" : "Change note", this::note));
            lines.addView(extras);
            if (!cart.note.isEmpty()) {
                TextView n = Kit.muted(l, "Note: " + cart.note);
                n.setPadding(l.dp(8), 0, 0, l.dp(6));
                lines.addView(n);
            }
        }
        ScrollView sv = Kit.scroll(l, lines);
        col.addView(sv, Kit.fill());

        LinearLayout foot = Kit.vbox(a);
        foot.setPadding(l.dp(16), l.dp(8), l.dp(16), l.dp(16));
        Doc.Business b = s.doc.business;
        Totals t = cart.totals(b, Doc.CARD);
        if (!cart.isEmpty()) {
            foot.addView(Kit.divider(l));
            foot.addView(row("Subtotal", s.money(t.subtotal), false));
            if (t.discount > 0) {
                String what = cart.discountType.equals("percent") ? "Discount " + Text.percent((int) cart.discountValue) : "Discount";
                foot.addView(row(what, "-" + s.money(t.discount), false));
            }
            if (b.taxRate > 0) foot.addView(row(Text.taxLine(b.taxName, b.taxRate, b.taxInclusive), s.money(t.tax), false));
            foot.addView(row("Total", s.money(t.total), true));
            foot.addView(Kit.gap(l, 10));
        }
        TextView charge = Kit.primary(l, cart.isEmpty() ? "Charge" : "Charge " + s.money(t.total), listener::charge);
        Kit.enable(charge, !cart.isEmpty());
        foot.addView(charge, Kit.match());
        col.addView(foot, Kit.match());
        view.addView(col, new FrameLayout.LayoutParams(-1, -1));
    }

    private View line(Cart.Item it) {
        LinearLayout v = Kit.vbox(a);
        v.setPadding(0, l.dp(10), 0, l.dp(10));
        LinearLayout top = Kit.hbox(a);
        top.setGravity(Gravity.TOP);
        TextView name = Kit.text(l, l.t(it.name), 15, l.ink, l.medium);
        name.setLetterSpacing(l.tracking);
        top.addView(name, Kit.weight(1));
        top.addView(Kit.text(l, s.money(it.price * it.qty), 15, l.ink, l.bold));
        v.addView(top);
        LinearLayout bottom = Kit.hbox(a);
        bottom.addView(Kit.muted(l, s.money(it.price) + " each"), Kit.weight(1));
        bottom.addView(Kit.iconButton(l, R.drawable.ic_minus, l.ink, l.raised, () -> {
            s.cart.change(s.doc, it.productId, -1);
            changed();
        }));
        TextView q = Kit.text(l, String.valueOf(it.qty), 16, l.ink, l.bold);
        q.setGravity(Gravity.CENTER);
        q.setMinWidth(l.dp(40));
        bottom.addView(q);
        bottom.addView(Kit.iconButton(l, R.drawable.ic_plus, l.ink, l.raised, () -> {
            if (!s.cart.change(s.doc, it.productId, 1)) {
                Doc.Product p = s.doc.product(it.productId);
                Kit.toast(a, "Only " + (p == null ? 0 : p.stock) + " in stock");
                return;
            }
            changed();
        }));
        bottom.setPadding(0, l.dp(4), 0, 0);
        v.addView(bottom);
        return v;
    }

    private View row(String label, String value, boolean big) {
        LinearLayout r = Kit.hbox(a);
        r.setPadding(0, l.dp(big ? 8 : 4), 0, 0);
        TextView a1 = big ? Kit.heading(l, label) : Kit.text(l, label, 14, l.muted, l.regular);
        r.addView(a1, Kit.weight(1));
        TextView b1 = Kit.text(l, value, big ? 20 : 14, l.ink, big ? l.bold : l.regular);
        r.addView(b1);
        return r;
    }

    private void changed() {
        refresh();
        listener.changed();
    }

    private void discount() {
        Cart cart = s.cart;
        int decimals = s.currency().decimals;
        String[] type = {cart.discountType.equals("percent") ? "percent" : "amount"};
        LinearLayout box = Kit.vbox(a);
        LinearLayout chips = Kit.hbox(a);
        EditText value = Kit.moneyField(l, "", cart.discountType.equals("none") ? ""
                : cart.discountType.equals("percent") ? Text.percent((int) cart.discountValue).replace("%", "")
                : Money.plain(cart.discountValue, decimals));
        Runnable[] paint = new Runnable[1];
        paint[0] = () -> {
            chips.removeAllViews();
            chips.addView(Kit.chip(l, "Amount off", type[0].equals("amount"), () -> {
                type[0] = "amount";
                paint[0].run();
            }));
            chips.addView(Kit.gap(l, 8));
            chips.addView(Kit.chip(l, "% off", type[0].equals("percent"), () -> {
                type[0] = "percent";
                paint[0].run();
            }));
            value.setHint(type[0].equals("percent") ? "10" : Money.plain(500, decimals));
        };
        paint[0].run();
        box.addView(chips);
        box.addView(Kit.gap(l, 12));
        box.addView(value, Kit.match());
        if (!cart.discountType.equals("none")) {
            box.addView(Kit.link(l, "Remove discount", () -> {
            }));
        }
        Dialog[] d = new Dialog[1];
        d[0] = Kit.sheet(a, l, "Discount", box, "Apply", () -> {
            String text = value.getText().toString();
            if (type[0].equals("percent")) {
                int bp = Text.parsePercent(text);
                if (bp <= 0) {
                    Kit.toast(a, "Type a percent between 0 and 100");
                    return;
                }
                cart.discountType = "percent";
                cart.discountValue = bp;
            } else {
                long v = Money.parse(text, decimals);
                if (v <= 0) {
                    Kit.toast(a, "Type the amount to take off");
                    return;
                }
                cart.discountType = "amount";
                cart.discountValue = v;
            }
            d[0].dismiss();
            changed();
        }, false);
        if (!cart.discountType.equals("none")) {
            View remove = box.getChildAt(box.getChildCount() - 1);
            remove.setOnClickListener(v -> {
                cart.discountType = "none";
                cart.discountValue = 0;
                d[0].dismiss();
                changed();
            });
        }
        value.requestFocus();
    }

    private void note() {
        Kit.ask(a, l, "Note on this sale", "e.g. Table 4, less sugar", s.cart.note, "Save", text -> {
            s.cart.note = text;
            changed();
        });
    }
}
