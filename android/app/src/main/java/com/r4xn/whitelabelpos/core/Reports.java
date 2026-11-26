package com.r4xn.whitelabelpos.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Sales figures for a time range (SPEC section 4). */
public final class Reports {
    public static final class Top {
        public final String name;
        public final int qty;
        public final long amount;

        Top(String name, int qty, long amount) {
            this.name = name;
            this.qty = qty;
            this.amount = amount;
        }
    }

    public static final class Bar {
        public final String label;
        public final long amount;

        Bar(String label, long amount) {
            this.label = label;
            this.amount = amount;
        }
    }

    public static final class Report {
        public int sales;
        public long gross;
        public long average;
        public long tax;
        public long discount;
        public int refunds;
        public long refunded;
        public final Map<String, Long> byMethod = new LinkedHashMap<>();
        public final List<Top> top = new ArrayList<>();
    }

    private Reports() {
    }

    public static Report report(List<Doc.Sale> sales, long from, long to) {
        Report r = new Report();
        Map<String, long[]> top = new LinkedHashMap<>();
        for (Doc.Sale s : sales) {
            if (s.time < from || s.time >= to) continue;
            if (s.refunded) {
                r.refunds++;
                r.refunded += s.due;
                continue;
            }
            r.sales++;
            r.gross += s.due;
            r.tax += s.tax;
            r.discount += s.discount;
            Long m = r.byMethod.get(s.method);
            r.byMethod.put(s.method, (m == null ? 0 : m) + s.due);
            for (Doc.Line l : s.lines) {
                long[] t = top.get(l.name);
                if (t == null) top.put(l.name, t = new long[2]);
                t[0] += l.qty;
                t[1] += l.amount();
            }
        }
        r.average = r.sales == 0 ? 0 : Money.roundDiv(r.gross, r.sales);
        for (Map.Entry<String, long[]> e : top.entrySet()) {
            r.top.add(new Top(e.getKey(), (int) e.getValue()[0], e.getValue()[1]));
        }
        r.top.sort((a, b) -> {
            if (a.qty != b.qty) return Integer.compare(b.qty, a.qty);
            if (a.amount != b.amount) return Long.compare(b.amount, a.amount);
            return a.name.compareTo(b.name);
        });
        return r;
    }

    /** Takings per hour from 6:00 to 23:00 on the day that starts at dayStart. */
    public static List<Bar> hourly(List<Doc.Sale> sales, long dayStart, ZoneId zone) {
        ZonedDateTime start = Instant.ofEpochMilli(dayStart).atZone(zone);
        List<Bar> out = new ArrayList<>();
        for (int h = 6; h <= 23; h++) {
            long from = start.withHour(h).toInstant().toEpochMilli();
            long to = h == 23 ? start.plusDays(1).toInstant().toEpochMilli() : start.withHour(h + 1).toInstant().toEpochMilli();
            out.add(new Bar(h + "", sum(sales, from, to)));
        }
        return out;
    }

    /** Takings per local day for the given number of days ending today. */
    public static List<Bar> daily(List<Doc.Sale> sales, LocalDate today, int days, ZoneId zone) {
        List<Bar> out = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            long from = d.atStartOfDay(zone).toInstant().toEpochMilli();
            long to = d.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
            out.add(new Bar(Integer.toString(d.getDayOfMonth()), sum(sales, from, to)));
        }
        return out;
    }

    private static long sum(List<Doc.Sale> sales, long from, long to) {
        long t = 0;
        for (Doc.Sale s : sales) if (!s.refunded && s.time >= from && s.time < to) t += s.due;
        return t;
    }
}
