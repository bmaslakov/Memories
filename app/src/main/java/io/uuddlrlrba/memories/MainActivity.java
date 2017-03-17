package io.uuddlrlrba.memories;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.cameraview.CameraView;

public class MainActivity extends GoogleDriveActivity {

    private TextView mTextMessage;
    private CameraView mCameraView;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_memories:
                    mTextMessage.setText(R.string.title_memories);
                    if (mCameraView.getVisibility() == View.VISIBLE) {
                        mCameraView.setVisibility(View.GONE);
                        mCameraView.stop();
                    }
                    return true;
                case R.id.navigation_camera:
                    mCameraView.setVisibility(View.VISIBLE);
                    mCameraView.start();
                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextMessage = (TextView) findViewById(R.id.message);
        mCameraView = (CameraView) findViewById(R.id.camera_view);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCameraView.getVisibility() == View.VISIBLE) {
            mCameraView.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraView.getVisibility() == View.VISIBLE) {
            mCameraView.stop();
        }
    }
}
