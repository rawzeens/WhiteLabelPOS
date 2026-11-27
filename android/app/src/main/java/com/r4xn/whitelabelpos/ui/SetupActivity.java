package com.r4xn.whitelabelpos.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;

import com.r4xn.whitelabelpos.R;
import com.r4xn.whitelabelpos.core.Catalog;
import com.r4xn.whitelabelpos.core.Colors;
import com.r4xn.whitelabelpos.core.Currency;
import com.r4xn.whitelabelpos.core.Demo;
import com.r4xn.whitelabelpos.core.Design;
import com.r4xn.whitelabelpos.core.Doc;
import com.r4xn.whitelabelpos.core.Money;
import com.r4xn.whitelabelpos.core.Text;
import com.r4xn.whitelabelpos.core.Totals;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * First-launch setup (SPEC section 6): welcome, business, logo & colours, design, money & receipt, finish.
 * Settings opens one step on its own with extra "only" = step.
 */
public class SetupActivity extends BaseActivity {
    static final String[] BRAND = {"#2563EB", "#0EA5E9", "#0F766E", "#15803D", "#65A30D", "#CA8A04", "#EA580C",
            "#D62828", "#E0457B", "#9333EA", "#4F46E5", "#6F4E37", "#1F2937", "#111111"};
    static final String[] ACCENT = {"#F59E0B", "#FCBF49", "#E0A43B", "#22C55E", "#06B6D4", "#F472B6", "#A78BFA", "#B08D57"};
    private static final String[] STEP_NAMES = {"", "Your business", "Logo & colours", "Pick a design", "Money & receipt", "Ready to sell"};

