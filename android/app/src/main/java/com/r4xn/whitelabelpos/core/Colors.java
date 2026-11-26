package com.r4xn.whitelabelpos.core;

/** Colour maths shared with the iPhone app (SPEC section 5). Colours are ARGB ints. */
public final class Colors {
    public static final int WHITE = 0xFFFFFFFF;
    public static final int NEAR_BLACK = 0xFF111111;

    private Colors() {
    }

    /** "#RRGGBB" (or "RRGGBB") → opaque ARGB, or the fallback. */
    public static int parse(String hex, int fallback) {
        if (hex == null) return fallback;
        String h = hex.trim();
        if (h.startsWith("#")) h = h.substring(1);
        if (h.length() != 6) return fallback;
        try {
            return 0xFF000000 | Integer.parseInt(h, 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static boolean isHex(String hex) {
        return parse(hex, 1) != 1;
    }

    public static String hex(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }

    private static double channel(int v) {
        double c = v / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    public static double luminance(int color) {
        return 0.2126 * channel((color >> 16) & 0xFF) + 0.7152 * channel((color >> 8) & 0xFF) + 0.0722 * channel(color & 0xFF);
    }

    /** Text colour that reads well on top of this colour. */
    public static int on(int color) {
        double l = luminance(color);
        return 1.05 / (l + 0.05) >= (l + 0.05) / 0.05 ? WHITE : NEAR_BLACK;
    }

    /** a over b with the given amount of a (0..1). */
    public static int mix(int a, int b, double amount) {
        int r = (int) Math.round(((a >> 16) & 0xFF) * amount + ((b >> 16) & 0xFF) * (1 - amount));
        int g = (int) Math.round(((a >> 8) & 0xFF) * amount + ((b >> 8) & 0xFF) * (1 - amount));
        int bl = (int) Math.round((a & 0xFF) * amount + (b & 0xFF) * (1 - amount));
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /** The colour at 14 % over the page (22 % on dark designs). */
    public static int soft(int color, int page, boolean dark) {
        return mix(color, page, dark ? 0.22 : 0.14);
    }

    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0xFFFFFF);
    }
}
