package com.r4xn.whitelabelpos.core;

import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** The "Try a demo shop" data, made the same way on Android and iPhone (SPEC section 8). */
public final class Demo {
    private long state = 42;

    private Demo() {
    }

    private long next() {
        state = (state * 1103515245L + 12345L) % 2147483648L;
        return state;
    }

    private int rand(int n) {
        return (int) (next() % n);
    }

    /** A complete shop of this kind with 30 days of made-up sales up to now. */
    public static Doc shop(Catalog type, List<Currency> currencies, long now, ZoneId zone) {
        Doc d = new Doc();
        JSONObject demo = type.demo;
        d.brand.name = demo.optString("name", type.name);
        d.brand.tagline = demo.optString("tagline", "");
        d.brand.primary = demo.optString("primary", d.brand.primary);
        d.brand.accent = demo.optString("accent", d.brand.accent);
        d.brand.design = demo.optString("design", "fresh");
        d.business.type = type.id;
        d.business.phone = demo.optString("phone", "");
        d.business.address = demo.optString("address", "");
        d.business.regNo = demo.optString("regNo", "");
        d.business.footer = demo.optString("footer", d.business.footer);
        d.business.currency = "MYR";
        d.business.taxName = "SST";
        d.business.taxRate = 600;
        d.business.taxInclusive = false;
        d.business.cashRoundTo = 5;
        d.useStarterProducts(type, Currency.find(currencies, "MYR"));
        new Demo().sales(d, now, zone);
        d.setupDone = true;
        return d;
    }

    private void sales(Doc d, long now, ZoneId zone) {
        if (d.products.isEmpty()) return;
        LocalDate today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        java.util.List<Doc.Sale> made = new java.util.ArrayList<>();
        for (int back = 29; back >= 0; back--) {
            long dayStart = today.minusDays(back).atStartOfDay(zone).toInstant().toEpochMilli();
            int count = 5 + rand(8);
            for (int i = 0; i < count; i++) {
                int minute = 480 + rand(780);
                long time = dayStart + minute * 60_000L;
                Cart cart = new Cart();
                int lineCount = 1 + rand(3);
                for (int k = 0; k < lineCount; k++) {
                    Doc.Product p = d.products.get(rand(d.products.size()));
                    int qty = 1 + rand(2);
                    Cart.Item it = cart.item(p.id);
                    if (it == null) {
                        it = new Cart.Item();
                        it.productId = p.id;
                        it.name = p.name;
                        it.price = p.price;
                        cart.items.add(it);
                    }
                    it.qty += qty;
                }
                int m = rand(10);
                String method = m <= 3 ? Doc.CASH : m <= 6 ? Doc.CARD : m <= 8 ? Doc.QR : Doc.EWALLET;
                if (rand(12) == 0) {
                    cart.discountType = "percent";
                    cart.discountValue = 1000;
                }
                boolean refunded = rand(40) == 0;
                if (time > now) continue;
                Doc.Sale s = saleFrom(d, cart, method, time);
                if (refunded) {
                    s.refunded = true;
                    s.refundTime = time + 30 * 60_000L;
                }
                made.add(s);
            }
        }
        made.sort((a, b) -> Long.compare(a.time, b.time));
        for (Doc.Sale s : made) {
            s.number = d.nextNumber++;
            s.id = "s-" + s.number;
            d.sales.add(s);
        }
    }

    /** Like Doc.completeSale but without touching stock or numbers. */
    private static Doc.Sale saleFrom(Doc d, Cart cart, String method, long time) {
        Totals t = cart.totals(d.business, method);
        Doc.Sale s = new Doc.Sale();
        s.time = time;
        for (Cart.Item it : cart.items) {
            Doc.Line l = new Doc.Line();
            l.product = it.productId;
            l.name = it.name;
            l.price = it.price;
            l.qty = it.qty;
            s.lines.add(l);
        }
        s.discountType = cart.discountType;
        s.discountValue = cart.discountValue;
        s.subtotal = t.subtotal;
        s.discount = t.discount;
        s.tax = t.tax;
        s.total = t.total;
        s.rounding = t.rounding;
        s.due = t.due;
        s.taxName = d.business.taxName;
        s.taxRate = d.business.taxRate;
        s.taxInclusive = d.business.taxInclusive;
        s.method = method;
        s.paid = Doc.CASH.equals(method) ? Money.nextThousand(t.due) : t.due;
        s.change = Totals.change(method, s.paid, t.due);
        return s;
    }

    /** Products 0 × 2, 4 × 1 and 9 × 1, for screenshots. */
    public static void fillCart(Doc d, Cart cart) {
        cart.clear();
        int[][] picks = {{0, 2}, {4, 1}, {9, 1}};
        for (int[] pick : picks) {
            if (pick[0] >= d.products.size()) continue;
            Doc.Product p = d.products.get(pick[0]);
            for (int i = 0; i < pick[1]; i++) cart.add(p);
        }
    }
}
