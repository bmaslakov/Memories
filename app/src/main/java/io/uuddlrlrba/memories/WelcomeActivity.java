package io.uuddlrlrba.memories;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class WelcomeActivity extends GoogleDriveActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        if (getSharedPreferences().getBoolean("registered", false)) {
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
        getSharedPreferences()
                .edit()
                .putBoolean("registered", true)
                .apply();
    }

    private void navigateToMainActivity() {
        startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
        finish();
    }
}
