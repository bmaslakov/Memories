package io.uuddlrlrba.memories;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.content.FileProvider;
import android.widget.BaseAdapter;

import java.io.File;
import java.io.FileOutputStream;

import io.uuddlrlrba.memories.core.CommandCallback;
import io.uuddlrlrba.memories.core.GoogleDriveMemories;
import io.uuddlrlrba.memories.core.Memories;

public class MainActivityPresenter implements Memories.OnItemShareListener {
    private Memories mMemories;
    private Context mContext;
    private Handler mBackgroundHandler;
    private final View mView;

    public MainActivityPresenter(Activity activity, Handler backgroundHandler, View view) {
        this.mContext = activity;
        this.mBackgroundHandler = backgroundHandler;
        this.mView = view;
        this.mMemories = new GoogleDriveMemories(activity, backgroundHandler);
    }

    @Override
    public void share(final Bitmap bitmap) {
        /*
         * OMG, we cannot share Google Drive file from the Android SDK. That's unfortunate.
         * :(
         */

        mView.showProgressDialog(mContext.getString(R.string.share_dialog));

        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    // save bitmap to cache directory
                    File cachePath = new File(mContext.getCacheDir(), "images");
                    cachePath.mkdirs();
                    // overwrite the image every time
                    FileOutputStream stream = new FileOutputStream(cachePath + "/image.jpg");
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                    stream.close();

                    File imagePath = new File(mContext.getCacheDir(), "images");
                    File newFile = new File(imagePath, "image.jpg");
                    Uri contentUri = FileProvider.getUriForFile(mContext,
                            "io.uuddlrlrba.memories.fileprovider", newFile);

                    if (contentUri != null) {
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        shareIntent.setDataAndType(contentUri,
                                mContext.getContentResolver().getType(contentUri));
                        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                        mView.dismissProgressDialog();
                        mContext.startActivity(Intent.createChooser(shareIntent,
                                mContext.getString(R.string.share_prompt)));
                    }
                } catch(Exception e) {
                    mView.showMessage(mContext.getString(R.string.share_exception));
                    mView.dismissProgressDialog();
                }
            }
        });

    }

    public void upload(final Bitmap bitmap) {
        mView.showProgressDialog(mContext.getString(R.string.preparing_photo));
        mMemories.upload(bitmap, new CommandCallback<Void>() {
            @Override
            public void onResult(Void result) {
                mView.dismissProgressDialog();
            }

            @Override
            public void onError(Exception e) {
                mView.dismissProgressDialog();
                mView.showMessage(e.getMessage());
            }
        });
    }

    public void connect() {
        mMemories.connect(new CommandCallback<Void>() {
            @Override
            public void onResult(Void result) {
                mMemories.createAdapter(MainActivityPresenter.this, new CommandCallback<BaseAdapter>() {
                    @Override
                    public void onResult(BaseAdapter result) {
                        mView.setAdapter(result);
                    }

                    @Override
                    public void onError(Exception e) {
                        mView.showMessage(e.getMessage());
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                mView.showMessage(e.getMessage());
            }
        });
    }

    public void disconnect() {
        mMemories.disconnect();
    }

    interface View {
        void setAdapter(BaseAdapter adapter);
        void showMessage(String mesage);
        void showProgressDialog(String message);
        void dismissProgressDialog();
    }
}
