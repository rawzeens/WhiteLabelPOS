package com.r4xn.whitelabelpos.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.r4xn.whitelabelpos.R;
import com.r4xn.whitelabelpos.core.Doc;

/** Shows a receipt after a sale, or an old one from Orders (with Refund). */
public class ReceiptActivity extends BaseActivity {
    private Doc.Sale sale;
    private boolean justSold;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        sale = store.doc.sale(getIntent().getStringExtra("sale"));
        justSold = getIntent().getBooleanExtra("justSold", false);
        if (sale == null) {
            finish();
            return;
        }
        build();
    }

    private void build() {
        LinearLayout v = Kit.vbox(this);
        String title = justSold ? "Sale complete" : "Receipt #" + sale.number;
        v.addView(topBar(title, Kit.iconButton(look, R.drawable.ic_share, look.ink, 0, () -> Receipts.share(this, look, store, sale))), Kit.match());

        LinearLayout inner = Kit.vbox(this);
        inner.setPadding(look.dp(20), look.dp(4), look.dp(20), look.dp(20));
        inner.setGravity(Gravity.CENTER_HORIZONTAL);
        if (justSold) {
            TextView done = Kit.text(look, Doc.CASH.equals(sale.method) && sale.change > 0
                    ? "Give change  " + store.money(sale.change) : look.t("Paid · " + store.money(sale.due)), 22, look.ink, look.bold);
            done.setGravity(Gravity.CENTER);
            done.setPadding(0, look.dp(4), 0, look.dp(14));
            inner.addView(done, Kit.match());
        }
        LinearLayout paperWrap = new LinearLayout(this);
        paperWrap.setPadding(1, 1, 1, 1);
        paperWrap.setBackground(Kit.outline(0xFFFFFFFF, look.d.dark ? look.line : 0xFFE5E5E5, Math.max(1, look.dp(1)), look.dp(6)));
        paperWrap.addView(Receipts.build(look, store.doc, store.currency(), sale), Kit.match());
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(Math.min(getResources().getDisplayMetrics().widthPixels - look.dp(40), look.dp(420)), -2);
        inner.addView(paperWrap, pp);
        v.addView(Kit.scroll(look, inner), Kit.fill());

        LinearLayout foot = Kit.hbox(this);
        foot.setPadding(look.dp(20), look.dp(8), look.dp(20), look.dp(16));
        if (justSold) {
            foot.addView(Kit.secondary(look, "Share receipt", () -> Receipts.share(this, look, store, sale)), Kit.weight(1));
            foot.addView(Kit.gap(look, 10));
            foot.addView(Kit.primary(look, "New sale", this::newSale), Kit.weight(1));
        } else if (!sale.refunded) {
            foot.addView(Kit.danger(look, "Refund", this::refund), Kit.weight(1));
            foot.addView(Kit.gap(look, 10));
            foot.addView(Kit.primary(look, "Share receipt", () -> Receipts.share(this, look, store, sale)), Kit.weight(1));
        } else {
            foot.addView(Kit.primary(look, "Share receipt", () -> Receipts.share(this, look, store, sale)), Kit.weight(1));
        }
        v.addView(foot, Kit.match());
        setScreen(v, look.page);
    }

    private void newSale() {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("sell", true);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (justSold) newSale();
        else super.onBackPressed();
    }

    private void refund() {
        Runnable doIt = () -> Kit.confirm(this, look, "Refund receipt #" + sale.number + "?",
                "The sale of " + store.money(sale.due) + " no longer counts in your reports and any stock goes back on the shelf.",
                "Refund", true, () -> {
                    store.doc.refund(sale, System.currentTimeMillis());
                    store.save();
                    Kit.toast(this, "Refunded");
                    build();
                });
        if (!store.doc.pin.isEmpty()) Pin.ask(this, look, "Owner PIN to refund", doIt);
        else doIt.run();
    }
}
