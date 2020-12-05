package ru.nntu.lprclient;

import android.app.Application;
import android.content.Context;

/**
 * Application class with {@link Context} getter.
 */
public class LprApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        LprApplication.context = getApplicationContext();
    }

    /**
     * Returns application context.
     *
     * @return current {@link Context} object
     */
    public static Context getAppContext() {
        return LprApplication.context;
    }
}
