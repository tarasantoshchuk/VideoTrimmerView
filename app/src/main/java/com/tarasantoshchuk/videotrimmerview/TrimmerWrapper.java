package com.tarasantoshchuk.videotrimmerview;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class TrimmerWrapper extends LinearLayout {
    private MediaMetadataRetriever mMetadataRetriever = new MediaMetadataRetriever();

    public TrimmerWrapper(Context context) {
        this(context, null);
    }

    public TrimmerWrapper(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TrimmerWrapper(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.trimmer, this);
        mMetadataRetriever.setDataSource("/storage/emulated/0/video.mp4");
        int duration = Integer.parseInt(mMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        ((ImageView) findViewById(R.id.image1)).setImageBitmap(mMetadataRetriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST));
        ((ImageView) findViewById(R.id.image2)).setImageBitmap(mMetadataRetriever.getFrameAtTime(duration / 2, MediaMetadataRetriever.OPTION_CLOSEST));
        ((ImageView) findViewById(R.id.image3)).setImageBitmap(mMetadataRetriever.getFrameAtTime(duration - 1, MediaMetadataRetriever.OPTION_CLOSEST));
    }
}
