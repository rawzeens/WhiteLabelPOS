package com.r4xn.whitelabelpos.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

/** Runs the shared cases in whitelabel/fixtures (the iPhone app runs the same ones). */
public class CoreTest {
    private static File shared() {
        File dir = new File("").getAbsoluteFile();
        while (dir != null && !new File(dir, "whitelabel/SPEC.md").exists()) dir = dir.getParentFile();
        assertNotNull("whitelabel folder not found", dir);
        return new File(dir, "whitelabel");
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(new File(shared(), path).toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void totalsMatchTheSharedCases() throws Exception {
        JSONArray cases = new JSONObject(read("fixtures/money-cases.json")).getJSONArray("totals");
        assertTrue(cases.length() >= 10);
        for (int i = 0; i < cases.length(); i++) {
            JSONObject c = cases.getJSONObject(i);
            String name = c.getString("name");
            long sub = 0;
            JSONArray lines = c.getJSONArray("lines");
            for (int k = 0; k < lines.length(); k++) {
                sub += lines.getJSONObject(k).getLong("price") * lines.getJSONObject(k).getLong("qty");
            }
            Totals t = Totals.compute(sub, c.getString("discountType"), c.getLong("discountValue"),
                    c.getInt("taxRate"), c.getBoolean("taxInclusive"), c.getInt("cashRoundTo"), c.getString("method"));
            JSONObject e = c.getJSONObject("expect");
            assertEquals(name + " subtotal", e.getLong("subtotal"), t.subtotal);
            assertEquals(name + " discount", e.getLong("discount"), t.discount);
            assertEquals(name + " tax", e.getLong("tax"), t.tax);
            assertEquals(name + " total", e.getLong("total"), t.total);
            assertEquals(name + " rounding", e.getLong("rounding"), t.rounding);
            assertEquals(name + " due", e.getLong("due"), t.due);
            assertEquals(name + " change", e.getLong("change"), Totals.change(c.getString("method"), c.getLong("paid"), t.due));
        }
    }

    @Test
    public void moneyIsShownAndTypedTheSameWay() throws Exception {
        JSONObject all = new JSONObject(read("fixtures/money-cases.json"));
        JSONArray fmt = all.getJSONArray("format");
        for (int i = 0; i < fmt.length(); i++) {
            JSONObject c = fmt.getJSONObject(i);
            assertEquals(c.getString("expect"), Money.format(c.getLong("amount"), c.getString("symbol"), c.getInt("decimals")));
        }
        JSONArray parse = all.getJSONArray("parse");
        for (int i = 0; i < parse.length(); i++) {
            JSONObject c = parse.getJSONObject(i);
            assertEquals("typing '" + c.getString("text") + "'", c.getLong("expect"), Money.parse(c.getString("text"), c.getInt("decimals")));
        }
    }

    @Test
    public void quickCashButtons() {
        assertEquals(Arrays.asList(2700L, 3000L, 4000L, 5000L), Money.quickCash(2620, 2));
        assertEquals(Arrays.asList(1000L, 2000L, 5000L, 10000L), Money.quickCash(500, 2));
        assertEquals(Arrays.asList(76000L, 80000L, 100000L), Money.quickCash(75480, 0));
        assertTrue(Money.quickCash(0, 2).isEmpty());
    }

    @Test
    public void backupFromEitherAppOpens() throws Exception {
        Doc d = Doc.fromJson(read("fixtures/backup-sample.json"));
        JSONObject checks = new JSONObject(read("fixtures/backup-checks.json"));
        assertEquals(checks.getString("brandName"), d.brand.name);
        assertEquals(checks.getString("design"), d.brand.design);
        assertEquals(checks.getInt("products"), d.products.size());
        assertEquals(checks.getInt("categories"), d.categories.size());
        assertEquals(checks.getInt("salesCount"), d.sales.size());
        assertEquals(checks.getInt("nextNumber"), d.nextNumber);

        // Every stored sale agrees with the money rules.
        for (Doc.Sale s : d.sales) {
            long sub = 0;
            for (Doc.Line l : s.lines) sub += l.amount();
            Totals t = Totals.compute(sub, s.discountType, s.discountValue, s.taxRate, s.taxInclusive, d.business.cashRoundTo, s.method);
            assertEquals(s.id, s.due, t.due);
            assertEquals(s.id, s.tax, t.tax);
        }

        JSONObject r = checks.getJSONObject("report");
        JSONObject e = r.getJSONObject("expect");
        Reports.Report rep = Reports.report(d.sales, r.getLong("from"), r.getLong("to"));
        assertEquals(e.getInt("sales"), rep.sales);
        assertEquals(e.getLong("gross"), rep.gross);
        assertEquals(e.getLong("average"), rep.average);
        assertEquals(e.getLong("tax"), rep.tax);
        assertEquals(e.getLong("discount"), rep.discount);
        assertEquals(e.getInt("refunds"), rep.refunds);
        assertEquals(e.getLong("refunded"), rep.refunded);
        JSONObject by = e.getJSONObject("byMethod");
        assertEquals(by.length(), rep.byMethod.size());
        for (String m : rep.byMethod.keySet()) assertEquals(m, by.getLong(m), (long) rep.byMethod.get(m));
        JSONArray top = e.getJSONArray("top");
        assertEquals(top.length(), rep.top.size());
        for (int i = 0; i < top.length(); i++) {
            assertEquals(top.getJSONObject(i).getString("name"), rep.top.get(i).name);
            assertEquals(top.getJSONObject(i).getInt("qty"), rep.top.get(i).qty);
            assertEquals(top.getJSONObject(i).getLong("amount"), rep.top.get(i).amount);
        }

        // Writing it back and reading again gives the same document.
        String once = d.toJson().toString();
        assertEquals(once, Doc.fromJson(once).toJson().toString());
    }

    @Test
    public void sellingLowersStockAndRefundPutsItBack() throws Exception {
        Doc d = Doc.fromJson(read("fixtures/backup-sample.json"));
        Doc.Product croissant = d.product("p-11");
        assertEquals(6, croissant.stock);
        Cart cart = new Cart();
        assertTrue(cart.add(croissant));
        assertTrue(cart.add(croissant));
        assertTrue(cart.add(d.product("p-3")));
        Doc.Sale s = d.completeSale(cart, Doc.CASH, 5000, 1_000L);
        assertEquals(1006, s.number);
        assertEquals("s-1006", s.id);
        assertEquals(4, croissant.stock);
        // 2 × 900 + 1100 = 2900, SST 6 % = 174 → 3074 → cash 3075
        assertEquals(2900, s.subtotal);
        assertEquals(174, s.tax);
        assertEquals(3075, s.due);
        assertEquals(1925, s.change);
        d.refund(s, 2_000L);
        assertTrue(s.refunded);
        assertEquals(6, croissant.stock);
        d.refund(s, 3_000L); // twice does nothing
        assertEquals(6, croissant.stock);

        croissant.stock = 1;
        Cart c2 = new Cart();
        assertTrue(c2.add(croissant));
        assertFalse("can't sell more than in stock", c2.add(croissant));
        croissant.stock = 0;
        assertTrue(croissant.soldOut());
    }

    @Test
    public void designsCatalogsAndCurrenciesLoad() throws Exception {
        List<Design> designs = Design.parse(read("app/designs.json"));
        assertEquals(5, designs.size());
        assertEquals(Arrays.asList("fresh", "midnight", "cafe", "street", "atelier"),
                designs.stream().map(x -> x.id).collect(java.util.stream.Collectors.toList()));
        assertEquals("fresh", Design.find(designs, "nope").id);
        List<Catalog> types = Catalog.parse(read("app/catalogs.json"));
        assertEquals(5, types.size());
        for (Catalog c : types) {
            assertTrue(c.id, c.products.size() >= 10);
            for (Catalog.Seed s : c.products) assertTrue(s.name, s.category >= 0 && s.category < c.categories.size());
            String design = c.demo.getString("design");
            assertEquals(design, Design.find(designs, design).id);
        }
        List<Currency> money = Currency.parse(read("app/currencies.json"));
        assertEquals("RM", Currency.find(money, "MYR").symbol);
        assertEquals(0, Currency.find(money, "IDR").decimals);
    }

    @Test
    public void demoShopIsBuiltAndRepeatable() throws Exception {
        List<Catalog> types = Catalog.parse(read("app/catalogs.json"));
        List<Currency> money = Currency.parse(read("app/currencies.json"));
        ZoneId kl = ZoneId.of("Asia/Kuala_Lumpur");
        long now = java.time.ZonedDateTime.of(2026, 11, 27, 9, 41, 0, 0, kl).toInstant().toEpochMilli();
        Doc a = Demo.shop(Catalog.find(types, "cafe"), money, now, kl);
        Doc b = Demo.shop(Catalog.find(types, "cafe"), money, now, kl);
        assertEquals(a.toJson().toString(), b.toJson().toString());
        assertTrue(a.setupDone);
        assertEquals("Kopi Pagi", a.brand.name);
        assertTrue(a.sales.size() > 100);
        for (Doc.Sale s : a.sales) assertTrue(s.time <= now);
        assertEquals(1001, a.sales.get(0).number);
        assertEquals(a.sales.size() + 1001, a.nextNumber);
    }

    @Test
    public void demoShopMatchesTheIphoneApp() throws Exception {
        // The iPhone checks get exactly these numbers for the same time and calendar.
        List<Catalog> types = Catalog.parse(read("app/catalogs.json"));
        List<Currency> money = Currency.parse(read("app/currencies.json"));
        ZoneId utc = ZoneId.of("UTC");
        long now = java.time.ZonedDateTime.of(2026, 11, 27, 12, 0, 0, 0, utc).toInstant().toEpochMilli();
        Doc d = Demo.shop(Catalog.find(types, "cafe"), money, now, utc);
        assertEquals(222, d.sales.size());
        long gross = 0;
        int refunded = 0;
        for (Doc.Sale s : d.sales) {
            if (s.refunded) refunded++;
            else gross += s.due;
        }
        assertEquals(1, refunded);
        assertEquals(611027, gross);
    }

    @Test
    public void colourAndTextRules() {
        assertEquals(Colors.WHITE, Colors.on(0xFF6F4E37));
        assertEquals(Colors.NEAR_BLACK, Colors.on(0xFFFCBF49));
        assertEquals(Colors.WHITE, Colors.on(0xFF15803D));
        assertEquals(Colors.NEAR_BLACK, Colors.on(0xFFE0457B));
        assertEquals(0xFFFFDBDB, Colors.soft(0xFFFF0000, 0xFFFFFFFF, false));
        assertEquals(0xFF383838, Colors.soft(0xFFFFFFFF, 0xFF000000, true));
        assertEquals("KP", Text.initials("Kopi Pagi"));
        assertEquals("LC", Text.initials("Lily & Co"));
        assertEquals("S", Text.initials("segar"));
        assertEquals("?", Text.initials("  "));
        assertEquals("6%", Text.percent(600));
        assertEquals("6.5%", Text.percent(650));
        assertEquals(650, Text.parsePercent("6.5"));
    }
}
