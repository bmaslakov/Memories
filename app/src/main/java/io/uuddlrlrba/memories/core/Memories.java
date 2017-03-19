package io.uuddlrlrba.memories.core;

import android.graphics.Bitmap;
import android.widget.BaseAdapter;

public interface Memories {
    void upload(Bitmap bitmap, CommandCallback<Void> callback);
    void connect(CommandCallback<Void> commandCallback);
    void createAdapter(OnItemShareListener listener, CommandCallback<BaseAdapter> commandCallback);
    void disconnect();

    interface OnItemShareListener {
        void share(Bitmap share);
    }
}
