package io.uuddlrlrba.memories;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.cameraview.CameraView;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.uuddlrlrba.memories.glide.DriveIdModelLoader;

public class MainActivity extends GoogleDriveActivity {

    private ListView mListView;
    private CameraView mCameraView;
    private MemoriesAdapter mResultsAdapter;

    private Handler mBackgroundHandler;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_memories:
                    if (mCameraView.getVisibility() == View.VISIBLE) {
                        mCameraView.setVisibility(View.GONE);
                        mCameraView.stop();
                    }
                    mListView.setVisibility(View.VISIBLE);
                    return true;
                case R.id.navigation_camera:
                    mCameraView.setVisibility(View.VISIBLE);
                    mListView.setVisibility(View.GONE);
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

        mListView = (ListView) findViewById(R.id.list_view);
        mCameraView = (CameraView) findViewById(R.id.camera_view);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        mCameraView.addCallback(mCallback);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraView.takePicture();
            }
        });

        mResultsAdapter = new MemoriesAdapter(this);
        mListView.setAdapter(mResultsAdapter);
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
                    } else {
                        showMessage("Captured a new memory");
                        mResultsAdapter.notifyDataSetChanged();
                    }
                }
            };

    private void showMessage(String string) {
        Toast.makeText(this, string, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        super.onConnected(connectionHint);
        Drive.DriveApi.getAppFolder(getGoogleApiClient()).listChildren(getGoogleApiClient())
                .setResultCallback(metadataResult);

        Glide.get(this)
                .register(DriveId.class, InputStream.class,
                        new DriveIdModelLoader.Factory(getGoogleApiClient()));
    }

    final private ResultCallback<DriveApi.MetadataBufferResult> metadataResult = new
            ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(@NonNull DriveApi.MetadataBufferResult result) {
                    if (!result.getStatus().isSuccess()) {
                        showMessage("Problem while retrieving files");
                        return;
                    }
                    mResultsAdapter.clear();
                    mResultsAdapter.append(result.getMetadataBuffer());
                    showMessage("Successfully listed files.");
                }
            };

}
