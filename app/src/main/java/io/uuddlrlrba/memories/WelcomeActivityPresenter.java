package io.uuddlrlrba.memories;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import io.uuddlrlrba.memories.core.CommandCallback;
import io.uuddlrlrba.memories.core.GoogleDriveMemories;
import io.uuddlrlrba.memories.core.Memories;

public class WelcomeActivityPresenter {
    private SharedPreferences mSharedPreferences;
    private Memories mMemories;
    private View mView;

    public WelcomeActivityPresenter(Activity activity, View view) {
        mSharedPreferences = activity.getSharedPreferences("io.uuddlrlrba.memories",
                Context.MODE_PRIVATE);
        mView = view;

        if (mSharedPreferences.getBoolean("registered", false)) {
            view.navigateToMainActivity();
        } else {
            mMemories = new GoogleDriveMemories(activity);
        }
    }

    public void connect() {
        mMemories.connect(new CommandCallback<Void>() {
            @Override
            public void onResult(Void result) {
                mView.navigateToMainActivity();
                mSharedPreferences
                        .edit()
                        .putBoolean("registered", true)
                        .apply();
                mMemories.disconnect();
            }

            @Override
            public void onError(Exception e) {

            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GoogleDriveMemories.REQUEST_CODE_RESOLUTION && resultCode == Activity.RESULT_OK) {
            connect();
        }
    }

    public interface View {
        void navigateToMainActivity();
    }
}
