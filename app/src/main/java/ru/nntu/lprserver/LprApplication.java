package ru.nntu.lprserver;

import android.app.Application;
import android.content.Context;

public class LprApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        LprApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return LprApplication.context;
    }
}
