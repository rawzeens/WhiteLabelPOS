package com.r4xn.whitelabelpos.core;

/** Subtotal → discount → tax → cash rounding → amount due. SPEC section 3. */
public final class Totals {
    public final long subtotal;
    public final long discount;
    public final long tax;
    public final long total;
    public final long rounding;
    public final long due;

    private Totals(long subtotal, long discount, long tax, long total, long rounding) {
        this.subtotal = subtotal;
        this.discount = discount;
        this.tax = tax;
        this.total = total;
        this.rounding = rounding;
        this.due = total + rounding;
    }

    public static Totals compute(long subtotal, String discountType, long discountValue,
                                 int taxRate, boolean taxInclusive, int cashRoundTo, String method) {
        long sub = Math.max(0, subtotal);
        long discount;
        if ("amount".equals(discountType)) {
            discount = Math.max(0, Math.min(discountValue, sub));
        } else if ("percent".equals(discountType)) {
            long pct = Math.max(0, Math.min(discountValue, 10000));
            discount = Math.min(sub, Money.roundDiv(sub * pct, 10000));
        } else {
            discount = 0;
        }
        long net = sub - discount;
        long tax;
        long total;
        if (taxRate <= 0) {
            tax = 0;
            total = net;
        } else if (taxInclusive) {
            tax = net - Money.roundDiv(net * 10000, 10000 + taxRate);
            total = net;
        } else {
            tax = Money.roundDiv(net * taxRate, 10000);
            total = net + tax;
        }
        long rounding = 0;
        if (Doc.CASH.equals(method) && cashRoundTo > 0) {
            long step = cashRoundTo;
            rounding = ((total + step / 2) / step) * step - total;
        }
        return new Totals(sub, discount, tax, total, rounding);
    }

    public static long change(String method, long paid, long due) {
        return Doc.CASH.equals(method) ? Math.max(0, paid - due) : 0;
    }
}
