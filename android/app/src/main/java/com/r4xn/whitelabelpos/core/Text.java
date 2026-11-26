package com.r4xn.whitelabelpos.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Small text rules shared with the iPhone app. */
public final class Text {
    private Text() {
    }

    /** Brand / product initials (SPEC section 5): "Kopi Pagi" → "KP", "Lily & Co" → "LC". */
    public static String initials(String name) {
        List<String> words = new ArrayList<>();
        if (name != null) {
            for (String w : name.trim().split("\\s+")) {
                if (!w.isEmpty() && Character.isLetterOrDigit(w.codePointAt(0))) words.add(w);
            }
        }
        if (words.isEmpty()) return "?";
        String first = firstChar(words.get(0));
        if (words.size() == 1) return first.toUpperCase(Locale.ROOT);
        return (first + firstChar(words.get(words.size() - 1))).toUpperCase(Locale.ROOT);
    }

    private static String firstChar(String w) {
        return new String(Character.toChars(w.codePointAt(0)));
    }

    /** "6%" / "6.5%" from basis points. */
    public static String percent(int basisPoints) {
        if (basisPoints % 100 == 0) return (basisPoints / 100) + "%";
        String s = String.format(Locale.ROOT, "%.2f", basisPoints / 100.0);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        return s + "%";
    }

    /** "6.5" typed → 650 basis points, or -1. */
    public static int parsePercent(String text) {
        long v = Money.parse(text, 2);
        if (v < 0 || v > 10000) return -1;
        return (int) v;
    }

    public static String methodName(String method) {
        switch (method) {
            case Doc.CARD:
                return "Card";
            case Doc.QR:
                return "QR pay";
            case Doc.EWALLET:
                return "E-wallet";
            default:
                return "Cash";
        }
    }

    public static String items(int n) {
        return n == 1 ? "1 item" : n + " items";
    }

    public static String taxLine(String name, int rate, boolean inclusive) {
        String n = name == null || name.isBlank() ? "Tax" : name.trim();
        return inclusive ? "Includes " + n + " " + percent(rate) : n + " " + percent(rate);
    }
}