    private Doc draft;
    private int step;
    private int only = -1;
    private boolean starter = true;
    private String pin1 = "", pin2 = "";
    private FrameLayout previewHolder;
    private Backups.Launchers launchers;
    private ActivityResultLauncher<PickVisualMediaRequest> pickLogo;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        launchers = new Backups.Launchers(this, this::afterRestore);
        pickLogo = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), this::logoPicked);
        only = getIntent().getIntExtra("only", -1);
        draft = store.doc.copy();
        if (only >= 0) step = only;
        else if (getIntent().hasExtra("start")) step = getIntent().getIntExtra("start", 0);
        else step = 0;
        if (saved != null) {
            step = saved.getInt("step", step);
            try {
                draft = Doc.fromJson(saved.getString("draft"));
            } catch (Exception ignored) {
                // keep the copy of the saved shop
            }
        }
        render();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("step", step);
        out.putString("draft", draft.toJson().toString());
    }

    private Look draftLook() {
        return new Look(this, Design.find(store.designs, draft.brand.design), draft.brand);
    }

    private Currency cur() {
        return Currency.find(store.currencies, draft.business.currency);
    }

    /** Rebuilds the whole screen in the design being chosen. */
    private void render() {
        look = step == 0 ? new Look(this, store.designs.get(0), new Doc.Brand()) : draftLook();
        LinearLayout v = Kit.vbox(this);
        if (step > 0) v.addView(top(), Kit.match());
        LinearLayout body = Kit.vbox(this);
        body.setPadding(look.dp(20), look.dp(4), look.dp(20), look.dp(24));
        previewHolder = new FrameLayout(this);
        switch (step) {
            case 0:
                welcome(body);
                break;
            case 1:
                business(body);
                break;
            case 2:
                colours(body);
                break;
            case 3:
                design(body);
                break;
            case 4:
                money(body);
                break;
            default:
                finishStep(body);
        }
        android.widget.ScrollView sv = Kit.scroll(look, body);
        v.addView(sv, Kit.fill());
        if (step > 0) v.addView(bottom(), Kit.match());
        setScreen(v, look.page);
        refreshPreview();
    }

    private View top() {
        LinearLayout t = Kit.vbox(this);
        t.setPadding(look.dp(20), look.dp(10), look.dp(20), look.dp(6));
        if (only < 0) {
            LinearLayout bars = Kit.hbox(this);
            for (int i = 1; i <= 5; i++) {
                View seg = new View(this);
                seg.setBackground(Kit.round(i <= step ? look.primary : look.raised, look.d.radius == 0 ? 0 : look.dp(99)));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, look.dp(5), 1f);
                if (i > 1) lp.leftMargin = look.dp(6);
                bars.addView(seg, lp);
            }
            t.addView(bars, Kit.match());
            TextView n = Kit.text(look, look.t("Step " + step + " of 5"), 12, look.muted, look.medium);
            n.setLetterSpacing(look.tracking);
            n.setPadding(0, look.dp(12), 0, 0);
            t.addView(n);
        } else {
            LinearLayout row = Kit.hbox(this);
            row.addView(Kit.iconButton(look, R.drawable.ic_close, look.ink, 0, this::finish));
            t.addView(row);
        }
        t.addView(Kit.title(look, STEP_NAMES[step]));
        return t;
    }

    private View bottom() {
        LinearLayout b = Kit.hbox(this);
        b.setPadding(look.dp(20), look.dp(8), look.dp(20), look.dp(16));
        if (only >= 0) {
            b.addView(Kit.primary(look, "Save", this::saveOnly), Kit.weight(1));
            return b;
        }
        if (step > 1) {
            b.addView(Kit.secondary(look, "Back", () -> go(step - 1)), Kit.weight(1));
            b.addView(Kit.gap(look, 10));
        }
        b.addView(Kit.primary(look, step == 5 ? "Open my POS" : "Next", () -> {
            if (step == 5) finishSetup();
            else go(step + 1);
        }), Kit.weight(step > 1 ? 1.4f : 1));
        return b;
    }

    private void go(int next) {
        if (step == 1 && next > 1 && draft.brand.name.trim().isEmpty()) {
            Kit.toast(this, "Type your business name first");
            return;
        }
        draft.brand.name = draft.brand.name.trim();
        step = next;
        render();
    }

    @Override
    public void onBackPressed() {
        if (only < 0 && step > 1) go(step - 1);
        else if (only < 0 && step == 1) go(0);
        else super.onBackPressed();
    }

    // ------------------------------------------------------------------ preview

    private void refreshPreview() {
        if (previewHolder == null) return;
        previewHolder.removeAllViews();
        Look l = draftLook();
        View p;
        switch (step) {
            case 1:
                p = headerPreview(l);
                break;
            case 2:
            case 5:
                p = Styles.miniSell(l, store, draft, 4, step == 5);
                break;
            case 4:
                p = receiptPreview(l);
                break;
            default:
                return;
        }
        previewHolder.addView(p, new FrameLayout.LayoutParams(-1, -2));
    }

    private View headerPreview(Look l) {
        LinearLayout v = Kit.vbox(this);
        v.setBackground(Kit.outline(l.page, l.d.dark ? l.line : Colors.mix(l.ink, l.page, 0.12), Math.max(1, l.dp(1)), l.dp(18)));
        v.setClipToOutline(true);
        v.addView(Styles.header(l, draft.brand), Kit.match());
        v.addView(Kit.gap(l, 10));
        return v;
    }

    private View receiptPreview(Look l) {
        List<Doc.Product> sample = Styles.sampleProducts(store, draft, 2);
        Doc.Sale sale = new Doc.Sale();
        sale.number = draft.nextNumber;
        sale.time = System.currentTimeMillis();
        long sub = 0;
        for (Doc.Product p : sample) {
            Doc.Line line = new Doc.Line();
            line.name = p.name;
            line.price = p.price;
            line.qty = sale.lines.isEmpty() ? 2 : 1;
            sale.lines.add(line);
            sub += line.amount();
        }
        Doc.Business b = draft.business;
        Totals t = Totals.compute(sub, "none", 0, b.taxRate, b.taxInclusive, b.cashRoundTo, Doc.CASH);
        sale.subtotal = t.subtotal;
        sale.tax = t.tax;
        sale.total = t.total;
        sale.rounding = t.rounding;
        sale.due = t.due;
        sale.taxName = b.taxName;
        sale.taxRate = b.taxRate;
        sale.taxInclusive = b.taxInclusive;
        sale.method = Doc.CASH;
        sale.paid = Money.nextThousand(t.due) * (cur().decimals == 0 ? 100 : 1);
        sale.change = sale.paid - t.due;
        LinearLayout wrap = new LinearLayout(this);
        wrap.setGravity(Gravity.CENTER_HORIZONTAL);
        wrap.setBackground(Kit.round(l.raised, l.radius()));
        wrap.setPadding(l.dp(16), l.dp(16), l.dp(16), l.dp(16));
        View paper = Receipts.build(l, draft, cur(), sale);
        wrap.addView(paper, new LinearLayout.LayoutParams(Math.min(l.dp(320), getResources().getDisplayMetrics().widthPixels - l.dp(72)), -2));
        return wrap;
    }

    // ------------------------------------------------------------------ step 0: welcome

    private void welcome(LinearLayout body) {
        Look l = look;
        body.setPadding(0, l.dp(28), 0, l.dp(24));
        LinearLayout pad = Kit.vbox(this);
        pad.setPadding(l.dp(24), 0, l.dp(24), 0);
        TextView hero = Kit.text(l, "Your shop.\nYour brand.\nYour till.", 34, l.ink, l.heavy);
        hero.setLineSpacing(0, 1.05f);
        pad.addView(hero);
        TextView sub = Kit.text(l, "Set up your own point of sale in a few minutes: your name, logo and colours, and one of five looks made for different kinds of shops.", 16, l.muted, l.regular);
        sub.setPadding(0, l.dp(12), 0, l.dp(20));
        sub.setLineSpacing(0, 1.15f);
        pad.addView(sub);
        body.addView(pad, Kit.match());

        // The five designs, each shown with a made-up shop.
        LinearLayout row = Kit.hbox(this);
        row.setGravity(Gravity.TOP);
        row.setPadding(l.dp(24), 0, l.dp(12), 0);
        for (Catalog c : store.catalogs) {
            Doc demo = demoBrand(c);
            Look dl = new Look(this, Design.find(store.designs, demo.brand.design), demo.brand);
            LinearLayout card = Kit.vbox(this);
            View mini = Styles.miniSell(dl, store, demo, 2, true);
            card.addView(mini, new LinearLayout.LayoutParams(l.dp(270), -2));
            TextView name = Kit.text(l, Design.find(store.designs, demo.brand.design).name, 14, l.ink, l.bold);
            name.setPadding(l.dp(4), l.dp(8), 0, 0);
            card.addView(name);
            LinearLayout.LayoutParams cp = Kit.wrap();
            cp.rightMargin = l.dp(12);
            row.addView(card, cp);
        }
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(row);
        body.addView(hs, Kit.match());

        LinearLayout buttons = Kit.vbox(this);
        buttons.setPadding(l.dp(24), l.dp(24), l.dp(24), 0);
        buttons.addView(Kit.primary(l, "Set up my shop", () -> go(1)), Kit.match());
        buttons.addView(Kit.gap(l, 10));
        buttons.addView(Kit.secondary(l, "Restore a backup", () -> Backups.restore(launchers)), Kit.match());
        buttons.addView(Kit.gap(l, 4));
        TextView demoLink = Kit.link(l, "Try a demo shop first", this::pickDemo);
        demoLink.setGravity(Gravity.CENTER);
        buttons.addView(demoLink, Kit.match());
        body.addView(buttons, Kit.match());
    }

    /** A shop with a kind of business's demo name and colours and its starter products, without sales. */
    private Doc demoBrand(Catalog c) {
        Doc d = new Doc();
        d.brand.name = c.demo.optString("name");
        d.brand.tagline = c.demo.optString("tagline");
        d.brand.primary = c.demo.optString("primary");
        d.brand.accent = c.demo.optString("accent");
        d.brand.design = c.demo.optString("design");
        d.business.type = c.id;
        d.useStarterProducts(c, Currency.find(store.currencies, "MYR"));
        return d;
    }

    private void pickDemo() {
        List<String> names = new ArrayList<>();
        for (Catalog c : store.catalogs) names.add(c.icon + "  " + c.demo.optString("name") + " · " + c.name);
        Kit.choose(this, look, "Pick a demo shop", names, i -> {
            store.replace(Demo.shop(store.catalogs.get(i), store.currencies, System.currentTimeMillis(), java.time.ZoneId.systemDefault()));
            store.unlocked = true;
            openTill();
        });
    }

    private void afterRestore() {
        if (store.doc.setupDone) openTill();
        else {
            draft = store.doc.copy();
            go(1);
        }
    }

    // ------------------------------------------------------------------ step 1: business

    private void business(LinearLayout body) {
        Look l = look;
        body.addView(previewHolder, Kit.match());
        EditText name = Kit.field(l, "e.g. Kopi Pagi", draft.brand.name);
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        Kit.onText(name, t -> {
            draft.brand.name = t;
            refreshPreview();
        });
        body.addView(Kit.labelled(l, "Business name", name));
        EditText tag = Kit.field(l, "A short line under your name (optional)", draft.brand.tagline);
        tag.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        Kit.onText(tag, t -> {
            draft.brand.tagline = t.trim();
            refreshPreview();
        });
        body.addView(Kit.labelled(l, "Tagline", tag));

        body.addView(Kit.label(l, "What kind of business?"));
        List<View> kinds = new ArrayList<>();
        for (Catalog c : store.catalogs) {
            boolean sel = c.id.equals(draft.business.type);
            LinearLayout k = Kit.vbox(this);
            k.setPadding(l.dp(12), l.dp(12), l.dp(12), l.dp(12));
            k.setBackground(Kit.outline(sel ? l.primarySoft : l.surface, sel ? l.primary : l.line, l.dp(sel ? 2 : 1), Math.min(l.radius(), l.dp(16))));
            k.addView(Kit.text(l, c.icon, 26, l.ink, l.regular));
            TextView n = Kit.text(l, c.name, 14, l.ink, l.medium);
            n.setPadding(0, l.dp(6), 0, 0);
            n.setMinLines(2);
            k.addView(n);
            k.setOnClickListener(v -> {
                draft.business.type = c.id;
                render();
            });
            kinds.add(k);
        }
        body.addView(Kit.grid(l, kinds, getResources().getConfiguration().screenWidthDp >= 600 ? 5 : 2, 10), Kit.match());

        EditText phone = Kit.field(l, "Shown on receipts (optional)", draft.business.phone);
        phone.setInputType(InputType.TYPE_CLASS_PHONE);
        Kit.onText(phone, t -> draft.business.phone = t.trim());
        body.addView(Kit.labelled(l, "Phone", phone));
        EditText addr = Kit.field(l, "Shown on receipts (optional)", draft.business.address);
        addr.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        Kit.onText(addr, t -> draft.business.address = t.trim());
        body.addView(Kit.labelled(l, "Address", addr));
        EditText reg = Kit.field(l, "Company or tax number (optional)", draft.business.regNo);
        Kit.onText(reg, t -> draft.business.regNo = t.trim());
        body.addView(Kit.labelled(l, "Registration number", reg));
    }

    // ------------------------------------------------------------------ step 2: logo and colours

    private void colours(LinearLayout body) {
        Look l = look;
        body.addView(previewHolder, Kit.match());

        body.addView(Kit.label(l, "Logo"));
        LinearLayout logoRow = Kit.hbox(this);
        logoRow.addView(Kit.logo(l, draft.brand, 72, false));
        LinearLayout logoButtons = Kit.vbox(this);
        logoButtons.setPadding(l.dp(16), 0, 0, 0);
        logoButtons.addView(Kit.secondary(l, draft.brand.logo.isEmpty() ? "Choose a picture" : "Change picture", () ->
                pickLogo.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build())), Kit.match());
        if (!draft.brand.logo.isEmpty()) {
            logoButtons.addView(Kit.link(l, "Remove picture, use initials", () -> {
                draft.brand.logo = "";
                render();
            }));
        } else {
            logoButtons.addView(Kit.muted(l, "No picture? Your initials are used."));
        }
        logoRow.addView(logoButtons, Kit.weight(1));
        body.addView(logoRow, Kit.match());

        body.addView(Kit.label(l, "Brand colour"));
        body.addView(swatches(BRAND, draft.brand.primary, hex -> draft.brand.primary = hex), Kit.match());
        body.addView(hexField(draft.brand.primary, hex -> draft.brand.primary = hex), Kit.match());

        body.addView(Kit.label(l, "Accent colour"));
        TextView why = Kit.muted(l, "Used for badges, like how many of an item are in the sale.");
        why.setPadding(0, 0, 0, l.dp(8));
        body.addView(why);
        body.addView(swatches(ACCENT, draft.brand.accent, hex -> draft.brand.accent = hex), Kit.match());
        body.addView(hexField(draft.brand.accent, hex -> draft.brand.accent = hex), Kit.match());
    }

    private View swatches(String[] colors, String current, java.util.function.Consumer<String> set) {
        Look l = look;
        List<View> cells = new ArrayList<>();
        for (String hex : colors) {
            int c = Colors.parse(hex, 0);
            boolean sel = hex.equalsIgnoreCase(current);
            FrameLayout f = new FrameLayout(this);
            View dot = new View(this);
            dot.setBackground(Kit.outline(c, sel ? l.ink : Colors.mix(l.ink, c, 0.08), l.dp(sel ? 3 : 1), l.d.radius == 0 ? 0 : l.dp(99)));
            f.addView(dot, new FrameLayout.LayoutParams(l.dp(38), l.dp(38), Gravity.CENTER));
            if (sel) {
                android.widget.ImageView tick = Kit.icon(l, R.drawable.ic_check, Colors.on(c), 20);
                f.addView(tick, new FrameLayout.LayoutParams(l.dp(20), l.dp(20), Gravity.CENTER));
            }
            f.setMinimumHeight(l.dp(44));
            f.setOnClickListener(v -> {
                set.accept(hex);
                render();
            });
            cells.add(f);
        }
        return Kit.grid(l, cells, 7, 4);
    }

    private View hexField(String value, java.util.function.Consumer<String> set) {
        EditText e = Kit.field(look, "#RRGGBB", value);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        Kit.onText(e, t -> {
            String h = t.trim().startsWith("#") ? t.trim() : "#" + t.trim();
            if (Colors.isHex(h)) {
                set.accept(h.toUpperCase(java.util.Locale.ROOT));
                refreshPreview();
            }
        });
        LinearLayout.LayoutParams lp = Kit.match();
        lp.topMargin = look.dp(8);
        e.setLayoutParams(lp);
        LinearLayout wrap = Kit.vbox(this);
        wrap.addView(e, lp);
        return wrap;
    }

    private void logoPicked(Uri uri) {
        if (uri == null) return;
        try {
            Bitmap bmp;
            if (Build.VERSION.SDK_INT >= 28) {
                bmp = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri), (decoder, info, src) -> {
                    int w = info.getSize().getWidth(), h = info.getSize().getHeight();
                    float scale = Math.min(1f, 512f / Math.max(w, h));
                    decoder.setTargetSize(Math.max(1, Math.round(w * scale)), Math.max(1, Math.round(h * scale)));
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                });
            } else {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    BitmapFactory.decodeStream(in, null, o);
                }
                int sample = 1;
                while (Math.max(o.outWidth, o.outHeight) / sample > 1024) sample *= 2;
                BitmapFactory.Options o2 = new BitmapFactory.Options();
                o2.inSampleSize = sample;
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    bmp = BitmapFactory.decodeStream(in, null, o2);
                }
                if (bmp == null) throw new IllegalStateException("not a picture");
                float scale = Math.min(1f, 512f / Math.max(bmp.getWidth(), bmp.getHeight()));
                if (scale < 1f) bmp = Bitmap.createScaledBitmap(bmp, Math.round(bmp.getWidth() * scale), Math.round(bmp.getHeight() * scale), true);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            draft.brand.logo = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
            render();
        } catch (Exception e) {
            Kit.toast(this, "Couldn't open that picture");
        }
    }

    // ------------------------------------------------------------------ step 3: design

    private void design(LinearLayout body) {
        Look l = look;
        TextView intro = Kit.muted(l, "Each design changes the whole till: layout, letters, shapes and colours. Your logo and colours go on top. You can switch any time in Settings.");
        intro.setPadding(0, l.dp(2), 0, l.dp(6));
        body.addView(intro);
        List<View> cards = new ArrayList<>();
        for (Design d : store.designs) {
            boolean sel = d.id.equals(draft.brand.design);
            Look dl = new Look(this, d, draft.brand);
            LinearLayout card = Kit.vbox(this);
            card.setPadding(l.dp(12), l.dp(12), l.dp(12), l.dp(14));
            card.setBackground(Kit.outline(sel ? l.primarySoft : l.surface, sel ? l.primary : l.line, l.dp(sel ? 2.5f : 1), Math.max(l.dp(8), Math.min(l.radius(), l.dp(20)))));
            card.addView(Styles.miniSell(dl, store, draft, 4, true), Kit.match());
            LinearLayout names = Kit.hbox(this);
            names.setPadding(l.dp(4), l.dp(12), l.dp(4), 0);
            LinearLayout texts = Kit.vbox(this);
            texts.addView(Kit.text(l, d.name, 18, l.ink, l.bold));
            TextView about = Kit.muted(l, d.about);
            texts.addView(about);
            names.addView(texts, Kit.weight(1));
            if (sel) names.addView(Kit.pill(l, "Chosen", l.onPrimary, l.primary));
            card.addView(names, Kit.match());
            card.setOnClickListener(v -> {
                draft.brand.design = d.id;
                render();
            });
            cards.add(card);
        }
        int cols = getResources().getConfiguration().screenWidthDp >= 900 ? 3 : getResources().getConfiguration().screenWidthDp >= 600 ? 2 : 1;
        LinearLayout.LayoutParams gp = Kit.match();
        gp.topMargin = l.dp(10);
        body.addView(Kit.grid(l, cards, cols, 14), gp);
    }

    // ------------------------------------------------------------------ step 4: money and receipt

    private void money(LinearLayout body) {
        Look l = look;
        Doc.Business b = draft.business;
        Currency c = cur();
        body.addView(Kit.label(l, "Currency"));
        LinearLayout curRow = Kit.card(l);
        curRow.setOrientation(LinearLayout.HORIZONTAL);
        curRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout ct = Kit.vbox(this);
        ct.addView(Kit.text(l, c.symbol + "  ·  " + c.name, 16, l.ink, l.medium));
        ct.addView(Kit.muted(l, "Prices are shown like " + Money.format(c.decimals == 0 ? 1250000 : 1250, c)));
        curRow.addView(ct, Kit.weight(1));
        curRow.addView(Kit.icon(l, R.drawable.ic_chevron, l.muted, 18));
        curRow.setOnClickListener(v -> {
            List<String> names = new ArrayList<>();
            for (Currency x : store.currencies) names.add(x.symbol + "  " + x.name + " (" + x.code + ")");
            Kit.choose(this, l, "Currency", names, i -> {
                Currency x = store.currencies.get(i);
                if (!x.code.equals(b.currency)) {
                    b.currency = x.code;
                    b.taxName = x.taxName;
                    b.cashRoundTo = x.cashRoundTo;
                }
                render();
            });
        });
        body.addView(curRow, Kit.match());

        body.addView(Kit.label(l, "Tax"));
        LinearLayout taxRow = Kit.hbox(this);
        taxRow.setGravity(Gravity.TOP);
        EditText taxName = Kit.field(l, "Tax", b.taxName);
        Kit.onText(taxName, t -> {
            b.taxName = t.trim();
            refreshPreview();
        });
        taxRow.addView(Kit.labelled(l, "Name on receipts", taxName), Kit.weight(1.2f));
        taxRow.addView(Kit.gap(l, 10));
        EditText rate = Kit.moneyField(l, "0", b.taxRate == 0 ? "" : Text.percent(b.taxRate).replace("%", ""));
        Kit.onText(rate, t -> {
            int bp = t.trim().isEmpty() ? 0 : Text.parsePercent(t);
            if (bp >= 0) {
                b.taxRate = bp;
                refreshPreview();
            }
        });
        taxRow.addView(Kit.labelled(l, "Rate %", rate), Kit.weight(1));
        body.addView(taxRow, Kit.match());
        body.addView(Kit.toggle(l, "Prices already include tax", "Turn off to add tax on top at the till", b.taxInclusive, on -> {
            b.taxInclusive = on;
            refreshPreview();
        }), Kit.match());

        int step = b.cashRoundTo > 0 ? b.cashRoundTo : c.cashRoundTo;
        if (step > 0) {
            body.addView(Kit.toggle(l, "Round cash to the nearest " + Money.format(step, c), "Card and e-wallet payments are never rounded",
                    b.cashRoundTo > 0, on -> {
                        b.cashRoundTo = on ? step : 0;
                        refreshPreview();
                    }), Kit.match());
        }

        EditText footer = Kit.field(l, "e.g. Thank you, come again!", b.footer);
        footer.setSingleLine(false);
        footer.setMinLines(2);
        footer.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        Kit.onText(footer, t -> {
            b.footer = t.trim();
            refreshPreview();
        });
        body.addView(Kit.labelled(l, "Last line of every receipt", footer));

        body.addView(Kit.label(l, "Your receipt"));
        body.addView(previewHolder, Kit.match());
    }

    // ------------------------------------------------------------------ step 5: finish

    private void finishStep(LinearLayout body) {
        Look l = look;
        TextView intro = Kit.muted(l, "This is how your till will look. You can change anything later in Settings.");
        intro.setPadding(0, 0, 0, l.dp(12));
        body.addView(intro);
        body.addView(previewHolder, Kit.match());
        if (draft.products.isEmpty()) {
            Catalog type = Catalog.find(store.catalogs, draft.business.type);
            body.addView(Kit.label(l, "Products"));
            body.addView(Kit.toggle(l, "Add the " + type.name.toLowerCase(java.util.Locale.getDefault()) + " starter products",
                    type.products.size() + " ready-made products with prices you can change. Turn off to start empty.", starter, on -> starter = on), Kit.match());
        }
        body.addView(Kit.label(l, "Owner PIN (optional)"));
        body.addView(Kit.muted(l, "4 numbers that lock Products, Settings and refunds, so staff can only sell."));
        LinearLayout pins = Kit.hbox(this);
        pins.setPadding(0, l.dp(10), 0, 0);
        EditText a = Kit.pinField(l, "PIN");
        a.setText(pin1);
        EditText b = Kit.pinField(l, "Again");
        b.setText(pin2);
        Kit.onText(a, t -> pin1 = t);
        Kit.onText(b, t -> pin2 = t);
        pins.addView(a, Kit.weight(1));
        pins.addView(Kit.gap(l, 10));
        pins.addView(b, Kit.weight(1));
        body.addView(pins, Kit.match());
    }

    private void finishSetup() {
        if (!pin1.isEmpty() || !pin2.isEmpty()) {
            if (pin1.length() != 4) {
                Kit.toast(this, "The PIN needs 4 numbers, or leave it empty");
                return;
            }
            if (!pin1.equals(pin2)) {
                Kit.toast(this, "The two PINs are different");
                return;
            }
            draft.pin = pin1;
        }
        if (starter && draft.products.isEmpty()) {
            draft.useStarterProducts(Catalog.find(store.catalogs, draft.business.type), cur());
        }
        draft.setupDone = true;
        store.replace(draft);
        store.unlocked = true;
        openTill();
    }

    private void saveOnly() {
        if (draft.brand.name.trim().isEmpty()) {
            Kit.toast(this, "Type your business name");
            return;
        }
        draft.brand.name = draft.brand.name.trim();
        Doc d = store.doc;
        d.brand.name = draft.brand.name;
        d.brand.tagline = draft.brand.tagline;
        d.brand.logo = draft.brand.logo;
        d.brand.primary = draft.brand.primary;
        d.brand.accent = draft.brand.accent;
        d.brand.design = draft.brand.design;
        d.business.type = draft.business.type;
        d.business.phone = draft.business.phone;
        d.business.address = draft.business.address;
        d.business.regNo = draft.business.regNo;
        d.business.currency = draft.business.currency;
        d.business.taxName = draft.business.taxName;
        d.business.taxRate = draft.business.taxRate;
        d.business.taxInclusive = draft.business.taxInclusive;
        d.business.cashRoundTo = draft.business.cashRoundTo;
        d.business.footer = draft.business.footer;
        store.save();
        finish();
    }

    private void openTill() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
