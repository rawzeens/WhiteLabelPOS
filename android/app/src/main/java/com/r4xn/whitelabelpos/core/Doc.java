package com.r4xn.whitelabelpos.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The whole shop as one document. The saved file and the backup file are both this, written by
 * {@link #toJson()}; the iPhone app reads and writes the same thing (whitelabel/SPEC.md section 2).
 */
public final class Doc {
    public static final String CASH = "cash";
    public static final String CARD = "card";
    public static final String QR = "qr";
    public static final String EWALLET = "ewallet";
    public static final String[] METHODS = {CASH, CARD, QR, EWALLET};

    public boolean setupDone;
    public final Brand brand = new Brand();
    public final Business business = new Business();
    public String pin = "";
    public final List<Category> categories = new ArrayList<>();
    public final List<Product> products = new ArrayList<>();
    public final List<Sale> sales = new ArrayList<>();
    public int nextNumber = 1001;
    public int nextId = 1;

    // ------------------------------------------------------------------ parts

    public static final class Brand {
        public String name = "";
        public String tagline = "";
        public String logo = "";
        public String primary = "#2563EB";
        public String accent = "#F59E0B";
        public String design = "fresh";
    }

    public static final class Business {
        public String type = "cafe";
        public String phone = "";
        public String address = "";
        public String regNo = "";
        public String currency = "MYR";
        public String taxName = "SST";
        public int taxRate = 0;
        public boolean taxInclusive = false;
        public int cashRoundTo = 5;
        public String footer = "Thank you! Please come again.";
    }

    public static final class Category {
        public String id = "";
        public String name = "";
    }

    public static final class Product {
        public String id = "";
        public String name = "";
        public long price;
        public String category = "";
        public String icon = "";
        public String color = "#64748B";
        public String sku = "";
        public boolean trackStock;
        public int stock;

        public boolean soldOut() {
            return trackStock && stock <= 0;
        }
    }

    public static final class Line {
        public String product = "";
        public String name = "";
        public long price;
        public int qty;

        public long amount() {
            return price * qty;
        }
    }

    public static final class Sale {
        public String id = "";
        public int number;
        public long time;
        public final List<Line> lines = new ArrayList<>();
        public String discountType = "none";
        public long discountValue;
        public long subtotal, discount, tax, total, rounding, due;
        public String taxName = "";
        public int taxRate;
        public boolean taxInclusive;
        public String method = CASH;
        public long paid, change;
        public String note = "";
        public boolean refunded;
        public long refundTime;

        public int itemCount() {
            int n = 0;
            for (Line l : lines) n += l.qty;
            return n;
        }
    }

    // ------------------------------------------------------------------ lookups

    public String newId(String prefix) {
        return prefix + "-" + (nextId++);
    }

    public Product product(String id) {
        for (Product p : products) if (p.id.equals(id)) return p;
        return null;
    }

    public Category category(String id) {
        for (Category c : categories) if (c.id.equals(id)) return c;
        return null;
    }

    public Sale sale(String id) {
        for (Sale s : sales) if (s.id.equals(id)) return s;
        return null;
    }

    public String categoryName(String id) {
        Category c = category(id);
        return c == null ? "" : c.name;
    }

    // ------------------------------------------------------------------ selling

    /** Saves the cart as a sale, lowers stock and moves the receipt number on. */
    public Sale completeSale(Cart cart, String method, long paid, long now) {
        Totals t = cart.totals(business, method);
        Sale s = new Sale();
        s.number = nextNumber++;
        s.id = "s-" + s.number;
        s.time = now;
        for (Cart.Item it : cart.items) {
            Line l = new Line();
            l.product = it.productId;
            l.name = it.name;
            l.price = it.price;
            l.qty = it.qty;
            s.lines.add(l);
            Product p = product(it.productId);
            if (p != null && p.trackStock) p.stock -= it.qty;
        }
        s.discountType = cart.discountType;
        s.discountValue = cart.discountValue;
        s.subtotal = t.subtotal;
        s.discount = t.discount;
        s.tax = t.tax;
        s.total = t.total;
        s.rounding = t.rounding;
        s.due = t.due;
        s.taxName = business.taxName;
        s.taxRate = business.taxRate;
        s.taxInclusive = business.taxInclusive;
        s.method = method;
        s.paid = CASH.equals(method) ? paid : t.due;
        s.change = Totals.change(method, s.paid, t.due);
        s.note = cart.note == null ? "" : cart.note;
        sales.add(s);
        return s;
    }

    /** Marks a sale refunded and puts tracked stock back. */
    public void refund(Sale s, long now) {
        if (s.refunded) return;
        s.refunded = true;
        s.refundTime = now;
        for (Line l : s.lines) {
            Product p = product(l.product);
            if (p != null && p.trackStock) p.stock += l.qty;
        }
    }

    public void deleteCategory(String id) {
        categories.removeIf(c -> c.id.equals(id));
        for (Product p : products) if (p.category.equals(id)) p.category = "";
    }

    /** Replaces the product list with a kind of business's starter products. */
    public void useStarterProducts(Catalog type, Currency currency) {
        categories.clear();
        products.clear();
        List<String> catIds = new ArrayList<>();
        for (String name : type.categories) {
            Category c = new Category();
            c.id = newId("c");
            c.name = name;
            categories.add(c);
            catIds.add(c.id);
        }
        long scale = currency.decimals == 0 ? 100 : 1;
        for (Catalog.Seed seed : type.products) {
            Product p = new Product();
            p.id = newId("p");
            p.name = seed.name;
            p.price = seed.price * scale;
            p.category = seed.category >= 0 && seed.category < catIds.size() ? catIds.get(seed.category) : "";
            p.icon = seed.icon;
            p.color = seed.color;
            p.sku = seed.sku;
            p.trackStock = seed.stock >= 0;
            p.stock = Math.max(0, seed.stock);
            products.add(p);
        }
    }

    // ------------------------------------------------------------------ JSON

    public JSONObject toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("format", "whitelabel-pos");
            o.put("version", 1);
            o.put("setupDone", setupDone);
            JSONObject b = new JSONObject();
            b.put("name", brand.name);
            b.put("tagline", brand.tagline);
            b.put("logo", brand.logo);
            b.put("primary", brand.primary);
            b.put("accent", brand.accent);
            b.put("design", brand.design);
            o.put("brand", b);
            JSONObject bz = new JSONObject();
            bz.put("type", business.type);
            bz.put("phone", business.phone);
            bz.put("address", business.address);
            bz.put("regNo", business.regNo);
            bz.put("currency", business.currency);
            bz.put("taxName", business.taxName);
            bz.put("taxRate", business.taxRate);
            bz.put("taxInclusive", business.taxInclusive);
            bz.put("cashRoundTo", business.cashRoundTo);
            bz.put("footer", business.footer);
            o.put("business", bz);
            o.put("pin", pin);
            JSONArray cs = new JSONArray();
            for (Category c : categories) {
                JSONObject j = new JSONObject();
                j.put("id", c.id);
                j.put("name", c.name);
                cs.put(j);
            }
            o.put("categories", cs);
            JSONArray ps = new JSONArray();
            for (Product p : products) {
                JSONObject j = new JSONObject();
                j.put("id", p.id);
                j.put("name", p.name);
                j.put("price", p.price);
                j.put("category", p.category);
                j.put("icon", p.icon);
                j.put("color", p.color);
                j.put("sku", p.sku);
                j.put("trackStock", p.trackStock);
                j.put("stock", p.stock);
                ps.put(j);
            }
            o.put("products", ps);
            JSONArray ss = new JSONArray();
            for (Sale s : sales) {
                JSONObject j = new JSONObject();
                j.put("id", s.id);
                j.put("number", s.number);
                j.put("time", s.time);
                JSONArray ls = new JSONArray();
                for (Line l : s.lines) {
                    JSONObject lj = new JSONObject();
                    lj.put("product", l.product);
                    lj.put("name", l.name);
                    lj.put("price", l.price);
                    lj.put("qty", l.qty);
                    ls.put(lj);
                }
                j.put("lines", ls);
                j.put("discountType", s.discountType);
                j.put("discountValue", s.discountValue);
                j.put("subtotal", s.subtotal);
                j.put("discount", s.discount);
                j.put("tax", s.tax);
                j.put("total", s.total);
                j.put("rounding", s.rounding);
                j.put("due", s.due);
                j.put("taxName", s.taxName);
                j.put("taxRate", s.taxRate);
                j.put("taxInclusive", s.taxInclusive);
                j.put("method", s.method);
                j.put("paid", s.paid);
                j.put("change", s.change);
                j.put("note", s.note);
                j.put("refunded", s.refunded);
                j.put("refundTime", s.refundTime);
                ss.put(j);
            }
            o.put("sales", ss);
            o.put("nextNumber", nextNumber);
            o.put("nextId", nextId);
            return o;
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Reads a saved file or a backup. Missing parts get their defaults. */
    public static Doc fromJson(String text) throws JSONException {
        JSONObject o = new JSONObject(text);
        if (o.has("format") && !"whitelabel-pos".equals(o.optString("format"))) {
            throw new JSONException("not a shop file");
        }
        Doc d = new Doc();
        d.setupDone = o.optBoolean("setupDone", false);
        JSONObject b = o.optJSONObject("brand");
        if (b != null) {
            d.brand.name = b.optString("name", d.brand.name);
            d.brand.tagline = b.optString("tagline", d.brand.tagline);
            d.brand.logo = b.optString("logo", d.brand.logo);
            d.brand.primary = b.optString("primary", d.brand.primary);
            d.brand.accent = b.optString("accent", d.brand.accent);
            d.brand.design = b.optString("design", d.brand.design);
        }
        JSONObject bz = o.optJSONObject("business");
        if (bz != null) {
            d.business.type = bz.optString("type", d.business.type);
            d.business.phone = bz.optString("phone", d.business.phone);
            d.business.address = bz.optString("address", d.business.address);
            d.business.regNo = bz.optString("regNo", d.business.regNo);
            d.business.currency = bz.optString("currency", d.business.currency);
            d.business.taxName = bz.optString("taxName", d.business.taxName);
            d.business.taxRate = bz.optInt("taxRate", d.business.taxRate);
            d.business.taxInclusive = bz.optBoolean("taxInclusive", d.business.taxInclusive);
            d.business.cashRoundTo = bz.optInt("cashRoundTo", d.business.cashRoundTo);
            d.business.footer = bz.optString("footer", d.business.footer);
        }
        d.pin = o.optString("pin", "");
        JSONArray cs = o.optJSONArray("categories");
        if (cs != null) for (int i = 0; i < cs.length(); i++) {
            JSONObject j = cs.getJSONObject(i);
            Category c = new Category();
            c.id = j.optString("id");
            c.name = j.optString("name");
            d.categories.add(c);
        }
        JSONArray ps = o.optJSONArray("products");
        if (ps != null) for (int i = 0; i < ps.length(); i++) {
            JSONObject j = ps.getJSONObject(i);
            Product p = new Product();
            p.id = j.optString("id");
            p.name = j.optString("name");
            p.price = j.optLong("price", 0);
            p.category = j.optString("category", "");
            p.icon = j.optString("icon", "");
            p.color = j.optString("color", p.color);
            p.sku = j.optString("sku", "");
            p.trackStock = j.optBoolean("trackStock", false);
            p.stock = j.optInt("stock", 0);
            d.products.add(p);
        }
        JSONArray ss = o.optJSONArray("sales");
        if (ss != null) for (int i = 0; i < ss.length(); i++) {
            JSONObject j = ss.getJSONObject(i);
            Sale s = new Sale();
            s.id = j.optString("id");
            s.number = j.optInt("number");
            s.time = j.optLong("time");
            JSONArray ls = j.optJSONArray("lines");
            if (ls != null) for (int k = 0; k < ls.length(); k++) {
                JSONObject lj = ls.getJSONObject(k);
                Line l = new Line();
                l.product = lj.optString("product", "");
                l.name = lj.optString("name", "");
                l.price = lj.optLong("price", 0);
                l.qty = lj.optInt("qty", 1);
                s.lines.add(l);
            }
            s.discountType = j.optString("discountType", "none");
            s.discountValue = j.optLong("discountValue", 0);
            s.subtotal = j.optLong("subtotal");
            s.discount = j.optLong("discount");
            s.tax = j.optLong("tax");
            s.total = j.optLong("total");
            s.rounding = j.optLong("rounding");
            s.due = j.optLong("due", s.total + s.rounding);
            s.taxName = j.optString("taxName", "");
            s.taxRate = j.optInt("taxRate", 0);
            s.taxInclusive = j.optBoolean("taxInclusive", false);
            s.method = j.optString("method", CASH);
            s.paid = j.optLong("paid", s.due);
            s.change = j.optLong("change", 0);
            s.note = j.optString("note", "");
            s.refunded = j.optBoolean("refunded", false);
            s.refundTime = j.optLong("refundTime", 0);
            d.sales.add(s);
        }
        d.nextNumber = o.optInt("nextNumber", 1001);
        d.nextId = o.optInt("nextId", 1);
        // Never hand out a number or id that is already used.
        for (Sale s : d.sales) d.nextNumber = Math.max(d.nextNumber, s.number + 1);
        for (Category c : d.categories) d.nextId = Math.max(d.nextId, idNumber(c.id) + 1);
        for (Product p : d.products) d.nextId = Math.max(d.nextId, idNumber(p.id) + 1);
        return d;
    }

    private static int idNumber(String id) {
        int dash = id.lastIndexOf('-');
        try {
            return dash < 0 ? 0 : Integer.parseInt(id.substring(dash + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public Doc copy() {
        try {
            return fromJson(toJson().toString());
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }
}
