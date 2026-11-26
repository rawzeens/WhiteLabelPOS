package com.r4xn.whitelabelpos.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import com.r4xn.whitelabelpos.R;
import com.r4xn.whitelabelpos.core.Colors;
import com.r4xn.whitelabelpos.core.Doc;
import com.r4xn.whitelabelpos.core.Text;

import java.util.List;
import java.util.function.Consumer;

/** Building blocks for every screen. They all take a {@link Look}, so each design paints them its own way. */
public final class Kit {
    private Kit() {
    }

    // ------------------------------------------------------------------ text

    public static TextView text(Look l, CharSequence s, float sp, int color, Typeface face) {
        TextView t = new TextView(l.c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(face);
        t.setIncludeFontPadding(true);
        return t;
    }

    public static TextView title(Look l, String s) {
        TextView t = text(l, l.t(s), l.caps ? 20 : 24, l.ink, l.bold);
        t.setLetterSpacing(l.tracking);
        return t;
    }

    public static TextView heading(Look l, String s) {
        TextView t = text(l, l.t(s), l.caps ? 15 : 17, l.ink, l.bold);
        t.setLetterSpacing(l.tracking);
        return t;
    }

    public static TextView body(Look l, CharSequence s) {
        return text(l, s, 15, l.ink, l.regular);
    }

    public static TextView muted(Look l, CharSequence s) {
        return text(l, s, 13, l.muted, l.regular);
    }

    /** Small label above a group of things. */
    public static TextView label(Look l, String s) {
        TextView t = text(l, s.toUpperCase(java.util.Locale.getDefault()), 12, l.muted, l.medium);
        t.setLetterSpacing(0.06f + l.tracking);
        t.setPadding(l.dp(2), l.dp(18), 0, l.dp(8));
        return t;
    }

    /** Coloured little label ("Refunded", "3 left"). */
    public static TextView pill(Look l, String s, int fg, int bg) {
        TextView t = text(l, l.t(s), 12, fg, l.medium);
        t.setPadding(l.dp(9), l.dp(3), l.dp(9), l.dp(3));
        t.setBackground(round(bg, l.d.radius == 0 ? 0 : l.dp(99)));
        t.setLetterSpacing(l.tracking);
        return t;
    }

    // ------------------------------------------------------------------ shapes

    public static GradientDrawable round(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }

    public static GradientDrawable outline(int fill, int stroke, int strokePx, float radius) {
        GradientDrawable g = round(fill, radius);
        g.setStroke(strokePx, stroke);
        return g;
    }

    /** Card background for this design: flat on light designs, a fine border on dark / minimal / bold ones. */
    public static Drawable cardBg(Look l) {
        switch (l.d.products) {
            case "glow":
            case "lines":
                return outline(l.surface, l.line, Math.max(1, l.dp(1)), l.radius());
            case "blocks":
                return outline(l.surface, l.ink, l.dp(2), 0);
            default:
                return round(l.surface, l.radius());
        }
    }

    public static LinearLayout card(Look l) {
        LinearLayout v = vbox(l.c);
        v.setBackground(cardBg(l));
        v.setPadding(l.dp(16), l.dp(14), l.dp(16), l.dp(14));
        return v;
    }

    // ------------------------------------------------------------------ layout

    public static LinearLayout vbox(Context c) {
        LinearLayout v = new LinearLayout(c);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }

    public static LinearLayout hbox(Context c) {
        LinearLayout v = new LinearLayout(c);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    public static LinearLayout.LayoutParams match() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
    }

    public static LinearLayout.LayoutParams fill() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
    }

    public static LinearLayout.LayoutParams margins(LinearLayout.LayoutParams lp, Look l, int left, int top, int right, int bottom) {
        lp.setMargins(l.dp(left), l.dp(top), l.dp(right), l.dp(bottom));
        return lp;
    }

    public static View gap(Look l, int dp) {
        View v = new View(l.c);
        v.setLayoutParams(new LinearLayout.LayoutParams(l.dp(dp), l.dp(dp)));
        return v;
    }

