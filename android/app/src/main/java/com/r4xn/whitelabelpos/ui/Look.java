package com.r4xn.whitelabelpos.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;

import com.r4xn.whitelabelpos.core.Colors;
import com.r4xn.whitelabelpos.core.Design;
import com.r4xn.whitelabelpos.core.Doc;

import java.util.Locale;

/** A design environment with the shop's brand colours laid on top: everything a screen needs to paint itself. */
public final class Look {
    public final Context c;
    public final Design d;
    public final int page, surface, raised, ink, muted, line;
    public final int primary, onPrimary, primarySoft, accent, onAccent;
    public final int good = 0xFF16A34A, bad = 0xFFDC2626;
    public final Typeface regular, medium, bold, heavy;
    public final boolean caps;
    public final float tracking;

    public Look(Context c, Design d, Doc.Brand b) {
        this.c = c;
        this.d = d;
        page = d.page;
        surface = d.surface;
        raised = d.raised;
        ink = d.ink;
        muted = d.muted;
        line = d.line;
        primary = Colors.parse(b.primary, 0xFF2563EB);
        onPrimary = Colors.on(primary);
        primarySoft = Colors.soft(primary, page, d.dark);
        accent = Colors.parse(b.accent, 0xFFF59E0B);
        onAccent = Colors.on(accent);
        caps = d.caps;
        tracking = d.tracking;
        switch (d.font) {
            case "serif":
                regular = Typeface.create("serif", Typeface.NORMAL);
                medium = Typeface.create("serif", Typeface.BOLD);
                bold = Typeface.create("serif", Typeface.BOLD);
                heavy = bold;
                break;
            case "condensed":
                regular = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
                medium = Typeface.create("sans-serif-condensed", Typeface.BOLD);
                bold = Typeface.create("sans-serif-condensed", Typeface.BOLD);
                heavy = Typeface.create("sans-serif-black", Typeface.NORMAL);
                break;
            case "light":
                regular = Typeface.create("sans-serif-light", Typeface.NORMAL);
                medium = Typeface.create("sans-serif", Typeface.NORMAL);
                bold = Typeface.create("sans-serif-medium", Typeface.NORMAL);
                heavy = bold;
                break;
            default: // rounded, sans
                regular = Typeface.create("sans-serif", Typeface.NORMAL);
                medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
                bold = Typeface.create("sans-serif", Typeface.BOLD);
                heavy = Typeface.create("sans-serif-black", Typeface.NORMAL);
        }
    }

    public int dp(float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    /** Corner radius for cards and tiles, in pixels. */
    public float radius() {
        return dp(d.radius);
    }

    public float buttonRadius() {
        switch (d.buttons) {
            case "pill":
                return dp(99);
            case "square":
                return dp(Math.min(d.radius, 2));
            default:
                return dp(Math.min(d.radius, 16));
        }
    }

    /** Headings, buttons, labels and product names follow the design's capitals rule. */
    public String t(String s) {
        return caps && s != null ? s.toUpperCase(Locale.getDefault()) : s;
    }

    public boolean is(String productsStyle) {
        return d.products.equals(productsStyle);
    }
}
