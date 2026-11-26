package com.r4xn.whitelabelpos.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** One entry of whitelabel/app/currencies.json. */
public final class Currency {
    public final String code;
    public final String name;
    public final String symbol;
    public final int decimals;
    public final int cashRoundTo;
    public final String taxName;

    public Currency(String code, String name, String symbol, int decimals, int cashRoundTo, String taxName) {
        this.code = code;
        this.name = name;
        this.symbol = symbol;
        this.decimals = decimals;
        this.cashRoundTo = cashRoundTo;
        this.taxName = taxName;
    }

    public static final Currency MYR = new Currency("MYR", "Malaysian ringgit", "RM", 2, 5, "SST");

    public static List<Currency> parse(String json) throws JSONException {
        JSONArray a = new JSONObject(json).getJSONArray("currencies");
        List<Currency> out = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            out.add(new Currency(o.getString("code"), o.optString("name", o.getString("code")),
                    o.optString("symbol", o.getString("code")), o.optInt("decimals", 2),
                    o.optInt("cashRoundTo", 0), o.optString("taxName", "Tax")));
        }
        return out;
    }

    public static Currency find(List<Currency> all, String code) {
        for (Currency c : all) if (c.code.equals(code)) return c;
        return all.isEmpty() ? MYR : all.get(0);
    }
}
