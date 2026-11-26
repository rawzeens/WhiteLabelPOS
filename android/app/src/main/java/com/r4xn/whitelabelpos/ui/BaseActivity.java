package com.r4xn.whitelabelpos.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.r4xn.whitelabelpos.App;
import com.r4xn.whitelabelpos.R;
import com.r4xn.whitelabelpos.Store;

/** Every screen: draws edge to edge, keeps clear of the status bar, navigation bar and keyboard. */
public abstract class BaseActivity extends AppCompatActivity {
    protected Store store;
    protected Look look;
    private View topSpacer;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        store = App.store();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        look = newLook();
    }

    protected Look newLook() {
        return new Look(this, store.design(), store.doc.brand);
    }

    /** Shows content under a strip the height of the status bar, painted in topColor. */
    protected void setScreen(View content, int topColor) {
        LinearLayout root = Kit.vbox(this);
        root.setBackgroundColor(look.page);
        topSpacer = new View(this);
        root.addView(topSpacer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
        root.addView(content, Kit.fill());
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            topSpacer.getLayoutParams().height = bars.top;
            topSpacer.requestLayout();
            v.setPadding(bars.left, 0, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
        setContentView(root);
        paintTop(topColor);
    }

    protected void paintTop(int topColor) {
        if (topSpacer != null) topSpacer.setBackgroundColor(topColor);
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        c.setAppearanceLightStatusBars(!Styles.lightIcons(topColor));
        c.setAppearanceLightNavigationBars(!look.d.dark);
    }

    /** Back arrow + title, the design's way. Extra views go on the right. */
    protected LinearLayout topBar(String title, View... right) {
        LinearLayout bar = Kit.hbox(this);
        bar.setPadding(look.dp(6), look.dp(6), look.dp(12), look.dp(6));
        bar.addView(Kit.iconButton(look, R.drawable.ic_back, look.ink, 0, this::finish));
        TextView t = Kit.heading(look, title);
        t.setTextSize(19);
        t.setPadding(look.dp(4), 0, 0, 0);
        t.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(t, Kit.weight(1));
        for (View v : right) bar.addView(v);
        return bar;
    }
}
