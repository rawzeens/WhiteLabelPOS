package com.r4xn.whitelabelpos.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.r4xn.whitelabelpos.R;

/** The till: Sell · Orders · Products · Reports · Settings. */
public class MainActivity extends BaseActivity {
    static final int SELL = 0, ORDERS = 1, PRODUCTS = 2, REPORTS = 3, SETTINGS = 4;
    private static final String[] NAMES = {"Sell", "Orders", "Products", "Reports", "Settings"};
    private static final int[] ICONS = {R.drawable.ic_sell, R.drawable.ic_orders, R.drawable.ic_products, R.drawable.ic_reports, R.drawable.ic_settings};

    int tab = SELL;
    private FrameLayout content;
    private LinearLayout tabBar;
    private SellTab sellTab;
    // Remembered while switching tabs.
    String sellQuery = "", sellCategory = "", ordersQuery = "";
    int reportDays = 1;
    Backups.Launchers launchers;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        launchers = new Backups.Launchers(this, () -> {
            tab = SELL;
            build();
        });
        if (saved != null) tab = saved.getInt("tab", SELL);
        Debug.apply(this, getIntent());
        // onResume builds the screen, or sends a new shop to Setup.
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra("sell", false)) tab = SELL;
        Debug.apply(this, intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("tab", tab);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!store.doc.setupDone) {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }
        build();
    }

    /** Rebuilds the whole screen (the design or brand may have changed in Settings). */
    void build() {
        look = newLook();
        LinearLayout root = Kit.vbox(this);
        content = new FrameLayout(this);
        root.addView(content, Kit.fill());
        tabBar = Kit.hbox(this);
        root.addView(tabBar, Kit.match());
        setScreen(root, Styles.statusColor(look, tab == SELL));
        showTab();
    }

    void open(int which) {
        if ((which == PRODUCTS || which == SETTINGS) && store.locked()) {
            Pin.ask(this, look, "Owner PIN", () -> open(which));
            return;
        }
        tab = which;
        showTab();
    }

    private void showTab() {
        if ((tab == PRODUCTS || tab == SETTINGS) && store.locked()) tab = SELL;
        content.removeAllViews();
        sellTab = null;
        View v;
        switch (tab) {
            case ORDERS:
                v = OrdersTab.build(this);
                break;
            case PRODUCTS:
                v = ProductsTab.build(this);
                break;
            case REPORTS:
                v = ReportsTab.build(this);
                break;
            case SETTINGS:
                v = SettingsTab.build(this);
                break;
            default:
                sellTab = new SellTab(this);
                v = sellTab.view;
        }
        content.addView(v, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        paintTop(Styles.statusColor(look, tab == SELL));
        paintTabs();
    }

    private void paintTabs() {
        tabBar.removeAllViews();
        boolean street = look.is("blocks");
        int bg = street ? look.ink : look.surface;
        int on = street ? look.page : Kit.linkColor(look);
        int off = street ? 0xFF9A9A9A : look.muted;
        LinearLayout wrap = tabBar;
        wrap.setBackground(Kit.outline(bg, street ? look.ink : look.line, Math.max(1, look.dp(1) / 2), 0));
        for (int i = 0; i < NAMES.length; i++) {
            int index = i;
            boolean sel = i == tab;
            LinearLayout t = Kit.vbox(this);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, look.dp(8), 0, look.dp(8));
            ImageView icon = Kit.icon(look, ICONS[i], sel ? (street ? look.primary : on) : off, 24);
            t.addView(icon);
            TextView label = Kit.text(look, look.t(NAMES[i]), look.caps ? 10 : 11, sel ? (street ? look.page : on) : off, sel ? look.bold : look.medium);
            label.setLetterSpacing(look.tracking);
            label.setPadding(0, look.dp(3), 0, 0);
            t.addView(label);
            t.setOnClickListener(v -> open(index));
            tabBar.addView(t, Kit.weight(1));
        }
    }

    /** Called by Cart/Checkout screens when they change the sale. */
    void cartChanged() {
        if (sellTab != null) sellTab.refreshCart();
    }
}
