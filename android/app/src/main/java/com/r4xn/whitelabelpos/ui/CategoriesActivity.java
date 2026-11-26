package com.r4xn.whitelabelpos.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.r4xn.whitelabelpos.R;
import com.r4xn.whitelabelpos.core.Doc;

import java.util.Collections;

/** Add, rename, reorder and delete categories. */
public class CategoriesActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        build();
    }

    private void build() {
        LinearLayout v = Kit.vbox(this);
        v.addView(topBar("Categories"), Kit.match());
        LinearLayout body = Kit.vbox(this);
        body.setPadding(look.dp(20), look.dp(4), look.dp(20), look.dp(24));
        body.addView(Kit.muted(look, "Categories are the tabs on the Sell screen. Their order here is the order there."));
        body.addView(Kit.gap(look, 12));
        if (store.doc.categories.isEmpty()) {
            TextView t = Kit.muted(look, "No categories yet.");
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, look.dp(32), 0, look.dp(32));
            body.addView(t, Kit.match());
        } else {
            LinearLayout card = Kit.card(look);
            card.setPadding(look.dp(12), look.dp(2), look.dp(4), look.dp(2));
            for (int i = 0; i < store.doc.categories.size(); i++) {
                Doc.Category c = store.doc.categories.get(i);
                int index = i;
                if (i > 0) card.addView(Kit.divider(look));
                LinearLayout row = Kit.hbox(this);
                row.setPadding(0, look.dp(4), 0, look.dp(4));
                int count = 0;
                for (Doc.Product p : store.doc.products) if (p.category.equals(c.id)) count++;
                LinearLayout texts = Kit.vbox(this);
                texts.addView(Kit.body(look, c.name));
                texts.addView(Kit.muted(look, count == 1 ? "1 product" : count + " products"));
                texts.setOnClickListener(x -> rename(c));
                row.addView(texts, Kit.weight(1));
                if (i > 0) row.addView(Kit.iconButton(look, R.drawable.ic_up, look.muted, 0, () -> move(index, -1)));
                if (i < store.doc.categories.size() - 1) row.addView(Kit.iconButton(look, R.drawable.ic_down, look.muted, 0, () -> move(index, 1)));
                row.addView(Kit.iconButton(look, R.drawable.ic_edit, look.muted, 0, () -> rename(c)));
                row.addView(Kit.iconButton(look, R.drawable.ic_trash, look.bad, 0, () -> delete(c)));
                card.addView(row, Kit.match());
            }
            body.addView(card, Kit.match());
        }
        v.addView(Kit.scroll(look, body), Kit.fill());
        LinearLayout foot = Kit.vbox(this);
        foot.setPadding(look.dp(20), look.dp(8), look.dp(20), look.dp(16));
        foot.addView(Kit.primary(look, "Add category", () -> Kit.ask(this, look, "New category", "e.g. Desserts", "", "Add", name -> {
            Doc.Category c = new Doc.Category();
            c.id = store.doc.newId("c");
            c.name = name;
            store.doc.categories.add(c);
            store.save();
            build();
        })), Kit.match());
        v.addView(foot, Kit.match());
        setScreen(v, look.page);
    }

    private void move(int index, int delta) {
        Collections.swap(store.doc.categories, index, index + delta);
        store.save();
        build();
    }

    private void rename(Doc.Category c) {
        Kit.ask(this, look, "Rename category", "", c.name, "Save", name -> {
            c.name = name;
            store.save();
            build();
        });
    }

    private void delete(Doc.Category c) {
        Kit.confirm(this, look, "Delete " + c.name + "?", "Its products stay, just without a category.", "Delete", true, () -> {
            store.doc.deleteCategory(c.id);
            store.save();
            build();
        });
    }
}
