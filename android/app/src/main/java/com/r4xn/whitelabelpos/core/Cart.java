package com.r4xn.whitelabelpos.core;

import java.util.ArrayList;
import java.util.List;

/** The sale being rung up right now. */
public final class Cart {
    public static final class Item {
        public String productId;
        public String name;
        public long price;
        public int qty;
    }

    public final List<Item> items = new ArrayList<>();
    public String discountType = "none";
    public long discountValue;
    public String note = "";

    public Item item(String productId) {
        for (Item it : items) if (it.productId.equals(productId)) return it;
        return null;
    }

    public int qty(String productId) {
        Item it = item(productId);
        return it == null ? 0 : it.qty;
    }

    /** Adds one. Returns false when tracked stock would run out. */
    public boolean add(Doc.Product p) {
        int have = qty(p.id);
        if (p.trackStock && have + 1 > p.stock) return false;
        Item it = item(p.id);
        if (it == null) {
            it = new Item();
            it.productId = p.id;
            it.name = p.name;
            it.price = p.price;
            it.qty = 0;
            items.add(it);
        }
        it.qty++;
        return true;
    }

    /** Changes a line by +1 / -1; at zero the line goes. Returns false when stock stops it. */
    public boolean change(Doc doc, String productId, int delta) {
        Item it = item(productId);
        if (it == null) return false;
        if (delta > 0) {
            Doc.Product p = doc.product(productId);
            if (p != null && p.trackStock && it.qty + delta > p.stock) return false;
        }
        it.qty += delta;
        if (it.qty <= 0) items.remove(it);
        return true;
    }

    public int count() {
        int n = 0;
        for (Item it : items) n += it.qty;
        return n;
    }

    public long subtotal() {
        long s = 0;
        for (Item it : items) s += it.price * it.qty;
        return s;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public Totals totals(Doc.Business b, String method) {
        return Totals.compute(subtotal(), discountType, discountValue, b.taxRate, b.taxInclusive, b.cashRoundTo, method);
    }

    public void clear() {
        items.clear();
        discountType = "none";
        discountValue = 0;
        note = "";
    }
}
