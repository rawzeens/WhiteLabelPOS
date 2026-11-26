package com.r4xn.whitelabelpos;

import android.app.Application;

public class App extends Application {
    private static App app;
    private Store store;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        store = new Store(this);
    }

    public static Store store() {
        return app.store;
    }
}
