package com.r4xn.whitelabelpos.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** One design environment from whitelabel/app/designs.json (SPEC section 5). */
public final class Design {
    public final String id;
    public final String name;
    public final String about;
    public final boolean dark;
    public final int page;
    public final int surface;
    public final int raised;
    public final int ink;
    public final int muted;
    public final int line;
    public final int radius;
    public final String font;
    public final boolean caps;
    public final float tracking;
    public final String header;
    public final String products;
    public final int columns;
    public final String buttons;
    public final String logoShape;
    public final String chips;

    private Design(JSONObject o) throws JSONException {
        id = o.getString("id");
        name = o.optString("name", id);
        about = o.optString("about", "");
        dark = o.optBoolean("dark", false);
        page = Colors.parse(o.optString("page"), 0xFFF3F5F8);
        surface = Colors.parse(o.optString("surface"), 0xFFFFFFFF);
        raised = Colors.parse(o.optString("raised"), 0xFFE9EDF2);
        ink = Colors.parse(o.optString("ink"), 0xFF101828);
        muted = Colors.parse(o.optString("muted"), 0xFF667085);
        line = Colors.parse(o.optString("line"), 0xFFE4E7EC);
        radius = o.optInt("radius", 16);
        font = o.optString("font", "sans");
        caps = o.optBoolean("caps", false);
        tracking = (float) o.optDouble("tracking", 0);
        header = o.optString("header", "plain");
        products = o.optString("products", "tiles");
        columns = Math.max(1, o.optInt("columns", 3));
        buttons = o.optString("buttons", "rounded");
        logoShape = o.optString("logoShape", "circle");
        chips = o.optString("chips", "soft");
    }

    public static List<Design> parse(String json) throws JSONException {
        JSONArray a = new JSONObject(json).getJSONArray("designs");
        List<Design> out = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) out.add(new Design(a.getJSONObject(i)));
        return out;
    }

    /** Unknown ids fall back to the first design ("fresh"). */
    public static Design find(List<Design> all, String id) {
        for (Design d : all) if (d.id.equals(id)) return d;
        return all.get(0);
    }
}
