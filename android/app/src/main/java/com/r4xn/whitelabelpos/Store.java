package com.r4xn.whitelabelpos;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.r4xn.whitelabelpos.core.Cart;
import com.r4xn.whitelabelpos.core.Catalog;
import com.r4xn.whitelabelpos.core.Currency;
import com.r4xn.whitelabelpos.core.Design;
import com.r4xn.whitelabelpos.core.Doc;
import com.r4xn.whitelabelpos.core.Money;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/** Holds the shop document, the sale being rung up and the shared design/product/currency lists. */
public final class Store {
    public final List<Design> designs;
    public final List<Catalog> catalogs;
    public final List<Currency> currencies;
    public final Cart cart = new Cart();
    public Doc doc;
    /** True once the owner PIN was typed since the app started. */
    public boolean unlocked;

    private final Context context;
    private final File file;

    Store(Context c) {
        context = c.getApplicationContext();
        try {
            designs = Design.parse(asset("designs.json"));
            catalogs = Catalog.parse(asset("catalogs.json"));
            currencies = Currency.parse(asset("currencies.json"));
        } catch (Exception e) {
            throw new IllegalStateException("app files are missing", e);
        }
        file = new File(context.getFilesDir(), "shop.json");
        Doc d = null;
        if (file.exists()) {
            try {
                d = Doc.fromJson(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            } catch (Exception e) {
                // Keep the unreadable file aside instead of losing it, then start fresh.
                file.renameTo(new File(context.getFilesDir(), "shop-unreadable-" + System.currentTimeMillis() + ".json"));
            }
        }
        doc = d == null ? new Doc() : d;
    }

    private String asset(String name) throws IOException {
        try (InputStream in = context.getAssets().open(name)) {
            return read(in);
        }
    }

    public static String read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return out.toString("UTF-8");
    }

    /** Writes the shop to a new file first, then swaps it in, so a crash never leaves half a file. */
    public void save() {
        File tmp = new File(file.getParentFile(), "shop.json.tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(doc.toJson().toString().getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        } catch (IOException e) {
            throw new IllegalStateException("could not save", e);
        }
        if (!tmp.renameTo(file)) throw new IllegalStateException("could not save");
    }

    public void replace(Doc d) {
        doc = d;
        cart.clear();
        unlocked = false;
        save();
    }

    public Design design() {
        return Design.find(designs, doc.brand.design);
    }

    public Currency currency() {
        return Currency.find(currencies, doc.business.currency);
    }

    public Catalog catalog() {
        return Catalog.find(catalogs, doc.business.type);
    }

    public String money(long amount) {
        return Money.format(amount, currency());
    }

    public boolean locked() {
        return !doc.pin.isEmpty() && !unlocked;
    }

    public String appName() {
        return context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
    }

    public String version() {
        try {
            PackageInfo p = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return p.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    public boolean debuggable() {
        return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
}
