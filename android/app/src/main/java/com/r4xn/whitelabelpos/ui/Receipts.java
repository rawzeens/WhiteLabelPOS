package com.r4xn.whitelabelpos.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import com.r4xn.whitelabelpos.Store;
import com.r4xn.whitelabelpos.core.Currency;
import com.r4xn.whitelabelpos.core.Doc;
import com.r4xn.whitelabelpos.core.Money;
import com.r4xn.whitelabelpos.core.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.util.Date;

/** The receipt: always dark text on white like paper, in the design's font, with the shop's logo. */
final class Receipts {
    private Receipts() {
    }

    static View build(Look l, Doc doc, Currency cur, Doc.Sale sale) {
        int ink = 0xFF111111, muted = 0xFF6B6B6B, line = 0xFFD9D9D9;
        LinearLayout v = Kit.vbox(l.c);
        v.setBackgroundColor(Color.WHITE);
        v.setPadding(l.dp(22), l.dp(24), l.dp(22), l.dp(24));
        v.setGravity(Gravity.CENTER_HORIZONTAL);

        v.addView(Kit.logo(l, doc.brand, 56, false));
        TextView name = Kit.text(l, doc.brand.name, 20, ink, l.bold);
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, l.dp(10), 0, 0);
        v.addView(name);
        if (!doc.brand.tagline.isEmpty()) v.addView(center(l, doc.brand.tagline, 13, muted));
        if (!doc.business.address.isEmpty()) v.addView(center(l, doc.business.address, 12, muted));
        if (!doc.business.phone.isEmpty()) v.addView(center(l, "Tel " + doc.business.phone, 12, muted));
        if (!doc.business.regNo.isEmpty()) v.addView(center(l, doc.business.regNo, 12, muted));

        v.addView(dashes(l, line));
        LinearLayout info = Kit.hbox(l.c);
        info.addView(Kit.text(l, "Receipt #" + (sale == null ? 1001 : sale.number), 14, ink, l.bold), Kit.weight(1));
        long time = sale == null ? System.currentTimeMillis() : sale.time;
        info.addView(Kit.text(l, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(time)), 12, muted, l.regular));
        v.addView(info, Kit.match());
        if (sale != null && sale.refunded) {
            TextView r = Kit.text(l, "REFUNDED " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(sale.refundTime)), 13, 0xFFDC2626, l.bold);
            r.setPadding(0, l.dp(6), 0, 0);
            v.addView(r, Kit.match());
        }
        v.addView(dashes(l, line));

        if (sale != null) {
            for (Doc.Line ln : sale.lines) {
                LinearLayout r = Kit.hbox(l.c);
                r.setGravity(Gravity.TOP);
                r.setPadding(0, l.dp(3), 0, l.dp(3));
                r.addView(Kit.text(l, ln.qty + " × " + ln.name, 14, ink, l.regular), Kit.weight(1));
                r.addView(Kit.text(l, Money.format(ln.amount(), cur), 14, ink, l.regular));
                v.addView(r, Kit.match());
                if (ln.qty > 1) {
                    TextView each = Kit.text(l, "    " + Money.format(ln.price, cur) + " each", 11, muted, l.regular);
                    v.addView(each, Kit.match());
                }
            }
            v.addView(dashes(l, line));
            v.addView(row(l, "Subtotal", Money.format(sale.subtotal, cur), ink, false));
            if (sale.discount > 0) {
                String what = "percent".equals(sale.discountType) ? "Discount " + Text.percent((int) sale.discountValue) : "Discount";
                v.addView(row(l, what, "-" + Money.format(sale.discount, cur), ink, false));
            }
            if (sale.taxRate > 0) v.addView(row(l, Text.taxLine(sale.taxName, sale.taxRate, sale.taxInclusive), Money.format(sale.tax, cur), ink, false));
            if (sale.rounding != 0) v.addView(row(l, "Rounding", (sale.rounding > 0 ? "+" : "") + Money.format(sale.rounding, cur), ink, false));
            v.addView(row(l, "Total", Money.format(sale.due, cur), ink, true));
            v.addView(dashes(l, line));
            v.addView(row(l, "Paid by", Text.methodName(sale.method), ink, false));
            if (Doc.CASH.equals(sale.method)) {
                v.addView(row(l, "Cash", Money.format(sale.paid, cur), ink, false));
                v.addView(row(l, "Change", Money.format(sale.change, cur), ink, false));
            }
            if (!sale.note.isEmpty()) {
                TextView n = Kit.text(l, "Note: " + sale.note, 13, muted, l.regular);
                n.setPadding(0, l.dp(8), 0, 0);
                v.addView(n, Kit.match());
            }
        }
        if (!doc.business.footer.isEmpty()) {
            TextView f = center(l, doc.business.footer, 14, ink);
            f.setPadding(0, l.dp(18), 0, 0);
            v.addView(f, Kit.match());
        }
        return v;
    }

    private static TextView center(Look l, String s, float sp, int color) {
        TextView t = Kit.text(l, s, sp, color, l.regular);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private static View row(Look l, String a, String b, int ink, boolean big) {
        LinearLayout r = Kit.hbox(l.c);
        r.setPadding(0, l.dp(big ? 6 : 2), 0, l.dp(2));
        r.addView(Kit.text(l, a, big ? 17 : 14, ink, big ? l.bold : l.regular), Kit.weight(1));
        r.addView(Kit.text(l, b, big ? 19 : 14, ink, big ? l.bold : l.regular));
        LinearLayout.LayoutParams lp = Kit.match();
        r.setLayoutParams(lp);
        return r;
    }

    private static View dashes(Look l, int color) {
        View v = new Styles.Leader(l.c, color, l.dp(5));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, l.dp(10));
        lp.setMargins(0, l.dp(10), 0, l.dp(8));
        v.setLayoutParams(lp);
        return v;
    }

    /** Draws the receipt as a picture and opens the share sheet. */
    static void share(Activity a, Look l, Store s, Doc.Sale sale) {
        View v = build(l, s.doc, s.currency(), sale);
        int width = l.dp(380);
        v.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
        Bitmap bmp = Bitmap.createBitmap(v.getMeasuredWidth(), v.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(Color.WHITE);
        v.draw(c);
        try {
            File dir = new File(a.getCacheDir(), "shared");
            dir.mkdirs();
            File f = new File(dir, "receipt-" + sale.number + ".png");
            try (FileOutputStream out = new FileOutputStream(f)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            Uri uri = FileProvider.getUriForFile(a, a.getPackageName() + ".files", f);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("image/png");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.putExtra(Intent.EXTRA_TEXT, s.doc.brand.name + " receipt #" + sale.number + " · " + s.money(sale.due));
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            a.startActivity(Intent.createChooser(send, "Share receipt"));
        } catch (Exception e) {
            Kit.toast(a, "Couldn't share the receipt");
        }
    }
}
