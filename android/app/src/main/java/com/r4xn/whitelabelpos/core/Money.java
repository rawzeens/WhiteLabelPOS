package com.r4xn.whitelabelpos.core;

import java.util.ArrayList;
import java.util.List;

/** Money is always a whole number in the smallest unit (sen, cents). See whitelabel/SPEC.md section 3. */
public final class Money {
    private Money() {
    }

    /** Round-half-up of a / b for a >= 0, b > 0. */
    public static long roundDiv(long a, long b) {
        return (2 * a + b) / (2 * b);
    }

    public static long unit(int decimals) {
        long u = 1;
        for (int i = 0; i < decimals; i++) u *= 10;
        return u;
    }

    public static String format(long amount, String symbol, int decimals) {
        long abs = Math.abs(amount);
        long unit = unit(decimals);
        String whole = Long.toString(abs / unit);
        StringBuilder grouped = new StringBuilder();
        for (int i = 0; i < whole.length(); i++) {
            if (i > 0 && (whole.length() - i) % 3 == 0) grouped.append(',');
            grouped.append(whole.charAt(i));
        }
        if (decimals > 0) {
            String frac = Long.toString(abs % unit);
            grouped.append('.');
            for (int i = frac.length(); i < decimals; i++) grouped.append('0');
            grouped.append(frac);
        }
        String sym = symbol == null ? "" : symbol;
        boolean space = !sym.isEmpty() && Character.isLetter(sym.charAt(sym.length() - 1));
        return (amount < 0 ? "-" : "") + sym + (space ? " " : "") + grouped;
    }

    public static String format(long amount, Currency c) {
        return format(amount, c.symbol, c.decimals);
    }

    /** Plain number for an input box: 12.5 with 2 decimals → "12.50", no symbol and no commas. */
    public static String plain(long amount, int decimals) {
        String s = format(amount, "", decimals);
        return s.replace(",", "");
    }

    /** What the owner typed → smallest unit, or -1 when it is not a price. */
    public static long parse(String text, int decimals) {
        if (text == null) return -1;
        StringBuilder kept = new StringBuilder();
        int dots = 0;
        for (char ch : text.toCharArray()) {
            if (ch >= '0' && ch <= '9') kept.append(ch);
            else if (ch == '.') {
                kept.append(ch);
                dots++;
            }
        }
        if (dots > 1) return -1;
        String s = kept.toString();
        if (s.isEmpty() || s.equals(".")) return -1;
        String whole = s.contains(".") ? s.substring(0, s.indexOf('.')) : s;
        String frac = s.contains(".") ? s.substring(s.indexOf('.') + 1) : "";
        if (whole.length() > 13) return -1;
        long unit = unit(decimals);
        long value = (whole.isEmpty() ? 0 : Long.parseLong(whole)) * unit;
        if (decimals > 0) {
            StringBuilder f = new StringBuilder(frac.length() > decimals ? frac.substring(0, decimals) : frac);
            while (f.length() < decimals) f.append('0');
            value += Long.parseLong(f.toString());
        }
        if (frac.length() > decimals && frac.charAt(decimals) >= '5') value += 1;
        return value;
    }

    /** Quick "customer handed over" amounts for a cash payment (SPEC section 3). */
    public static List<Long> quickCash(long due, int decimals) {
        List<Long> out = new ArrayList<>();
        if (due <= 0) return out;
        long unit = decimals == 0 ? 1000 : unit(decimals);
        long[] steps = {1, 5, 10, 20, 50, 100};
        for (long s : steps) {
            long step = s * unit;
            long v = ((due + step - 1) / step) * step;
            if (v != due && !out.contains(v)) out.add(v);
            if (out.size() == 4) break;
        }
        return out;
    }

    /** The cash a demo customer pays: next multiple of 1000 that is ≥ due. */
    public static long nextThousand(long due) {
        return ((due + 999) / 1000) * 1000;
    }
}
