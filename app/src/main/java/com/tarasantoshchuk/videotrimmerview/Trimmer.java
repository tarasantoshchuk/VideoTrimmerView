package com.tarasantoshchuk.videotrimmerview;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class Trimmer extends LinearLayout implements TrimmerControls.Callback, PlayerListener, TrimmerControls.Listener, Callback {
    private static final int FRAMES_COUNT = 3;
    private static final int LONG_CLICK_EXPANTION = 3;

    private static final int MIN_TRIMMED_LENGTH_MS = 1000;
    private static final int MAX_TRIMMED_LENGTH_MS = 15000;

    private MediaMetadataRetriever mMetadataRetriever = new MediaMetadataRetriever();

    private OnTrimChangedListener mListener;

    private TrimmerControls mTrimmerControls;

    private float mVideoDurationMs;
    private float mVideoAspectRatio;
    private ZoomableLayout mZoomableLayout;

    public Trimmer(Context context) {
        this(context, null);
    }

    public Trimmer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Trimmer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.trimmer, this);
        mZoomableLayout = (ZoomableLayout) findViewById(R.id.frames);
        mZoomableLayout.setCallback(this);

        mTrimmerControls = (TrimmerControls) findViewById(R.id.controls);
        mTrimmerControls.setCallback(this);
        mTrimmerControls.setTrimListener(this);
        mMetadataRetriever.setDataSource("/storage/emulated/0/video.mp4");

        float videoHeight = Float.parseFloat(mMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        float videoWidth = Float.parseFloat(mMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));

        mVideoAspectRatio = videoWidth / videoHeight;

        mVideoDurationMs = Integer.parseInt(mMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        long durationMcs = (long) (mVideoDurationMs * 1000f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);

        if (widthMode == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("does not support this width mode");
        }

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = (int) (widthSize / mVideoAspectRatio / (float) FRAMES_COUNT);

        super.onMeasure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
    }

    public void setOnTrimChangedListener(OnTrimChangedListener listener) {
        mListener = listener;
    }

    @Override
    public float minTrimWidth() {
        return MIN_TRIMMED_LENGTH_MS / mVideoDurationMs * getWidth();
    }

    @Override
    public float maxTrimWidth() {
        return MAX_TRIMMED_LENGTH_MS / mVideoDurationMs * getWidth();
    }

    @Override
    public void onPause() {
        mTrimmerControls.hideVideoPositionIndicator();
    }

    @Override
    public void onPositionChange(float currentPosition) {
        mTrimmerControls.updateVideoPositionIndicator(currentPosition / mVideoDurationMs * getWidth());
    }

    @Override
    public void onTrimPositionChanged(float left, float right) {
        mListener.onTrimChanged(left / getWidth() * mVideoDurationMs, right / getWidth() * mVideoDurationMs);

        //todo: remove
        mTrimmerControls.updateVideoPositionIndicator((left + right) / 2f);
    }

    @Override
    public void onLongClick(float pivotX) {
        float pivotSecond = pixelToSecondPosition(pivotX);

        float zoomedStartPosition = pivotSecond - pivotSecond / LONG_CLICK_EXPANTION;
        float zoomedEndPosition = pivotSecond + (mVideoDurationMs - pivotSecond) / LONG_CLICK_EXPANTION;

        mZoomableLayout.animateViews();
    }

    @Override
    public void onLongClickRelease() {
        mZoomableLayout.revertAnimation();
    }

    @Override
    public Bitmap getBitmapAt(float pixelPosition) {
        return mMetadataRetriever.getFrameAtTime((long) (1000 * pixelToSecondPosition(pixelPosition)));
    }

    public interface OnTrimChangedListener {
        void onTrimChanged(float startTime, float endTime);
    }

    private float pixelToSecondPosition(float pixelPosition) {
        return pixelPosition / getWidth() * mVideoDurationMs;
    }

    private float secondToPixelPosition(float secondPosition) {
        return secondPosition / mVideoDurationMs * getWidth();
    }
}
