package com.r4xn.whitelabelpos.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.r4xn.whitelabelpos.core.Currency;
import com.r4xn.whitelabelpos.core.Doc;
import com.r4xn.whitelabelpos.core.Money;
import com.r4xn.whitelabelpos.core.Text;
import com.r4xn.whitelabelpos.core.Totals;

import java.util.ArrayList;
import java.util.List;

/** Take the payment: how the customer pays, cash received and change. */
public class CheckoutActivity extends BaseActivity {
    private String method = Doc.CASH;
    private String received = "";
    private LinearLayout body;
    private TextView complete;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        if (store.cart.isEmpty()) {
            finish();
            return;
        }
        if (saved != null) {
            method = saved.getString("method", Doc.CASH);
            received = saved.getString("received", "");
        }
        LinearLayout v = Kit.vbox(this);
        v.addView(topBar("Payment"), Kit.match());
        body = Kit.vbox(this);
        body.setPadding(look.dp(20), look.dp(4), look.dp(20), look.dp(20));
        v.addView(Kit.scroll(look, body), Kit.fill());
        LinearLayout foot = Kit.vbox(this);
        foot.setPadding(look.dp(20), look.dp(8), look.dp(20), look.dp(16));
        complete = Kit.primary(look, "Complete sale", this::completeSale);
        foot.addView(complete, Kit.match());
        v.addView(foot, Kit.match());
        setScreen(v, look.page);
        paint();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("method", method);
        out.putString("received", received);
    }

    private Totals totals() {
        return store.cart.totals(store.doc.business, method);
    }

    private void paint() {
        body.removeAllViews();
        Totals t = totals();
        Currency cur = store.currency();

        LinearLayout due = Kit.card(look);
        due.setGravity(Gravity.CENTER_HORIZONTAL);
        due.setPadding(look.dp(16), look.dp(22), look.dp(16), look.dp(22));
        TextView label = Kit.text(look, look.t("Amount due"), 13, look.muted, look.medium);
        label.setLetterSpacing(look.tracking + 0.04f);
        due.addView(label);
        TextView amount = Kit.text(look, store.money(t.due), 40, look.ink, look.is("blocks") ? look.heavy : look.bold);
        amount.setGravity(Gravity.CENTER);
        due.addView(amount);
        due.addView(Kit.muted(look, Text.items(store.cart.count()) + (t.rounding != 0 ? " · cash rounding " + (t.rounding > 0 ? "+" : "") + store.money(t.rounding) : "")));
        body.addView(due, Kit.match());

        body.addView(Kit.label(look, "Paid by"));
        List<View> methods = new ArrayList<>();
        for (String m : Doc.METHODS) {
            boolean sel = m.equals(method);
            TextView b = Kit.button(look, Text.methodName(m), sel ? look.onPrimary : look.ink,
                    sel ? Kit.round(look.primary, look.buttonRadius())
                            : look.is("blocks") ? Kit.outline(look.surface, look.ink, look.dp(2), 0)
                            : look.is("lines") || look.is("glow") ? Kit.outline(look.surface, look.line, Math.max(1, look.dp(1)), look.buttonRadius())
                            : Kit.round(look.surface, look.buttonRadius()), () -> {
                        method = m;
                        paint();
                    });
            b.setMinHeight(look.dp(56));
            methods.add(b);
        }
        body.addView(Kit.grid(look, methods, 2, 10), Kit.match());

        if (Doc.CASH.equals(method)) {
            body.addView(Kit.label(look, "Cash received"));
            EditText cash = Kit.moneyField(look, Money.plain(t.due, cur.decimals), received);
            cash.setTextSize(22);
            body.addView(cash, Kit.match());
            List<Long> amounts = new ArrayList<>();
            amounts.add(t.due);
            amounts.addAll(Money.quickCash(t.due, cur.decimals));
            List<View> chips = new ArrayList<>();
            for (long a : amounts) {
                String text = a == t.due ? "Exact" : store.money(a);
                chips.add(Kit.chip(look, text, Money.parse(received, cur.decimals) == a, () -> {
                    received = Money.plain(a, cur.decimals);
                    paint();
                }));
            }
            android.widget.HorizontalScrollView quickRow = Kit.chipRow(look, chips);
            quickRow.getChildAt(0).setPadding(0, 0, 0, 0);
            body.addView(quickRow, Kit.match());
            TextView change = Kit.text(look, "", 20, look.ink, look.bold);
            change.setPadding(look.dp(2), look.dp(18), 0, 0);
            body.addView(change);
            Runnable update = () -> {
                long paid = Money.parse(received, cur.decimals);
                if (received.trim().isEmpty()) {
                    change.setText("Type what the customer gave, or tap Exact.");
                    change.setTextSize(14);
                    change.setTextColor(look.muted);
                    Kit.enable(complete, false);
                } else if (paid < t.due) {
                    change.setText(paid < 0 ? "That isn't an amount." : "Still short by " + store.money(t.due - paid));
                    change.setTextSize(16);
                    change.setTextColor(look.bad);
                    Kit.enable(complete, false);
                } else {
                    change.setText("Change  " + store.money(paid - t.due));
                    change.setTextSize(24);
                    change.setTextColor(look.ink);
                    Kit.enable(complete, true);
                }
            };
            Kit.onText(cash, text -> {
                received = text;
                update.run();
            });
            update.run();
        } else {
            TextView how = Kit.body(look, Doc.QR.equals(method)
                    ? "Show the customer your QR code and check the payment came in, then complete the sale."
                    : Doc.CARD.equals(method)
                    ? "Charge " + store.money(t.due) + " on your card terminal, then complete the sale."
                    : "Ask the customer to pay " + store.money(t.due) + " by e-wallet, check it came in, then complete the sale.");
            how.setPadding(look.dp(2), look.dp(18), 0, 0);
            how.setTextColor(look.muted);
            body.addView(how);
            Kit.enable(complete, true);
        }
        complete.setText(look.t(Doc.CASH.equals(method) ? "Complete sale" : "Mark as paid · " + store.money(t.due)));
    }

    private void completeSale() {
        Totals t = totals();
        long paid = Doc.CASH.equals(method) ? Money.parse(received, store.currency().decimals) : t.due;
        if (Doc.CASH.equals(method) && paid < t.due) return;
        Doc.Sale sale = store.doc.completeSale(store.cart, method, paid, System.currentTimeMillis());
        store.cart.clear();
        store.save();
        Intent i = new Intent(this, ReceiptActivity.class);
        i.putExtra("sale", sale.id);
        i.putExtra("justSold", true);
        startActivity(i);
        finish();
    }
}
