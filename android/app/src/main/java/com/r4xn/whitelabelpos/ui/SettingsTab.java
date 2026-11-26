package com.r4xn.whitelabelpos.ui;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.r4xn.whitelabelpos.R;
import com.r4xn.whitelabelpos.Store;
import com.r4xn.whitelabelpos.core.Catalog;
import com.r4xn.whitelabelpos.core.Currency;
import com.r4xn.whitelabelpos.core.Doc;
import com.r4xn.whitelabelpos.core.Money;
import com.r4xn.whitelabelpos.core.Text;

import java.util.ArrayList;
import java.util.List;

/** The shop's branding, design, money settings, PIN and backups. */
final class SettingsTab {
    private SettingsTab() {
    }

    static View build(MainActivity a) {
        Look l = a.look;
        Store s = a.store;
        Doc d = s.doc;
        LinearLayout body = Kit.vbox(a);
        body.setPadding(l.dp(20), l.dp(16), l.dp(20), l.dp(28));
        body.addView(Kit.title(l, "Settings"));

        LinearLayout brand = Kit.vbox(a);
        brand.setBackground(Kit.outline(l.page, l.line, Math.max(1, l.dp(1)), l.radius()));
        brand.setClipToOutline(true);
        brand.addView(Styles.header(l, d.brand), Kit.match());
        LinearLayout under = Kit.hbox(a);
        under.setPadding(l.dp(16), l.dp(4), l.dp(8), l.dp(8));
        under.addView(Kit.muted(l, s.design().name + " design"), Kit.weight(1));
        under.addView(Kit.link(l, "Change look", () -> edit(a, 3)));
        brand.addView(under, Kit.match());
        LinearLayout.LayoutParams bp = Kit.match();
        bp.topMargin = l.dp(14);
        body.addView(brand, bp);

        Catalog type = s.catalog();
        Currency cur = s.currency();
        Doc.Business b = d.business;
        String money = cur.code + " · " + (b.taxRate > 0 ? Text.taxLine(b.taxName, b.taxRate, b.taxInclusive) : "no tax")
                + (b.cashRoundTo > 0 ? " · cash to " + Money.format(b.cashRoundTo, cur) : "");

        body.addView(Kit.label(l, "Your shop"));
        LinearLayout shop = Kit.card(l);
        shop.setPadding(l.dp(14), l.dp(2), l.dp(10), l.dp(2));
        shop.addView(row(l, "Business details", d.brand.name + " · " + type.name, () -> edit(a, 1)));
        shop.addView(Kit.divider(l));
        shop.addView(row(l, "Logo & colours", d.brand.logo.isEmpty() ? "Initials logo · " + d.brand.primary : "Your logo · " + d.brand.primary, () -> edit(a, 2)));
        shop.addView(Kit.divider(l));
        shop.addView(row(l, "Design", s.design().name + " · " + s.design().about, () -> edit(a, 3)));
        shop.addView(Kit.divider(l));
        shop.addView(row(l, "Money & receipt", money, () -> edit(a, 4)));
        body.addView(shop, Kit.match());

        body.addView(Kit.label(l, "Security"));
        LinearLayout sec = Kit.card(l);
        sec.setPadding(l.dp(14), l.dp(2), l.dp(10), l.dp(2));
        sec.addView(row(l, "Owner PIN", d.pin.isEmpty() ? "Off · anyone can change settings" : "On · asked for products, settings and refunds", () ->
                Pin.set(a, l, pin -> {
                    d.pin = pin;
                    s.unlocked = true;
                    s.save();
                    Kit.toast(a, pin.isEmpty() ? "PIN removed" : "PIN saved");
                    a.open(MainActivity.SETTINGS);
                })));
        body.addView(sec, Kit.match());

        body.addView(Kit.label(l, "Your data"));
        LinearLayout data = Kit.card(l);
        data.setPadding(l.dp(14), l.dp(2), l.dp(10), l.dp(2));
        data.addView(row(l, "Back up", d.products.size() + " products · " + d.sales.size() + " sales. Save or send a copy.", () -> Backups.backup(a, l)));
        data.addView(Kit.divider(l));
        data.addView(row(l, "Restore a backup", "Replace what's on this device with a backup", () -> Backups.restore(a.launchers)));
        data.addView(Kit.divider(l));
        data.addView(row(l, "Try a demo shop", "See the till filled with a made-up shop", () -> demo(a, l, s)));
        body.addView(data, Kit.match());

        LinearLayout.LayoutParams sp = Kit.match();
        sp.topMargin = l.dp(24);
        body.addView(Kit.danger(l, "Start over", () -> Kit.confirm(a, l, "Erase everything?",
                "Products, sales and branding on this device are deleted and setup starts again. Make a backup first if you may need them.",
                "Erase", true, () -> {
                    s.replace(new Doc());
                    a.startActivity(new Intent(a, SetupActivity.class));
                    a.finish();
                })), sp);

        TextView about = Kit.muted(l, s.appName() + " · version " + s.version());
        about.setGravity(android.view.Gravity.CENTER);
        about.setPadding(0, l.dp(20), 0, 0);
        body.addView(about, Kit.match());
        return Kit.scroll(l, body);
    }

    private static void edit(MainActivity a, int step) {
        Intent i = new Intent(a, SetupActivity.class);
        i.putExtra("only", step);
        a.startActivity(i);
    }

    static View row(Look l, String title, String sub, Runnable onTap) {
        LinearLayout row = Kit.hbox(l.c);
        row.setPadding(0, l.dp(12), 0, l.dp(12));
        LinearLayout texts = Kit.vbox(l.c);
        texts.addView(Kit.text(l, title, 16, l.ink, l.medium));
        TextView s = Kit.muted(l, sub);
        s.setMaxLines(2);
        texts.addView(s);
        row.addView(texts, Kit.weight(1));
        row.addView(Kit.icon(l, R.drawable.ic_chevron, l.muted, 18));
        row.setOnClickListener(v -> onTap.run());
        return row;
    }

    private static void demo(MainActivity a, Look l, Store s) {
        List<String> names = new ArrayList<>();
        for (Catalog c : s.catalogs) names.add(c.icon + "  " + c.demo.optString("name") + " · " + c.name);
        Kit.choose(a, l, "Pick a demo shop", names, i -> Kit.confirm(a, l, "Replace your shop with the demo?",
                "Everything on this device is swapped for the demo shop. Make a backup first if you need your data.",
                "Use demo", true, () -> {
                    Catalog c = s.catalogs.get(i);
                    s.replace(com.r4xn.whitelabelpos.core.Demo.shop(c, s.currencies, System.currentTimeMillis(), java.time.ZoneId.systemDefault()));
                    s.unlocked = true;
                    a.tab = MainActivity.SELL;
                    a.build();
                }));
    }
}
