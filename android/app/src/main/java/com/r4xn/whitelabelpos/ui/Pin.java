package com.r4xn.whitelabelpos.ui;

import android.app.Activity;
import android.app.Dialog;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.r4xn.whitelabelpos.App;
import com.r4xn.whitelabelpos.Store;

/** The owner PIN: asking for it, and setting / changing / removing it. */
final class Pin {
    private Pin() {
    }

    static void ask(Activity a, Look l, String title, Runnable onRight) {
        Store s = App.store();
        EditText e = Kit.pinField(l, "• • • •");
        Dialog[] d = new Dialog[1];
        d[0] = Kit.sheet(a, l, title, e, "Unlock", () -> {
            if (e.getText().toString().equals(s.doc.pin)) {
                s.unlocked = true;
                d[0].dismiss();
                onRight.run();
            } else {
                e.setText("");
                e.setError("Wrong PIN");
            }
        }, false);
        Kit.onText(e, text -> {
            if (text.length() == 4 && text.equals(s.doc.pin)) {
                s.unlocked = true;
                d[0].dismiss();
                onRight.run();
            }
        });
        e.requestFocus();
    }

    /** Two boxes; both empty removes the PIN. onSet gets the new PIN ("" = none). */
    static void set(Activity a, Look l, java.util.function.Consumer<String> onSet) {
        LinearLayout box = Kit.vbox(a);
        box.addView(Kit.muted(l, "4 numbers. Asked before Products, Settings and refunds. Leave both empty for no PIN."));
        EditText one = Kit.pinField(l, "New PIN");
        EditText two = Kit.pinField(l, "Type it again");
        box.addView(Kit.gap(l, 12));
        box.addView(one, Kit.match());
        box.addView(Kit.gap(l, 10));
        box.addView(two, Kit.match());
        Dialog[] d = new Dialog[1];
        d[0] = Kit.sheet(a, l, "Owner PIN", box, "Save", () -> {
            String p1 = one.getText().toString(), p2 = two.getText().toString();
            if (p1.isEmpty() && p2.isEmpty()) {
                d[0].dismiss();
                onSet.accept("");
                return;
            }
            if (p1.length() != 4) {
                one.setError("Use 4 numbers");
                return;
            }
            if (!p1.equals(p2)) {
                two.setError("The two PINs are different");
                return;
            }
            d[0].dismiss();
            onSet.accept(p1);
        }, false);
        one.requestFocus();
    }
}