    public static View grow(Context c) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }

    public static View divider(Look l) {
        View v = new View(l.c);
        v.setBackgroundColor(l.line);
        int thick = l.is("blocks") ? l.dp(2) : Math.max(1, l.dp(1) / 2);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, thick));
        return v;
    }

    public static ScrollView scroll(Look l, View content) {
        ScrollView s = new ScrollView(l.c);
        s.setFillViewport(true);
        s.setClipToPadding(false);
        s.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return s;
    }

    // ------------------------------------------------------------------ icons

    public static ImageView icon(Look l, int res, int color, int sizeDp) {
        ImageView i = new ImageView(l.c);
        i.setImageResource(res);
        i.setImageTintList(ColorStateList.valueOf(color));
        i.setLayoutParams(new LinearLayout.LayoutParams(l.dp(sizeDp), l.dp(sizeDp)));
        return i;
    }

    /** Round tappable icon (back, close, + / −). */
    public static ImageView iconButton(Look l, int res, int color, int bg, Runnable onTap) {
        ImageView i = icon(l, res, color, 40);
        int pad = l.dp(9);
        i.setPadding(pad, pad, pad, pad);
        if (bg != 0) i.setBackground(round(bg, l.d.buttons.equals("square") ? l.buttonRadius() : l.dp(99)));
        i.setOnClickListener(v -> onTap.run());
        return i;
    }

    // ------------------------------------------------------------------ buttons

    public static TextView button(Look l, String s, int fg, Drawable bg, Runnable onTap) {
        boolean street = l.is("blocks");
        TextView b = text(l, l.t(s), street ? 17 : 15, fg, street ? l.heavy : l.bold);
        b.setGravity(Gravity.CENTER);
        b.setLetterSpacing(l.tracking + (l.caps ? 0.02f : 0));
        b.setMinHeight(l.dp(52));
        b.setPadding(l.dp(18), l.dp(10), l.dp(18), l.dp(10));
        b.setBackground(bg);
        b.setOnClickListener(v -> onTap.run());
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    public static TextView primary(Look l, String s, Runnable onTap) {
        return button(l, s, l.onPrimary, round(l.primary, l.buttonRadius()), onTap);
    }

    public static TextView secondary(Look l, String s, Runnable onTap) {
        Drawable bg;
        if (l.is("blocks")) bg = outline(l.surface, l.ink, l.dp(2), 0);
        else if (l.is("lines") || l.is("glow")) bg = outline(Color.TRANSPARENT, l.is("glow") ? l.line : l.ink, Math.max(1, l.dp(1)), l.buttonRadius());
        else bg = round(l.raised, l.buttonRadius());
        return button(l, s, l.ink, bg, onTap);
    }

    public static TextView danger(Look l, String s, Runnable onTap) {
        return button(l, s, l.bad, outline(Color.TRANSPARENT, l.bad, Math.max(1, l.dp(1)), l.buttonRadius()), onTap);
    }

    /** Text-only action in the brand colour. */
    public static TextView link(Look l, String s, Runnable onTap) {
        TextView t = text(l, l.t(s), 14, l.d.dark ? l.primary : linkColor(l), l.bold);
        t.setLetterSpacing(l.tracking);
        t.setPadding(l.dp(8), l.dp(10), l.dp(8), l.dp(10));
        t.setOnClickListener(v -> onTap.run());
        return t;
    }

    /** Brand colour, or ink when the brand colour is too pale to read on the page. */
    public static int linkColor(Look l) {
        double lp = Colors.luminance(l.primary), lb = Colors.luminance(l.page);
        double contrast = (Math.max(lp, lb) + 0.05) / (Math.min(lp, lb) + 0.05);
        return contrast < 3 ? l.ink : l.primary;
    }

    public static void enable(View v, boolean on) {
        v.setEnabled(on);
        v.setAlpha(on ? 1f : 0.4f);
    }

    // ------------------------------------------------------------------ inputs

    public static EditText field(Look l, String hint, String value) {
        EditText e = new EditText(l.c);
        e.setText(value);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        e.setTextColor(l.ink);
        e.setHintTextColor(Colors.withAlpha(l.muted, 0xB0));
        e.setTypeface(l.regular);
        e.setPadding(l.dp(14), l.dp(12), l.dp(14), l.dp(12));
        e.setMinHeight(l.dp(50));
        if (l.is("lines") || l.is("blocks")) {
            e.setBackground(outline(l.surface, l.is("blocks") ? l.ink : l.line, l.is("blocks") ? l.dp(2) : Math.max(1, l.dp(1)), l.buttonRadius()));
        } else {
            e.setBackground(round(l.raised, Math.min(l.radius(), l.dp(14))));
        }
        e.setHighlightColor(Colors.withAlpha(l.primary, 0x55));
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            GradientDrawable cursor = round(Kit.linkColor(l), 0);
            cursor.setSize(l.dp(2), l.dp(20));
            e.setTextCursorDrawable(cursor);
        }
        return e;
    }

    public static EditText moneyField(Look l, String hint, String value) {
        EditText e = field(l, hint, value);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return e;
    }

    public static EditText pinField(Look l, String hint) {
        EditText e = field(l, hint, "");
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        e.setGravity(Gravity.CENTER);
        e.setLetterSpacing(0.5f);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        return e;
    }

    /** A label with its input underneath. */
    public static LinearLayout labelled(Look l, String label, View input) {
        LinearLayout v = vbox(l.c);
        TextView t = text(l, l.t(label), 13, l.muted, l.medium);
        t.setLetterSpacing(l.tracking);
        t.setPadding(l.dp(2), l.dp(14), 0, l.dp(6));
        v.addView(t);
        v.addView(input, match());
        return v;
    }

    public static void onText(EditText e, Consumer<String> change) {
        e.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                change.accept(s.toString());
            }
        });
    }

    /** A row with a title, an optional line under it and an on/off switch. */
    public static LinearLayout toggle(Look l, String title, String sub, boolean on, Consumer<Boolean> change) {
        LinearLayout row = hbox(l.c);
        row.setPadding(0, l.dp(10), 0, l.dp(10));
        LinearLayout texts = vbox(l.c);
        texts.addView(body(l, title));
        if (sub != null && !sub.isEmpty()) texts.addView(muted(l, sub));
        row.addView(texts, weight(1));
        SwitchCompat s = new SwitchCompat(l.c);
        s.setChecked(on);
        int[][] states = {{android.R.attr.state_checked}, {}};
        s.setThumbTintList(new ColorStateList(states, new int[]{l.primary, l.d.dark ? l.muted : Color.WHITE}));
        s.setTrackTintList(new ColorStateList(states, new int[]{Colors.withAlpha(l.primary, 0x80), Colors.withAlpha(l.muted, 0x60)}));
        s.setOnCheckedChangeListener((b, checked) -> change.accept(checked));
        row.addView(s);
        row.setOnClickListener(v -> s.toggle());
        return row;
    }

    // ------------------------------------------------------------------ chips

    /** Category / option chip, drawn the design's way (SPEC section 5). */
    public static View chip(Look l, String s, boolean selected, Runnable onTap) {
        TextView t = text(l, l.t(s), 14, l.ink, l.medium);
        t.setLetterSpacing(l.tracking);
        t.setGravity(Gravity.CENTER);
        t.setPadding(l.dp(16), l.dp(8), l.dp(16), l.dp(8));
        t.setMinHeight(l.dp(38));
        View out = t;
        switch (l.d.chips) {
            case "outline":
                t.setTextColor(selected ? Kit.linkColor(l) : l.muted);
                t.setBackground(outline(selected ? Colors.withAlpha(l.primary, 0x22) : Color.TRANSPARENT,
                        selected ? l.primary : l.line, l.dp(selected ? 1.5f : 1), l.dp(99)));
                break;
            case "solid":
                t.setTypeface(l.bold);
                t.setTextColor(selected ? l.onPrimary : l.page);
                t.setBackground(round(selected ? l.primary : l.ink, 0));
                break;
            case "underline": {
                t.setTextColor(selected ? l.ink : l.muted);
                t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                t.setPadding(l.dp(4), l.dp(8), l.dp(4), l.dp(6));
                LinearLayout box = vbox(l.c);
                box.setGravity(Gravity.CENTER_HORIZONTAL);
                box.addView(t);
                View bar = new View(l.c);
                bar.setBackgroundColor(selected ? l.primary : Color.TRANSPARENT);
                box.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, l.dp(2)));
                box.setPadding(l.dp(8), 0, l.dp(8), 0);
                out = box;
                break;
            }
            case "pill":
                t.setTextColor(selected ? l.onPrimary : l.ink);
                t.setBackground(round(selected ? l.primary : l.raised, l.dp(99)));
                break;
            default: // soft
                t.setTextColor(selected ? l.onPrimary : l.ink);
                t.setBackground(round(selected ? l.primary : l.raised, l.dp(Math.min(l.d.radius, 12))));
        }
        out.setOnClickListener(v -> onTap.run());
        return out;
    }

    /** A horizontally scrolling row of chips. */
    public static android.widget.HorizontalScrollView chipRow(Look l, List<View> chips) {
        android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(l.c);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = hbox(l.c);
        row.setPadding(l.dp(16), 0, l.dp(16), 0);
        for (View chip : chips) {
            LinearLayout.LayoutParams lp = wrap();
            lp.rightMargin = l.dp(l.d.chips.equals("underline") ? 2 : 8);
            row.addView(chip, lp);
        }
        hs.addView(row);
        return hs;
    }

    /** Wrapping grid of equal cells (swatches, choices). */
    public static LinearLayout grid(Look l, List<View> cells, int columns, int gapDp) {
        LinearLayout g = vbox(l.c);
        LinearLayout row = null;
        for (int i = 0; i < cells.size(); i++) {
            if (i % columns == 0) {
                row = hbox(l.c);
                row.setGravity(Gravity.TOP);
                LinearLayout.LayoutParams rp = match();
                if (i > 0) rp.topMargin = l.dp(gapDp);
                g.addView(row, rp);
            }
            LinearLayout.LayoutParams lp = weight(1);
            if (i % columns > 0) lp.leftMargin = l.dp(gapDp);
            row.addView(cells.get(i), lp);
        }
        if (row != null) {
            for (int k = cells.size() % columns; k > 0 && k < columns; k++) {
                LinearLayout.LayoutParams lp = weight(1);
                lp.leftMargin = l.dp(gapDp);
                row.addView(new View(l.c), lp);
            }
        }
        return g;
    }

    // ------------------------------------------------------------------ logo and product icons

    public static Bitmap logoBitmap(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * The shop logo, or its initials, in the design's logo shape. onBrand = it sits on the brand colour
     * (the Street header bar), so the monogram flips its colours.
     */
    public static View logo(Look l, Doc.Brand b, int sizeDp, boolean onBrand) {
        String shape = l.d.logoShape;
        int size = l.dp(sizeDp);
        float r = shape.equals("circle") || shape.equals("ring") ? size / 2f
                : shape.equals("rounded") ? size * 0.28f : shape.equals("outline") ? l.dp(2) : 0;
        int fill = onBrand ? l.onPrimary : l.primary;
        int letters = onBrand ? l.primary : l.onPrimary;
        FrameLayout frame = new FrameLayout(l.c);
        frame.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        View inner;
        Bitmap bmp = logoBitmap(b.logo);
        if (bmp != null) {
            ImageView iv = new ImageView(l.c);
            iv.setImageBitmap(bmp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setBackgroundColor(Color.WHITE);
            inner = iv;
        } else {
            TextView t = text(l, Text.initials(b.name), sizeDp * (shape.equals("ring") ? 0.32f : 0.38f), letters, l.bold);
            t.setGravity(Gravity.CENTER);
            if (shape.equals("outline")) {
                t.setTextColor(l.ink);
                t.setLetterSpacing(0.1f);
            } else {
                t.setBackgroundColor(fill);
            }
            inner = t;
        }
        int inset = shape.equals("ring") ? l.dp(sizeDp * 0.08f) : shape.equals("outline") ? Math.max(1, l.dp(1)) : 0;
        float innerR = Math.max(0, r - inset);
        inner.setClipToOutline(true);
        inner.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), innerR);
            }
        });
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.setMargins(inset, inset, inset, inset);
        frame.addView(inner, lp);
        if (shape.equals("ring")) frame.setBackground(outline(Color.TRANSPARENT, l.primary, l.dp(sizeDp * 0.04f), r));
        if (shape.equals("outline")) frame.setBackground(outline(Color.TRANSPARENT, l.ink, Math.max(1, l.dp(1)), r));
        return frame;
    }

    /** Emoji or initials for a product. */
    public static TextView productIcon(Look l, Doc.Product p, float sp, int color) {
        boolean emoji = p.icon != null && !p.icon.isEmpty();
        TextView t = text(l, emoji ? p.icon : Text.initials(p.name), emoji ? sp : sp * 0.62f, color, l.bold);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    // ------------------------------------------------------------------ dialogs

    /** A dialog box painted in the shop's design. Returns the dialog so callers can close it. */
    public static Dialog sheet(Activity a, Look l, String title, View content, String okLabel, Runnable onOk, boolean danger) {
        Dialog d = new Dialog(a);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = vbox(a);
        box.setPadding(l.dp(22), l.dp(20), l.dp(22), l.dp(14));
        box.setBackground(outline(l.surface, l.d.dark || l.is("lines") ? l.line : l.surface, Math.max(1, l.dp(1)), Math.max(l.radius(), l.dp(4))));
        if (title != null) {
            box.addView(heading(l, title));
            box.addView(gap(l, 8));
        }
        if (content != null) box.addView(content, match());
        LinearLayout buttons = hbox(a);
        buttons.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        buttons.setPadding(0, l.dp(12), 0, 0);
        TextView cancel = text(l, l.t(onOk == null ? "Close" : "Cancel"), 15, l.muted, l.bold);
        cancel.setPadding(l.dp(14), l.dp(12), l.dp(14), l.dp(12));
        cancel.setLetterSpacing(l.tracking);
        cancel.setOnClickListener(v -> d.dismiss());
        buttons.addView(cancel);
        if (onOk != null) {
            TextView ok = text(l, l.t(okLabel), 15, danger ? l.bad : Kit.linkColor(l), l.bold);
            ok.setPadding(l.dp(14), l.dp(12), l.dp(14), l.dp(12));
            ok.setLetterSpacing(l.tracking);
            ok.setOnClickListener(v -> onOk.run());
            buttons.addView(ok);
        }
        box.addView(buttons, match());
        ScrollView sv = new ScrollView(a);
        sv.addView(box);
        d.setContentView(sv);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = Math.min(a.getResources().getDisplayMetrics().widthPixels - l.dp(32), l.dp(460));
            w.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        d.show();
        return d;
    }

    public static void confirm(Activity a, Look l, String title, String message, String yes, boolean danger, Runnable onYes) {
        Dialog[] holder = new Dialog[1];
        holder[0] = sheet(a, l, title, message == null ? null : body(l, message), yes, () -> {
            holder[0].dismiss();
            onYes.run();
        }, danger);
    }

    /** Asks for one line of text. onDone gets the trimmed text. */
    public static void ask(Activity a, Look l, String title, String hint, String value, String ok, Consumer<String> onDone) {
        EditText e = field(l, hint, value);
        e.setSelection(e.getText().length());
        Dialog[] holder = new Dialog[1];
        holder[0] = sheet(a, l, title, e, ok, () -> {
            String s = e.getText().toString().trim();
            if (s.isEmpty()) return;
            holder[0].dismiss();
            onDone.accept(s);
        }, false);
        e.requestFocus();
    }

    /** A list of choices; tapping one closes the dialog. */
    public static void choose(Activity a, Look l, String title, List<String> options, Consumer<Integer> onPick) {
        LinearLayout list = vbox(a);
        Dialog[] holder = new Dialog[1];
        for (int i = 0; i < options.size(); i++) {
            int index = i;
            TextView t = body(l, options.get(i));
            t.setPadding(l.dp(4), l.dp(14), l.dp(4), l.dp(14));
            t.setOnClickListener(v -> {
                holder[0].dismiss();
                onPick.accept(index);
            });
            if (i > 0) list.addView(divider(l));
            list.addView(t, match());
        }
        holder[0] = sheet(a, l, title, list, null, null, false);
    }

    public static void toast(Context c, String s) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
    }
}
