package com.r4xn.whitelabelpos.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.r4xn.whitelabelpos.Store;
import com.r4xn.whitelabelpos.core.Catalog;
import com.r4xn.whitelabelpos.core.Colors;
import com.r4xn.whitelabelpos.core.Currency;
import com.r4xn.whitelabelpos.core.Doc;
import com.r4xn.whitelabelpos.core.Money;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** How each of the 5 design environments draws its header and its products (SPEC section 5). */
public final class Styles {
    private Styles() {
    }

    // ------------------------------------------------------------------ header

    /** The shop's name and logo at the top of the Sell screen, in the design's header style. */
    public static View header(Look l, Doc.Brand b) {
        String name = b.name == null || b.name.isEmpty() ? "Your shop" : b.name;
        switch (l.d.header) {
            case "centered": {
                LinearLayout v = Kit.vbox(l.c);
                v.setGravity(Gravity.CENTER_HORIZONTAL);
                v.setPadding(l.dp(16), l.dp(14), l.dp(16), l.dp(6));
                v.addView(Kit.logo(l, b, 64, false));
                TextView n = Kit.text(l, name, 24, l.ink, l.bold);
                n.setGravity(Gravity.CENTER);
                n.setPadding(0, l.dp(8), 0, 0);
                v.addView(n);
                if (!b.tagline.isEmpty()) {
                    TextView t = Kit.text(l, b.tagline, 14, l.muted, android.graphics.Typeface.create(l.regular, android.graphics.Typeface.ITALIC));
                    t.setGravity(Gravity.CENTER);
                    v.addView(t);
                }
                return v;
            }
            case "bar": {
                LinearLayout v = Kit.hbox(l.c);
                v.setBackgroundColor(l.primary);
                v.setPadding(l.dp(16), l.dp(14), l.dp(16), l.dp(14));
                v.addView(Kit.logo(l, b, 44, true));
                LinearLayout texts = Kit.vbox(l.c);
                texts.setPadding(l.dp(12), 0, 0, 0);
                TextView n = Kit.text(l, l.t(name), 24, l.onPrimary, l.heavy);
                n.setLetterSpacing(0.01f);
                n.setMaxLines(1);
                n.setEllipsize(TextUtils.TruncateAt.END);
                texts.addView(n);
                if (!b.tagline.isEmpty()) {
                    TextView t = Kit.text(l, l.t(b.tagline), 13, Colors.withAlpha(l.onPrimary, 0xCC), l.medium);
                    t.setLetterSpacing(0.04f);
                    texts.addView(t);
                }
                v.addView(texts, Kit.weight(1));
                return v;
            }
            case "minimal": {
                LinearLayout v = Kit.vbox(l.c);
                v.setGravity(Gravity.CENTER_HORIZONTAL);
                v.setPadding(l.dp(16), l.dp(16), l.dp(16), 0);
                LinearLayout row = Kit.hbox(l.c);
                row.setGravity(Gravity.CENTER);
                row.addView(Kit.logo(l, b, 30, false));
                TextView n = Kit.text(l, name.toUpperCase(java.util.Locale.getDefault()), 16, l.ink, l.medium);
                n.setLetterSpacing(0.3f);
                n.setPadding(l.dp(12), 0, 0, 0);
                n.setMaxLines(1);
                row.addView(n);
                v.addView(row);
                if (!b.tagline.isEmpty()) {
                    TextView t = Kit.text(l, b.tagline.toUpperCase(java.util.Locale.getDefault()), 10, l.muted, l.regular);
                    t.setLetterSpacing(0.25f);
                    t.setPadding(0, l.dp(6), 0, 0);
                    v.addView(t);
                }
                View hair = new View(l.c);
                hair.setBackgroundColor(l.line);
                LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, l.dp(1) / 2));
                hp.topMargin = l.dp(14);
                v.addView(hair, hp);
                return v;
            }
            default: { // plain and glow
                LinearLayout outer = Kit.vbox(l.c);
                LinearLayout v = Kit.hbox(l.c);
                v.setPadding(l.dp(16), l.dp(14), l.dp(16), l.dp(10));
                v.addView(Kit.logo(l, b, 46, false));
                LinearLayout texts = Kit.vbox(l.c);
                texts.setPadding(l.dp(12), 0, 0, 0);
                TextView n = Kit.text(l, name, 21, l.ink, l.bold);
                n.setMaxLines(1);
                n.setEllipsize(TextUtils.TruncateAt.END);
                texts.addView(n);
                if (!b.tagline.isEmpty()) {
                    TextView t = Kit.muted(l, b.tagline);
                    t.setMaxLines(1);
                    t.setEllipsize(TextUtils.TruncateAt.END);
                    texts.addView(t);
                }
                v.addView(texts, Kit.weight(1));
                outer.addView(v);
                if (l.d.header.equals("glow")) {
                    View glow = new View(l.c);
                    GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                            new int[]{l.primary, Colors.withAlpha(l.primary, 0x55), Colors.withAlpha(l.primary, 0)});
                    glow.setBackground(g);
                    LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, l.dp(2));
                    gp.setMargins(l.dp(16), l.dp(2), l.dp(16), l.dp(4));
                    outer.addView(glow, gp);
                }
                return outer;
            }
        }
    }

    // ------------------------------------------------------------------ one product

    /** A product on the Sell screen, with its "already in the cart" count. */
    public static final class Cell {
        public final Doc.Product p;
        public final View view;
        private final Consumer<Integer> onQty;

        Cell(Doc.Product p, View view, Consumer<Integer> onQty) {
            this.p = p;
            this.view = view;
            this.onQty = onQty;
        }

        public void setQty(int qty) {
            onQty.accept(qty);
        }
    }

    private static TextView badge(Look l) {
        TextView b = Kit.text(l, "", 12, l.onAccent, l.bold);
        b.setGravity(Gravity.CENTER);
        b.setMinWidth(l.dp(24));
        b.setPadding(l.dp(6), l.dp(2), l.dp(6), l.dp(2));
        b.setBackground(Kit.round(l.accent, l.d.radius == 0 ? 0 : l.dp(99)));
        b.setVisibility(View.GONE);
        return b;
    }

    private static String stockNote(Doc.Product p) {
        if (!p.trackStock) return "";
        if (p.stock <= 0) return "Sold out";
        return p.stock <= 5 ? p.stock + " left" : "";
    }

    public static Cell cell(Look l, Currency cur, Doc doc, Doc.Product p, int qty, Runnable onTap) {
        Cell c;
        switch (l.d.products) {
            case "glow":
                c = glow(l, cur, p);
                break;
            case "menu":
                c = menuRow(l, cur, p);
                break;
            case "blocks":
                c = block(l, cur, p);
                break;
            case "lines":
                c = lineRow(l, cur, doc, p);
                break;
            default:
                c = tile(l, cur, p);
        }
        c.setQty(qty);
        if (p.soldOut()) c.view.setAlpha(0.45f);
        if (onTap != null) {
            c.view.setOnClickListener(v -> onTap.run());
            c.view.setClickable(true);
        }
        return c;
    }

    private static Cell tile(Look l, Currency cur, Doc.Product p) {
        FrameLayout f = new FrameLayout(l.c);
        LinearLayout v = Kit.vbox(l.c);
        v.setBackground(Kit.cardBg(l));
        v.setPadding(l.dp(12), l.dp(12), l.dp(12), l.dp(12));
        int color = Colors.parse(p.color, l.primary);
        TextView icon = Kit.productIcon(l, p, 24, Colors.on(Colors.soft(color, l.page, false)) == Colors.WHITE ? color : l.ink);
        icon.setBackground(Kit.round(Colors.soft(color, l.surface, false), l.dp(99)));
        v.addView(icon, new LinearLayout.LayoutParams(l.dp(48), l.dp(48)));
        TextView name = Kit.text(l, l.t(p.name), 14, l.ink, l.medium);
        name.setMaxLines(2);
        name.setMinLines(2);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setPadding(0, l.dp(8), 0, l.dp(2));
        v.addView(name);
        String note = stockNote(p);
        TextView price = Kit.text(l, p.soldOut() ? "Sold out" : Money.format(p.price, cur), 15, p.soldOut() ? l.muted : Kit.linkColor(l), l.bold);
        v.addView(price);
        TextView sub = Kit.text(l, p.soldOut() ? "" : note, 11, l.muted, l.regular);
        sub.setVisibility(note.isEmpty() || p.soldOut() ? View.GONE : View.VISIBLE);
        v.addView(sub);
        f.addView(v, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView b = badge(l);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        bp.setMargins(0, l.dp(8), l.dp(8), 0);
        f.addView(b, bp);
        return new Cell(p, f, q -> {
            b.setText(String.valueOf(q));
            b.setVisibility(q > 0 ? View.VISIBLE : View.GONE);
        });
    }

    private static Cell glow(Look l, Currency cur, Doc.Product p) {
        FrameLayout f = new FrameLayout(l.c);
        f.setMinimumHeight(l.dp(96));
        int color = Colors.parse(p.color, l.primary);
        LinearLayout row = Kit.hbox(l.c);
        row.setGravity(Gravity.TOP);
        View stripe = new View(l.c);
        stripe.setBackground(Kit.round(color, l.dp(2)));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(l.dp(4), ViewGroup.LayoutParams.MATCH_PARENT);
        sp.setMargins(l.dp(8), l.dp(12), 0, l.dp(12));
        row.addView(stripe, sp);
        LinearLayout v = Kit.vbox(l.c);
        v.setPadding(l.dp(12), l.dp(12), l.dp(12), l.dp(12));
        LinearLayout top = Kit.hbox(l.c);
        top.setGravity(Gravity.TOP);
        top.addView(Kit.productIcon(l, p, 22, l.ink), new LinearLayout.LayoutParams(l.dp(30), l.dp(30)));
        TextView name = Kit.text(l, p.name, 15, l.ink, l.medium);
        name.setMaxLines(2);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setPadding(l.dp(8), l.dp(3), l.dp(20), 0);
        top.addView(name, Kit.weight(1));
        v.addView(top);
        String note = stockNote(p);
        TextView price = Kit.text(l, p.soldOut() ? "Sold out" : Money.format(p.price, cur), 19, p.soldOut() ? l.muted : l.primary, l.bold);
        price.setPadding(0, l.dp(10), 0, 0);
        v.addView(price);
        if (!note.isEmpty() && !p.soldOut()) v.addView(Kit.text(l, note, 11, l.muted, l.regular));
        row.addView(v, Kit.weight(1));
        f.addView(row, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        TextView b = badge(l);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        bp.setMargins(0, l.dp(10), l.dp(10), 0);
        f.addView(b, bp);
        return new Cell(p, f, q -> {
            b.setText(String.valueOf(q));
            b.setVisibility(q > 0 ? View.VISIBLE : View.GONE);
            f.setBackground(Kit.outline(q > 0 ? Colors.mix(l.primary, l.surface, 0.10) : l.surface,
                    q > 0 ? l.primary : l.line, q > 0 ? l.dp(1.5f) : Math.max(1, l.dp(1)), l.radius()));
        });
    }

    /** A dotted line between a menu item's name and its price. */
    static final class Leader extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float step;

        Leader(Context c, int color, float step) {
            super(c);
            paint.setColor(color);
            this.step = step;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float y = getHeight() * 0.68f;
            float r = step / 5f;
            for (float x = step / 2; x < getWidth(); x += step) canvas.drawCircle(x, y, r, paint);
        }
    }

    private static Cell menuRow(Look l, Currency cur, Doc.Product p) {
        LinearLayout row = Kit.hbox(l.c);
        row.setGravity(Gravity.BOTTOM);
        row.setPadding(l.dp(2), l.dp(11), l.dp(2), l.dp(11));
        TextView icon = Kit.productIcon(l, p, 18, l.muted);
        row.addView(icon, new LinearLayout.LayoutParams(l.dp(30), ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout names = Kit.vbox(l.c);
        TextView name = Kit.text(l, p.name, 17, l.ink, l.regular);
        name.setMaxLines(2);
        names.addView(name);
        String note = stockNote(p);
        if (!note.isEmpty()) names.addView(Kit.text(l, note, 12, l.muted, l.regular));
        row.addView(names, Kit.wrap());
        Leader dots = new Leader(l.c, Colors.withAlpha(l.muted, 0x99), l.dp(6));
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(0, l.dp(18), 1f);
        dp.setMargins(l.dp(6), 0, l.dp(6), l.dp(3));
        row.addView(dots, dp);
        TextView b = badge(l);
        LinearLayout.LayoutParams bp = Kit.wrap();
        bp.setMargins(0, 0, l.dp(8), l.dp(2));
        row.addView(b, bp);
        TextView price = Kit.text(l, p.soldOut() ? "Sold out" : Money.format(p.price, cur), 17, p.soldOut() ? l.muted : l.ink, l.bold);
        row.addView(price);
        return new Cell(p, row, q -> {
            b.setText("× " + q);
            b.setVisibility(q > 0 ? View.VISIBLE : View.GONE);
        });
    }

    private static Cell block(Look l, Currency cur, Doc.Product p) {
        FrameLayout f = new FrameLayout(l.c);
        int color = Colors.parse(p.color, l.primary);
        int on = Colors.on(color);
        f.setBackgroundColor(color);
        f.setMinimumHeight(l.dp(116));
        TextView big = Kit.productIcon(l, p, 46, on);
        big.setAlpha(0.35f);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        ip.setMargins(0, l.dp(4), l.dp(8), 0);
        f.addView(big, ip);
        LinearLayout v = Kit.vbox(l.c);
        v.setGravity(Gravity.BOTTOM);
        v.setPadding(l.dp(12), l.dp(12), l.dp(12), l.dp(12));
        TextView name = Kit.text(l, l.t(p.name), 18, on, l.heavy);
        name.setMaxLines(2);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setLineSpacing(0, 0.92f);
        v.addView(name);
        TextView price = Kit.text(l, p.soldOut() ? "SOLD OUT" : Money.format(p.price, cur), 16, on, l.bold);
        price.setPadding(0, l.dp(2), 0, 0);
        v.addView(price);
        String note = stockNote(p);
        if (!note.isEmpty() && !p.soldOut()) v.addView(Kit.text(l, l.t(note), 11, Colors.withAlpha(on, 0xCC), l.medium));
        f.addView(v, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        TextView b = Kit.text(l, "", 15, l.page, l.heavy);
        b.setGravity(Gravity.CENTER);
        b.setBackgroundColor(l.ink);
        b.setMinWidth(l.dp(30));
        b.setMinHeight(l.dp(30));
        b.setVisibility(View.GONE);
        f.addView(b, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START));
        return new Cell(p, f, q -> {
            b.setText(String.valueOf(q));
            b.setVisibility(q > 0 ? View.VISIBLE : View.GONE);
        });
    }

    private static Cell lineRow(Look l, Currency cur, Doc doc, Doc.Product p) {
        LinearLayout outer = Kit.vbox(l.c);
        LinearLayout row = Kit.hbox(l.c);
        row.setPadding(l.dp(2), l.dp(15), l.dp(2), l.dp(15));
        LinearLayout names = Kit.vbox(l.c);
        TextView name = Kit.text(l, p.name.toUpperCase(java.util.Locale.getDefault()), 13, l.ink, l.medium);
        name.setLetterSpacing(0.12f);
        names.addView(name);
        String sub = !p.sku.isEmpty() ? p.sku : doc == null ? "" : doc.categoryName(p.category);
        String note = stockNote(p);
        if (!note.isEmpty()) sub = sub.isEmpty() ? note : sub + "  ·  " + note;
        if (!sub.isEmpty()) {
            TextView s = Kit.text(l, sub.toUpperCase(java.util.Locale.getDefault()), 10, l.muted, l.regular);
            s.setLetterSpacing(0.1f);
            s.setPadding(0, l.dp(3), 0, 0);
            names.addView(s);
        }
        row.addView(names, Kit.weight(1));
        TextView q = Kit.text(l, "", 12, Kit.linkColor(l), l.bold);
        q.setLetterSpacing(0.1f);
        q.setPadding(0, 0, l.dp(14), 0);
        row.addView(q);
        TextView price = Kit.text(l, p.soldOut() ? "SOLD OUT" : Money.format(p.price, cur), 14, p.soldOut() ? l.muted : l.ink, l.regular);
        price.setLetterSpacing(0.04f);
        row.addView(price);
        outer.addView(row);
        View hair = new View(l.c);
        hair.setBackgroundColor(l.line);
        outer.addView(hair, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, l.dp(1) / 2)));
        return new Cell(p, outer, n -> {
            q.setText(n > 0 ? "× " + n : "");
            q.setVisibility(n > 0 ? View.VISIBLE : View.GONE);
        });
    }

    // ------------------------------------------------------------------ all products

    /** How many product columns fit in this width (SPEC section 5). */
    public static int columns(Look l, int widthPx) {
        float widthDp = widthPx / l.c.getResources().getDisplayMetrics().density;
        if (l.is("menu") || l.is("lines")) return widthDp >= 900 ? 2 : 1;
        if (widthDp < 600) return l.d.columns;
        int per = l.is("tiles") ? 150 : 170;
        return Math.max(l.d.columns, (int) (widthDp / per));
    }

    /**
     * Lays out the products the design's way. Cells are put in {@code cells} by product id so the
     * caller can update the in-cart counts without rebuilding.
     */
    public static View products(Look l, Store s, Doc doc, Currency cur, List<Doc.Product> list, int widthPx,
                                Map<String, Cell> cells, Consumer<Doc.Product> onTap) {
        int cols = columns(l, widthPx);
        if (l.is("menu") || l.is("lines")) {
            List<View> groups = new ArrayList<>();
            Map<String, List<Doc.Product>> byCat = group(doc, list);
            for (Map.Entry<String, List<Doc.Product>> e : byCat.entrySet()) {
                LinearLayout g = Kit.vbox(l.c);
                if (!e.getKey().isEmpty() || byCat.size() > 1) {
                    String title = e.getKey().isEmpty() ? "Other" : e.getKey();
                    if (l.is("menu")) {
                        TextView t = Kit.text(l, title, 21, Kit.linkColor(l), android.graphics.Typeface.create(l.bold, android.graphics.Typeface.BOLD_ITALIC));
                        t.setPadding(l.dp(2), l.dp(14), 0, l.dp(2));
                        g.addView(t);
                    } else {
                        TextView t = Kit.text(l, title.toUpperCase(java.util.Locale.getDefault()), 11, l.muted, l.medium);
                        t.setLetterSpacing(0.22f);
                        t.setPadding(l.dp(2), l.dp(22), 0, l.dp(4));
                        g.addView(t);
                    }
                }
                for (Doc.Product p : e.getValue()) {
                    Cell c = cell(l, cur, doc, p, s == null ? 0 : s.cart.qty(p.id), onTap == null ? null : () -> onTap.accept(p));
                    if (cells != null) cells.put(p.id, c);
                    g.addView(c.view, Kit.match());
                }
                groups.add(g);
            }
            LinearLayout wrap = Kit.vbox(l.c);
            if (l.is("menu")) {
                wrap.setBackground(Kit.round(l.surface, l.radius()));
                wrap.setPadding(l.dp(18), l.dp(4), l.dp(18), l.dp(16));
            }
            if (cols == 1) {
                for (View g : groups) wrap.addView(g, Kit.match());
                return wrap;
            }
            LinearLayout left = Kit.vbox(l.c), right = Kit.vbox(l.c);
            for (int i = 0; i < groups.size(); i++) (i < (groups.size() + 1) / 2 ? left : right).addView(groups.get(i), Kit.match());
            LinearLayout two = Kit.hbox(l.c);
            two.setGravity(Gravity.TOP);
            two.addView(left, Kit.weight(1));
            two.addView(Kit.gap(l, 28));
            two.addView(right, Kit.weight(1));
            wrap.addView(two, Kit.match());
            return wrap;
        }
        List<View> views = new ArrayList<>();
        for (Doc.Product p : list) {
            Cell c = cell(l, cur, doc, p, s == null ? 0 : s.cart.qty(p.id), onTap == null ? null : () -> onTap.accept(p));
            if (cells != null) cells.put(p.id, c);
            views.add(c.view);
        }
        int gap = l.is("blocks") ? 6 : 10;
        LinearLayout grid = Kit.grid(l, views, cols, gap);
        // Make every cell in a row the same height.
        for (int i = 0; i < grid.getChildCount(); i++) {
            LinearLayout row = (LinearLayout) grid.getChildAt(i);
            row.setGravity(Gravity.FILL_VERTICAL);
            for (int k = 0; k < row.getChildCount(); k++) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) row.getChildAt(k).getLayoutParams();
                lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            }
        }
        return grid;
    }

    /** Products by category name, in category order, with uncategorised ones last. */
    public static Map<String, List<Doc.Product>> group(Doc doc, List<Doc.Product> list) {
        Map<String, List<Doc.Product>> out = new LinkedHashMap<>();
        for (Doc.Category c : doc.categories) out.put(c.name, new ArrayList<>());
        List<Doc.Product> none = new ArrayList<>();
        for (Doc.Product p : list) {
            Doc.Category c = doc.category(p.category);
            if (c == null) none.add(p);
            else out.get(c.name).add(p);
        }
        out.values().removeIf(List::isEmpty);
        if (!none.isEmpty()) out.put("", none);
        return out;
    }

    // ------------------------------------------------------------------ previews (setup, settings)

    /** Up to n products to show in a preview: the shop's own, or the starter ones for its kind of business. */
    public static List<Doc.Product> sampleProducts(Store s, Doc doc, int n) {
        List<Doc.Product> out = new ArrayList<>();
        for (Doc.Product p : doc.products) {
            if (out.size() == n) break;
            out.add(p);
        }
        if (!out.isEmpty()) return out;
        Catalog type = Catalog.find(s.catalogs, doc.business.type);
        long scale = Currency.find(s.currencies, doc.business.currency).decimals == 0 ? 100 : 1;
        for (Catalog.Seed seed : type.products) {
            if (out.size() == n) break;
            Doc.Product p = new Doc.Product();
            p.id = "preview-" + out.size();
            p.name = seed.name;
            p.price = seed.price * scale;
            p.icon = seed.icon;
            p.color = seed.color;
            p.sku = seed.sku;
            out.add(p);
        }
        return out;
    }

    /** A small Sell screen in this design with this shop's brand: header, a few products, the Charge button. */
    public static View miniSell(Look l, Store s, Doc doc, int products, boolean withButton) {
        Currency cur = Currency.find(s.currencies, doc.business.currency);
        LinearLayout v = Kit.vbox(l.c);
        v.setBackground(Kit.outline(l.page, l.d.dark ? l.line : Colors.mix(l.ink, l.page, 0.12), Math.max(1, l.dp(1)), l.dp(18)));
        v.setClipToOutline(true);
        v.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), l.dp(18));
            }
        });
        v.addView(header(l, doc.brand), Kit.match());
        List<Doc.Product> list = sampleProducts(s, doc, products);
        // A fake doc so menu/lines previews don't group under the real categories.
        Doc shadow = new Doc();
        View grid = products(l, null, shadow, cur, list, l.dp(l.is("menu") || l.is("lines") ? 320 : 330), null, null);
        LinearLayout.LayoutParams gp = Kit.match();
        gp.setMargins(l.dp(12), l.dp(10), l.dp(12), l.dp(10));
        v.addView(grid, gp);
        if (withButton) {
            long total = 0;
            for (Doc.Product p : list.subList(0, Math.min(2, list.size()))) total += p.price;
            TextView b = Kit.primary(l, "Charge " + Money.format(total, cur), () -> {
            });
            b.setClickable(false);
            LinearLayout.LayoutParams bp = Kit.match();
            bp.setMargins(l.dp(12), 0, l.dp(12), l.dp(12));
            v.addView(b, bp);
        }
        return v;
    }

    public static int statusColor(Look l, boolean sellTab) {
        return sellTab && l.d.header.equals("bar") ? l.primary : l.page;
    }

    public static boolean lightIcons(int background) {
        return Colors.on(background) == Color.WHITE;
    }
}
