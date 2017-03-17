package io.uuddlrlrba.memories;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.widget.DataBufferAdapter;

import java.text.DateFormat;

public class MemoriesAdapter extends DataBufferAdapter<Metadata> {
    private DateFormat timeFormat;
    private DateFormat dateFormat;

    public MemoriesAdapter(Context context) {
        super(context, R.layout.row_memory);
        timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        dateFormat = android.text.format.DateFormat.getLongDateFormat(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(getContext(), R.layout.row_memory, null);
        }

        Metadata metadata = getItem(position);

        TextView titleTextView = (TextView) convertView.findViewById(R.id.text_view);
        ImageView imageView = (ImageView) convertView.findViewById(R.id.image_view);
        CardView cardView = (CardView) convertView.findViewById(R.id.card_view);

        titleTextView.setText(dateFormat.format(metadata.getCreatedDate()) + " "
                + timeFormat.format(metadata.getCreatedDate()));
        Glide.with(getContext())
                .from(DriveId.class)
                .load(metadata.getDriveId())
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
