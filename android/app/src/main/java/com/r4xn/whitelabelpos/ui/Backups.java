package com.r4xn.whitelabelpos.ui;

import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.r4xn.whitelabelpos.App;
import com.r4xn.whitelabelpos.Store;
import com.r4xn.whitelabelpos.core.Doc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/** Saving and restoring the backup file (the same file works on the iPhone app). */
final class Backups {
    private Backups() {
    }

    /** File pickers have to be set up while a screen is being created. */
    static final class Launchers {
        final ActivityResultLauncher<String> save;
        final ActivityResultLauncher<String[]> open;

        Launchers(BaseActivity a, Runnable afterRestore) {
            save = a.registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri != null) write(a, uri);
            });
            open = a.registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) read(a, uri, afterRestore);
            });
        }
    }

    static String fileName(Store s) {
        String name = s.doc.brand.name.isEmpty() ? "Shop" : s.doc.brand.name.replaceAll("[^A-Za-z0-9 &-]", "").trim();
        return name + " backup " + new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date()) + ".json";
    }

    static void backup(MainActivity a, Look l) {
        Store s = a.store;
        Kit.choose(a, l, "Back up", Arrays.asList("Send a copy (WhatsApp, email, Drive…)", "Save to a folder on this device"), i -> {
            if (i == 0) send(a, s);
            else a.launchers.save.launch(fileName(s));
        });
    }

    private static void send(BaseActivity a, Store s) {
        try {
            File dir = new File(a.getCacheDir(), "shared");
            dir.mkdirs();
            File f = new File(dir, fileName(s));
            try (FileOutputStream out = new FileOutputStream(f)) {
                out.write(s.doc.toJson().toString(1).getBytes(StandardCharsets.UTF_8));
            }
            Uri uri = FileProvider.getUriForFile(a, a.getPackageName() + ".files", f);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("application/json");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.putExtra(Intent.EXTRA_SUBJECT, f.getName());
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            a.startActivity(Intent.createChooser(send, "Back up"));
        } catch (Exception e) {
            Kit.toast(a, "Couldn't make the backup");
        }
    }

    private static void write(AppCompatActivity a, Uri uri) {
        try (OutputStream out = a.getContentResolver().openOutputStream(uri)) {
            out.write(App.store().doc.toJson().toString(1).getBytes(StandardCharsets.UTF_8));
            Kit.toast(a, "Backup saved");
        } catch (Exception e) {
            Kit.toast(a, "Couldn't save the backup");
        }
    }

    static void restore(Launchers ls) {
        ls.open.launch(new String[]{"application/json", "text/plain", "application/octet-stream", "*/*"});
    }

    private static void read(BaseActivity a, Uri uri, Runnable after) {
        Doc d;
        try (InputStream in = a.getContentResolver().openInputStream(uri)) {
            d = Doc.fromJson(Store.read(in));
        } catch (Exception e) {
            Kit.toast(a, "That file isn't a backup from this app");
            return;
        }
        String who = d.brand.name.isEmpty() ? "this backup" : d.brand.name;
        Kit.confirm(a, a.look, "Restore " + who + "?",
                d.products.size() + " products and " + d.sales.size() + " sales replace everything on this device.",
                "Restore", true, () -> {
                    App.store().replace(d);
                    App.store().unlocked = true;
                    Kit.toast(a, "Restored");
                    after.run();
                });
    }
}
