package com.r4xn.whitelabelpos.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import com.r4xn.whitelabelpos.core.Reports;

import java.util.List;

/** Simple bars for takings per hour or per day. */
final class BarChart extends View {
    private final Look l;
    private final List<Reports.Bar> bars;
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int every;

    BarChart(Look l, List<Reports.Bar> bars, int labelEvery) {
        super(l.c);
        this.l = l;
        this.bars = bars;
        this.every = Math.max(1, labelEvery);
        bar.setColor(l.primary);
        label.setColor(l.muted);
        label.setTextSize(l.dp(10));
        label.setTypeface(l.regular);
        label.setTextAlign(Paint.Align.CENTER);
        base.setColor(l.line);
        base.setStrokeWidth(Math.max(1, l.dp(1)));
        setMinimumHeight(l.dp(150));
    }

    @Override
    protected void onDraw(Canvas c) {
        if (bars.isEmpty()) return;
        long max = 1;
        for (Reports.Bar b : bars) max = Math.max(max, b.amount);
        float labelH = l.dp(16);
        float h = getHeight() - labelH - l.dp(4);
        float slot = (float) getWidth() / bars.size();
        float w = Math.max(l.dp(2), slot * 0.62f);
        float r = l.d.radius == 0 ? 0 : Math.min(w / 2, l.dp(4));
        c.drawLine(0, h, getWidth(), h, base);
        for (int i = 0; i < bars.size(); i++) {
            Reports.Bar b = bars.get(i);
            float x = slot * i + (slot - w) / 2;
            float bh = b.amount == 0 ? 0 : Math.max(l.dp(2), h * b.amount / max);
            if (bh > 0) c.drawRoundRect(new RectF(x, h - bh, x + w, h), r, r, bar);
            if (i % every == 0 || i == bars.size() - 1) c.drawText(b.label, x + w / 2, getHeight() - l.dp(3), label);
        }
    }
}
