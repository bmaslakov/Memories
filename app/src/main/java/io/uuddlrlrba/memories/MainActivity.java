package io.uuddlrlrba.memories;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.cameraview.CameraView;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends GoogleDriveActivity {

    private TextView mTextMessage;
    private CameraView mCameraView;
    private Button mButton;

    private Handler mBackgroundHandler;

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
        mButton = (Button) findViewById(R.id.button);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        mCameraView.addCallback(mCallback);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraView.takePicture();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCameraView.getVisibility() == View.VISIBLE) {
            mCameraView.start();
        }
        getGoogleApiClient().connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraView.getVisibility() == View.VISIBLE) {
            mCameraView.stop();
        }
        getGoogleApiClient().disconnect();
    }

    private CameraView.Callback mCallback = new CameraView.Callback() {
        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {

            Drive.DriveApi.newDriveContents(getGoogleApiClient())
                    .setResultCallback(new PhotoUploader(data));
        }
    };

    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }

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

    private final class PhotoUploader implements ResultCallback<DriveApi.DriveContentsResult> {
        private final byte[] data;

        private PhotoUploader(byte[] data) {
            this.data = data;
        }

        @Override
        public void onResult(@NonNull DriveApi.DriveContentsResult result) {
            if (!result.getStatus().isSuccess()) {
                showMessage("Error while trying to create new file contents");
                return;
            }

            final DriveContents driveContents = result.getDriveContents();

            // Perform I/O off the UI thread.
            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {
                    // write content to DriveContents
                    OutputStream os = driveContents.getOutputStream();
                    try {
                        os.write(data);
                        os.close();
                    } catch (IOException e) {
                        showMessage(e.getMessage());
                    }

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle(String.valueOf(System.currentTimeMillis()))
                            .setMimeType("image/jpeg")
                            .build();

                    // create a file on root folder
                    Drive.DriveApi.getAppFolder(getGoogleApiClient())
                            .createFile(getGoogleApiClient(), changeSet, driveContents)
                            .setResultCallback(fileCallback);
                }
            });
        }
    }

    final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(@NonNull DriveFolder.DriveFileResult result) {
                    if (!result.getStatus().isSuccess()) {
                        showMessage("Error while trying to create the file");
                        return;
                    }
                    showMessage("Created a file in Google Drive Folder: "
                            + result.getDriveFile().getDriveId());
                }
            };

    private void showMessage(String string) {
        Toast.makeText(this, string, Toast.LENGTH_LONG).show();
    }
}
