package io.uuddlrlrba.memories;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

public class WelcomeActivity extends GoogleDriveActivity {
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        mSharedPreferences = getSharedPreferences("io.uuddlrlrba.memories", MODE_PRIVATE);

        if (mSharedPreferences.getBoolean("registered", false)) {
            navigateToMainActivity();
        } else {
            findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getGoogleApiClient().connect();
                }
            });
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        super.onConnected(connectionHint);
        navigateToMainActivity();
        mSharedPreferences
                .edit()
                .putBoolean("registered", true)
                .apply();
        getGoogleApiClient().disconnect();
    }

    private void navigateToMainActivity() {
        startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
        finish();
    }
}
