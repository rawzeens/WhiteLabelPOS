package com.r4xn.whitelabelpos.ui;

import android.content.Intent;

import com.r4xn.whitelabelpos.Store;
import com.r4xn.whitelabelpos.core.Catalog;
import com.r4xn.whitelabelpos.core.Demo;
import com.r4xn.whitelabelpos.core.Design;
import com.r4xn.whitelabelpos.core.Doc;
import com.r4xn.whitelabelpos.core.Money;

import java.time.ZoneId;

/**
 * Switches for screenshots, only in debug builds (SPEC section 8):
 * --es demo grocery  --es design midnight  --es open sell|till|cart|checkout|receipt|orders|products|product|reports|settings|setup0..5
 */
final class Debug {
    private Debug() {
    }

    /** Returns true when it already moved to another screen (the caller should stop). */
    static boolean apply(MainActivity a, Intent i) {
        Store s = a.store;
        if (!s.debuggable() || i == null) return false;
        String demo = i.getStringExtra("demo"), design = i.getStringExtra("design"), open = i.getStringExtra("open");
        if (demo == null && design == null && open == null) return false;
        i.removeExtra("demo");
        i.removeExtra("design");
        i.removeExtra("open");
        if (demo != null) {
            s.replace(Demo.shop(Catalog.find(s.catalogs, demo), s.currencies, System.currentTimeMillis(), ZoneId.systemDefault()));
        }
        if (design != null) {
            s.doc.brand.design = Design.find(s.designs, design).id;
            s.save();
        }
        if (open == null) open = "sell";
        s.unlocked = true;
        a.sellQuery = "";
        a.sellCategory = "";
        if (open.startsWith("setup")) {
            Intent setup = new Intent(a, SetupActivity.class);
            setup.putExtra("start", Integer.parseInt(open.substring(5)));
            a.startActivity(setup);
            a.finish();
            return true;
        }
        switch (open) {
            case "orders":
                a.tab = MainActivity.ORDERS;
                break;
            case "products":
                a.tab = MainActivity.PRODUCTS;
                break;
            case "reports":
                a.tab = MainActivity.REPORTS;
                break;
            case "settings":
                a.tab = MainActivity.SETTINGS;
                break;
            default:
                a.tab = MainActivity.SELL;
        }
        switch (open) {
            case "cart":
                Demo.fillCart(s.doc, s.cart);
                a.startActivity(new Intent(a, CartActivity.class));
                break;
            case "till":
                Demo.fillCart(s.doc, s.cart);
                break;
            case "checkout":
                Demo.fillCart(s.doc, s.cart);
                a.startActivity(new Intent(a, CheckoutActivity.class));
                break;
            case "receipt": {
                Demo.fillCart(s.doc, s.cart);
                long due = s.cart.totals(s.doc.business, Doc.CASH).due;
                Doc.Sale sale = s.doc.completeSale(s.cart, Doc.CASH, Money.nextThousand(due), System.currentTimeMillis());
                s.cart.clear();
                s.save();
                Intent r = new Intent(a, ReceiptActivity.class);
                r.putExtra("sale", sale.id);
                r.putExtra("justSold", true);
                a.startActivity(r);
                break;
            }
            case "product":
                if (!s.doc.products.isEmpty()) {
                    Intent e = new Intent(a, EditProductActivity.class);
                    e.putExtra("id", s.doc.products.get(0).id);
                    a.startActivity(e);
                }
                break;
            default:
                break;
        }
        return false;
    }
}
