package io.uuddlrlrba.memories.glide;

import android.content.Context;

import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.stream.StreamModelLoader;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.DriveId;

import java.io.InputStream;

/**
 * My homage for this class goes to http://stackoverflow.com/a/28830813/1256821
 */
public class DriveIdModelLoader implements StreamModelLoader<DriveId> {
    private final GoogleApiClient client;

    public DriveIdModelLoader(GoogleApiClient client) {
        this.client = client;
    }

    public DataFetcher<InputStream> getResourceFetcher(DriveId model, int width, int height) {
        return new DriveIdDataFetcher(client, model);
    }

    public static class Factory implements ModelLoaderFactory<DriveId, InputStream> {
        private final GoogleApiClient client;

        public Factory(GoogleApiClient client) {
            this.client = client;
        }

        public ModelLoader<DriveId, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new DriveIdModelLoader(client);
        }

        public void teardown() {
            client.disconnect();
        }
    }
}
