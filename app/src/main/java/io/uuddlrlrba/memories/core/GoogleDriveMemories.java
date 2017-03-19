package io.uuddlrlrba.memories.core;

import android.app.Activity;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.BaseAdapter;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.uuddlrlrba.memories.MemoriesAdapter;
import io.uuddlrlrba.memories.glide.DriveIdModelLoader;

public class GoogleDriveMemories implements Memories {
    private static final String TAG = "GoogleDriveMemories";

    public static int REQUEST_CODE_RESOLUTION = 1;

    private final Activity mActivity;
    private final Handler mBackgroundHandler;

    private GoogleApiClient mGoogleApiClient;
    private MemoriesAdapter mResultsAdapter;

    public GoogleDriveMemories(Activity activity) {
        this.mActivity = activity;
        this.mBackgroundHandler = null;
    }

    public GoogleDriveMemories(Activity activity, Handler backgroundHandler) {
        this.mActivity = activity;
        this.mBackgroundHandler = backgroundHandler;
    }

    @Override
    public void upload(Bitmap bitmap, CommandCallback<Void> callback) {
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new PhotoUploader(bitmap, callback));
    }

    @Override
    public void connect(final CommandCallback<Void> commandCallback) {
        this.mGoogleApiClient = new GoogleApiClient.Builder(mActivity)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addScope(Drive.SCOPE_APPFOLDER) // required for App Folder sample
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        Log.i(TAG, "GoogleApiClient connected");
                        commandCallback.onResult(null);
                        Glide.get(mActivity)
                                .register(DriveId.class, InputStream.class,
                                        new DriveIdModelLoader.Factory(mGoogleApiClient));
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.i(TAG, "GoogleApiClient connection suspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult result) {
                        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
                        if (!result.hasResolution()) {
                            // show the localized error dialog.
                            commandCallback.onError(
                                    new Exception(GoogleApiAvailability
                                            .getInstance().getErrorString(result.getErrorCode())));
                        } else {
                            try {
                                result.startResolutionForResult(mActivity, REQUEST_CODE_RESOLUTION);
                            } catch (IntentSender.SendIntentException e) {
                                Log.e(TAG, "Exception while starting resolution activity", e);
                            }
                        }
                    }
                })
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void createAdapter(final OnItemShareListener listener, final CommandCallback<BaseAdapter> commandCallback) {
        mResultsAdapter = new MemoriesAdapter(mActivity, listener);
        Drive.DriveApi.getAppFolder(mGoogleApiClient)
                .listChildren(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(@NonNull DriveApi.MetadataBufferResult result) {
                        if (!result.getStatus().isSuccess()) {
                            commandCallback.onError(new Exception("Problem while retrieving files"));
                        } else {
                            mResultsAdapter.clear();
                            mResultsAdapter.append(result.getMetadataBuffer());
                            commandCallback.onResult(mResultsAdapter);
                        }

                        // update list visibility
//                                        if (mCameraView.getVisibility() != View.VISIBLE) {
//                                            showList();
//                                        }
                    }
                });

    }

    @Override
    public void disconnect() {
        mGoogleApiClient.disconnect();
    }

    private final class PhotoUploader implements ResultCallback<DriveApi.DriveContentsResult> {
        private final Bitmap bitmap;
        private final CommandCallback<Void> callback;

        private PhotoUploader(Bitmap bitmap, CommandCallback<Void> callback) {
            this.bitmap = bitmap;
            this.callback = callback;
        }

        private Bitmap resize(Bitmap image, int maxWidth, int maxHeight) {
            if (maxHeight > 0 && maxWidth > 0) {
                int width = image.getWidth();
                int height = image.getHeight();
                float ratioBitmap = (float) width / (float) height;
                float ratioMax = (float) maxWidth / (float) maxHeight;

                int finalWidth = maxWidth;
                int finalHeight = maxHeight;
                if (ratioMax > 1) {
                    finalWidth = (int) ((float)maxHeight * ratioBitmap);
                } else {
                    finalHeight = (int) ((float)maxWidth / ratioBitmap);
                }
                image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
                return image;
            } else {
                return image;
            }
        }

        private byte[] resizeImage(Bitmap original) {
            Bitmap resized = resize(original, 2048, 2048);
            original.recycle();

            ByteArrayOutputStream blob = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 80, blob);
            resized.recycle();

            return blob.toByteArray();
        }

        @Override
        public void onResult(@NonNull DriveApi.DriveContentsResult result) {
            if (!result.getStatus().isSuccess()) {
                callback.onError(new Exception("Error while trying to create new file contents"));
                return;
            }

            final DriveContents driveContents = result.getDriveContents();

            // Perform I/O off the UI thread.
            mBackgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    // write content to DriveContents
                    OutputStream os = driveContents.getOutputStream();
                    try {
                        os.write(resizeImage(bitmap));
                        os.close();
                    } catch (IOException e) {
                        callback.onError(e);
                    }

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle(String.valueOf(System.currentTimeMillis()))
                            .setMimeType("image/jpeg")
                            .build();

                    // create a file on root folder
                    Drive.DriveApi.getAppFolder(mGoogleApiClient)
                            .createFile(mGoogleApiClient, changeSet, driveContents)
                            .setResultCallback(new ResultCallback<DriveFolder.DriveFileResult>() {
                                @Override
                                public void onResult(@NonNull DriveFolder.DriveFileResult result) {
                                   if (!result.getStatus().isSuccess()) {
                                       callback.onError(new Exception("Error while trying to create the file"));
                                   } else {
                                       callback.onResult(null);
                                   }
                                }
                            });
                }
            });
        }
    }
}
