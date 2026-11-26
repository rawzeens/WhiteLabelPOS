package com.r4xn.whitelabelpos.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.r4xn.whitelabelpos.core.Catalog;
import com.r4xn.whitelabelpos.core.Colors;
import com.r4xn.whitelabelpos.core.Doc;
import com.r4xn.whitelabelpos.core.Money;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Add or change a product, with a live preview in the shop's design. */
public class EditProductActivity extends BaseActivity {
    static final String[] COLORS = {"#E63946", "#F77F00", "#E9A23B", "#2A9D8F", "#15803D", "#0EA5E9", "#2563EB",
            "#7C3AED", "#DB2777", "#6F4E37", "#A47551", "#4A4E69", "#64748B", "#111111"};

    private Doc.Product p;
    private boolean isNew;
    private FrameLayout preview;
    private LinearLayout categoryRow, colourRow;
    private EditText price;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        Doc.Product existing = store.doc.product(getIntent().getStringExtra("id"));
        isNew = existing == null;
        p = new Doc.Product();
        if (existing != null) copy(existing, p);
        else {
            p.color = COLORS[store.doc.products.size() % COLORS.length];
            p.category = categoryFromSell();
        }
        build();
    }

    private String categoryFromSell() {
        return store.doc.categories.isEmpty() ? "" : store.doc.categories.get(0).id;
    }

    private static void copy(Doc.Product a, Doc.Product b) {
        b.id = a.id;
        b.name = a.name;
        b.price = a.price;
        b.category = a.category;
        b.icon = a.icon;
        b.color = a.color;
        b.sku = a.sku;
        b.trackStock = a.trackStock;
        b.stock = a.stock;
    }

    private void build() {
        int decimals = store.currency().decimals;
        LinearLayout v = Kit.vbox(this);
        v.addView(topBar(isNew ? "New product" : "Edit product"), Kit.match());
        LinearLayout body = Kit.vbox(this);
        body.setPadding(look.dp(20), 0, look.dp(20), look.dp(24));

        preview = new FrameLayout(this);
        preview.setPadding(0, look.dp(4), 0, look.dp(8));
        body.addView(preview, Kit.match());

        EditText name = Kit.field(look, "e.g. Iced Latte", p.name);
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        Kit.onText(name, t -> {
            p.name = t.trim();
            paintPreview();
        });
        body.addView(Kit.labelled(look, "Name", name));

        price = Kit.moneyField(look, Money.plain(0, decimals), isNew && p.price == 0 ? "" : Money.plain(p.price, decimals));
        Kit.onText(price, t -> {
            long v1 = Money.parse(t, decimals);
            p.price = Math.max(0, v1);
            paintPreview();
        });
        body.addView(Kit.labelled(look, "Price (" + store.currency().symbol + ")", price));

        categoryRow = Kit.vbox(this);
        body.addView(Kit.labelled(look, "Category", categoryRow));
        paintCategories();

        EditText icon = Kit.field(look, "An emoji, or leave empty for initials", p.icon);
        Kit.onText(icon, t -> {
            p.icon = t.trim();
            paintPreview();
        });
        body.addView(Kit.labelled(look, "Picture", icon));
        LinearLayout emojis = Kit.hbox(this);
        List<View> suggestions = new ArrayList<>();
        for (String e : emojiIdeas()) {
            TextView t = Kit.text(look, e, 22, look.ink, look.regular);
            t.setGravity(Gravity.CENTER);
            t.setPadding(look.dp(8), look.dp(6), look.dp(8), look.dp(6));
            t.setOnClickListener(x -> icon.setText(e));
            suggestions.add(t);
        }
        View sugRow = Kit.chipRow(look, suggestions);
        ((LinearLayout) ((android.widget.HorizontalScrollView) sugRow).getChildAt(0)).setPadding(0, look.dp(4), 0, 0);
        body.addView(sugRow, Kit.match());

        colourRow = Kit.vbox(this);
        body.addView(Kit.labelled(look, "Colour", colourRow));
        paintColours();

        EditText sku = Kit.field(look, "Optional code or barcode number", p.sku);
        Kit.onText(sku, t -> {
            p.sku = t.trim();
            paintPreview();
        });
        body.addView(Kit.labelled(look, "Item code", sku));

        body.addView(Kit.label(look, "Stock"));
        EditText stock = Kit.field(look, "How many you have now", p.trackStock ? String.valueOf(p.stock) : "");
        stock.setInputType(InputType.TYPE_CLASS_NUMBER);
        stock.setVisibility(p.trackStock ? View.VISIBLE : View.GONE);
        body.addView(Kit.toggle(look, "Count stock", "Stops selling when it runs out", p.trackStock, on -> {
            p.trackStock = on;
            stock.setVisibility(on ? View.VISIBLE : View.GONE);
            paintPreview();
        }), Kit.match());
        Kit.onText(stock, t -> {
            try {
                p.stock = Integer.parseInt(t.trim());
            } catch (NumberFormatException e) {
                p.stock = 0;
            }
            paintPreview();
        });
        body.addView(stock, Kit.match());

        if (!isNew) {
            body.addView(Kit.gap(look, 28));
            body.addView(Kit.danger(look, "Delete product", this::delete), Kit.match());
        }
        v.addView(Kit.scroll(look, body), Kit.fill());
        LinearLayout foot = Kit.vbox(this);
        foot.setPadding(look.dp(20), look.dp(8), look.dp(20), look.dp(16));
        foot.addView(Kit.primary(look, isNew ? "Add product" : "Save", this::save), Kit.match());
        v.addView(foot, Kit.match());
        setScreen(v, look.page);
        paintPreview();
    }

    private List<String> emojiIdeas() {
        Set<String> out = new LinkedHashSet<>();
        Catalog type = store.catalog();
        for (Catalog.Seed s : type.products) if (!s.icon.isEmpty()) out.add(s.icon);
        for (Catalog c : store.catalogs) for (Catalog.Seed s : c.products) if (!s.icon.isEmpty()) out.add(s.icon);
        return new ArrayList<>(out);
    }

    private void paintPreview() {
        preview.removeAllViews();
        Doc.Product show = new Doc.Product();
        copy(p, show);
        if (show.name.isEmpty()) show.name = "New product";
        LinearLayout box = Kit.vbox(this);
        box.setBackground(Kit.round(look.page, 0));
        View cell = Styles.cell(look, store.currency(), store.doc, show, 0, null).view;
        box.addView(cell, Kit.match());
        boolean grid = !(look.is("menu") || look.is("lines"));
        preview.addView(box, new FrameLayout.LayoutParams(grid ? look.dp(look.is("tiles") ? 170 : 210) : -1, -2, Gravity.CENTER_HORIZONTAL));
    }

    private void paintCategories() {
        categoryRow.removeAllViews();
        List<View> chips = new ArrayList<>();
        chips.add(Kit.chip(look, "None", p.category.isEmpty(), () -> {
            p.category = "";
            paintCategories();
        }));
        for (Doc.Category c : store.doc.categories) {
            chips.add(Kit.chip(look, c.name, c.id.equals(p.category), () -> {
                p.category = c.id;
                paintCategories();
                paintPreview();
            }));
        }
        chips.add(Kit.chip(look, "+ New", false, () -> Kit.ask(this, look, "New category", "e.g. Drinks", "", "Add", name -> {
            Doc.Category c = new Doc.Category();
            c.id = store.doc.newId("c");
            c.name = name;
            store.doc.categories.add(c);
            store.save();
            p.category = c.id;
            paintCategories();
        })));
        View row = Kit.chipRow(look, chips);
        ((LinearLayout) ((android.widget.HorizontalScrollView) row).getChildAt(0)).setPadding(0, 0, 0, 0);
        categoryRow.addView(row, Kit.match());
    }

    private void paintColours() {
        colourRow.removeAllViews();
        List<View> swatches = new ArrayList<>();
        for (String hex : COLORS) {
            int c = Colors.parse(hex, 0);
            boolean sel = hex.equalsIgnoreCase(p.color);
            FrameLayout f = new FrameLayout(this);
            View dot = new View(this);
            dot.setBackground(Kit.outline(c, sel ? look.ink : c, look.dp(sel ? 3 : 0), look.dp(99)));
            f.addView(dot, new FrameLayout.LayoutParams(look.dp(34), look.dp(34), Gravity.CENTER));
            f.setMinimumHeight(look.dp(40));
            f.setOnClickListener(x -> {
                p.color = hex;
                paintColours();
                paintPreview();
            });
            swatches.add(f);
        }
        colourRow.addView(Kit.grid(look, swatches, 7, 6), Kit.match());
    }

    private void save() {
        if (p.name.isEmpty()) {
            Kit.toast(this, "Give the product a name");
            return;
        }
        if (Money.parse(price.getText().toString(), store.currency().decimals) < 0) {
            Kit.toast(this, "Type the price");
            return;
        }
        if (isNew) {
            p.id = store.doc.newId("p");
            store.doc.products.add(p);
        } else {
            Doc.Product target = store.doc.product(p.id);
            if (target != null) copy(p, target);
        }
        store.save();
        finish();
    }

    private void delete() {
        Kit.confirm(this, look, "Delete " + p.name + "?", "Old receipts keep it. It just won't be on the Sell screen.", "Delete", true, () -> {
            store.doc.products.removeIf(x -> x.id.equals(p.id));
            store.cart.items.removeIf(x -> x.productId.equals(p.id));
            store.save();
            finish();
        });
    }
}
