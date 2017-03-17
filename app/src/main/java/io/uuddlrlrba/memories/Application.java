package io.uuddlrlrba.memories;

import android.content.SharedPreferences;

public class Application extends android.app.Application {

    /**
     * Google API client.
     */
    private SharedPreferences mSharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();

        if (mSharedPreferences == null) {
            mSharedPreferences = getSharedPreferences("io.uuddlrlrba.memories", MODE_PRIVATE);
        }
    }

    /**
     * Getter for the {@code SharedPreferences}.
     */
    public SharedPreferences getSharedPreferences() {
        return mSharedPreferences;
    }
}
