package io.uuddlrlrba.memories;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
    /**
     * Getter for the {@code SharedPreferences}.
     */
    public SharedPreferences getSharedPreferences() {
        return ((Application) getApplication()).getSharedPreferences();
    }
}
