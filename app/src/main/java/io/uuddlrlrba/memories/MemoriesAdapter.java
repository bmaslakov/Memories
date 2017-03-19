package io.uuddlrlrba.memories;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.widget.DataBufferAdapter;

import java.text.DateFormat;

import io.uuddlrlrba.memories.core.Memories;

public class MemoriesAdapter extends DataBufferAdapter<Metadata> {
    private DateFormat timeFormat;
    private DateFormat dateFormat;
    private Memories.OnItemShareListener listener;

    public MemoriesAdapter(Context context, Memories.OnItemShareListener listener) {
        super(context, R.layout.row_memory);
        this.timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        this.dateFormat = android.text.format.DateFormat.getLongDateFormat(context);
        this.listener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(getContext(), R.layout.row_memory, null);
        }

        final Metadata metadata = getItem(position);

        final TextView titleTextView = (TextView) convertView.findViewById(R.id.text_view);
        final TextView errorTextView = (TextView) convertView.findViewById(R.id.text_view_error);
        final ImageView imageView = (ImageView) convertView.findViewById(R.id.image_view);
        final CardView cardView = (CardView) convertView.findViewById(R.id.card_view);
        final ProgressBar progressBar = (ProgressBar) convertView.findViewById(R.id.progress_bar);

        progressBar.setVisibility(View.VISIBLE);
        errorTextView.setVisibility(View.GONE);
        imageView.setOnClickListener(null);

        titleTextView.setText(dateFormat.format(metadata.getCreatedDate()) + " "
                + timeFormat.format(metadata.getCreatedDate()));
        Glide.with(getContext())
                .from(DriveId.class)
                .load(metadata.getDriveId())
                .centerCrop()
                .listener(new RequestListener<DriveId, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, DriveId model,
                           Target<GlideDrawable> target, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        errorTextView.setVisibility(View.VISIBLE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(final GlideDrawable resource, DriveId model,
                           Target<GlideDrawable> target, boolean isFromMemoryCache,
                           boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        imageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                listener.share(((GlideBitmapDrawable) resource).getBitmap());
                            }
                        });
                        return false;
                    }
                })
                .into(imageView);

        int dp = getContext().getResources().getDimensionPixelSize(R.dimen.list_divider);
        if (position != 0) {
            ((LinearLayout.LayoutParams) cardView.getLayoutParams()).topMargin = 0;
        } else {
            ((LinearLayout.LayoutParams) cardView.getLayoutParams()).topMargin = dp;
        }

        return convertView;
    }
}
