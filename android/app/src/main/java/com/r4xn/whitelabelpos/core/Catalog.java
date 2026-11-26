package com.r4xn.whitelabelpos.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** A kind of business from whitelabel/app/catalogs.json: its starter products and demo shop. */
public final class Catalog {
    public final String id;
    public final String name;
    public final String icon;
    public final JSONObject demo;
    public final List<String> categories = new ArrayList<>();
    public final List<Seed> products = new ArrayList<>();

    public static final class Seed {
        public final String name;
        public final long price;
        public final int category;
        public final String icon;
        public final String color;
        public final String sku;
        public final int stock; // -1 = stock not tracked

        Seed(JSONObject o) throws JSONException {
            name = o.getString("name");
            price = o.getLong("price");
            category = o.optInt("category", -1);
            icon = o.optString("icon", "");
            color = o.optString("color", "#64748B");
            sku = o.optString("sku", "");
            stock = o.has("stock") ? o.getInt("stock") : -1;
        }
    }

    private Catalog(JSONObject o) throws JSONException {
        id = o.getString("id");
        name = o.optString("name", id);
        icon = o.optString("icon", "");
        demo = o.optJSONObject("demo") == null ? new JSONObject() : o.getJSONObject("demo");
        JSONArray c = o.optJSONArray("categories");
        if (c != null) for (int i = 0; i < c.length(); i++) categories.add(c.getString(i));
        JSONArray p = o.optJSONArray("products");
        if (p != null) for (int i = 0; i < p.length(); i++) products.add(new Seed(p.getJSONObject(i)));
    }

    public static List<Catalog> parse(String json) throws JSONException {
        JSONArray a = new JSONObject(json).getJSONArray("types");
        List<Catalog> out = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) out.add(new Catalog(a.getJSONObject(i)));
        return out;
    }

    public static Catalog find(List<Catalog> all, String id) {
        for (Catalog c : all) if (c.id.equals(id)) return c;
        return all.get(0);
    }
}
