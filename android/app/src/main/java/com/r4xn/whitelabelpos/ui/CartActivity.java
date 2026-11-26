package com.r4xn.whitelabelpos.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

/** The sale being rung up, full screen (phones). */
public class CartActivity extends BaseActivity {
    @Override
    protected void onResume() {
        super.onResume();
        if (store.cart.isEmpty() && isTaskRoot()) {
            finish();
            return;
        }
        build();
    }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
    }

    private void build() {
        LinearLayout v = Kit.vbox(this);
        v.addView(topBar("This sale"), Kit.match());
        CartPanel panel = new CartPanel(this, look, store, false, new CartPanel.Listener() {
            @Override
            public void changed() {
                if (store.cart.isEmpty()) finish();
            }

            @Override
            public void charge() {
                startActivity(new Intent(CartActivity.this, CheckoutActivity.class));
            }
        });
        v.addView(panel.view, Kit.fill());
        setScreen(v, look.page);
    }
}
