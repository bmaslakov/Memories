package io.uuddlrlrba.memories;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.cameraview.CameraView;

public class MainActivity extends AppCompatActivity implements
        MainActivityPresenter.View {

    private final static int PERMISSIONS_REQUEST_CAMERA = 0x123;

    private ListView mListView;
    private CameraView mCameraView;
    private TextView mTextViewStatus;
    private ProgressDialog mProgressDialog;

    private Handler mBackgroundHandler;

    private MainActivityPresenter mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = (ListView) findViewById(R.id.list_view);
        mCameraView = (CameraView) findViewById(R.id.camera_view);
        mCameraView.setSoundEffectsEnabled(true);
        mTextViewStatus = (TextView) findViewById(R.id.text_view_status);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.navigation_memories:
                                showList();
                                return true;
                            case R.id.navigation_camera:
                                showCamera();
                                return true;
                        }
                        return false;
                    }
                });

        mCameraView.addCallback(mCallback);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgressDialog(getString(R.string.preparing_photo));
                mCameraView.takePicture();
            }
        });

        HandlerThread thread = new HandlerThread("background");
        thread.start();
        mBackgroundHandler = new Handler(thread.getLooper());

        mPresenter = new MainActivityPresenter(this, mBackgroundHandler, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCameraView.getVisibility() == View.VISIBLE) {
            mCameraView.start();
        }
        mPresenter.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraView.getVisibility() == View.VISIBLE) {
            mCameraView.stop();
        }
        mPresenter.disconnect();
    }

    private CameraView.Callback mCallback = new CameraView.Callback() {
        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            Bitmap original = BitmapFactory.decodeByteArray(data , 0, data.length);
            mPresenter.upload(original);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mTextViewStatus.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // looks like a bug google's CameraView
                            showCamera();
                        }
                    }, 100);
                }
            }
        }
    }

    private void showList() {
        if (mCameraView.getVisibility() == View.VISIBLE) {
            mCameraView.setVisibility(View.GONE);
            mCameraView.stop();
        }
        if (mListView.getAdapter() == null || mListView.getAdapter().getCount() == 0) {
            mTextViewStatus.setText(R.string.no_memories);
            mTextViewStatus.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.GONE);
        } else {
            mTextViewStatus.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);
        }
    }

    private void showCamera() {
        mListView.setVisibility(View.GONE);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.CAMERA },
                    PERMISSIONS_REQUEST_CAMERA);

            mTextViewStatus.setVisibility(View.VISIBLE);
            mTextViewStatus.setText(R.string.camera_permission);
        } else {
            mTextViewStatus.setVisibility(View.GONE);
            mCameraView.setVisibility(View.VISIBLE);
            mCameraView.start();
        }
    }

    @Override
    public void showMessage(String string) {
        Toast.makeText(this, string, Toast.LENGTH_LONG).show();
    }

    @Override
    public void showProgressDialog(String message) {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(MainActivity.this, "",
                    message, true, false);
        }
    }

    @Override
    public void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @Override
    public void setAdapter(BaseAdapter adapter) {
        mListView.setAdapter(adapter);
        if (adapter.getCount() == 0) {
            mTextViewStatus.setText(R.string.no_memories);
            mTextViewStatus.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.GONE);
        } else {
            mTextViewStatus.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);
        }
    }
}
