package com.tarasantoshchuk.videotrimmerview;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class Trimmer extends FrameLayout implements TrimmerControls.Callback, PlayerListener, TrimmerControls.Listener, ZoomableLayout.Callback {
    private static final int FRAMES_COUNT = 5;

    private static final int MIN_TRIMMED_LENGTH_MS = 1000;
    private static final int MAX_TRIMMED_LENGTH_MS = 15000;

    private final HashMap<Integer, WeakReference<Bitmap>> mBitmapCache = new HashMap<>();

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
        return secondToPixelPosition(MIN_TRIMMED_LENGTH_MS);
    }

    @Override
    public float maxTrimWidth() {
        return secondToPixelPosition(MAX_TRIMMED_LENGTH_MS);
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
        //mTrimmerControls.updateVideoPositionIndicator((left + right) / 2f);
    }

    @Override
    public void onLongClick(float pivotX) {
        mZoomableLayout.animateViews(pivotX);
    }

    @Override
    public void onLongClickRelease() {
        mZoomableLayout.revertAnimation();
    }

    @Override
    public Observable<Bitmap> getBitmapAt(final float pixelPosition, int mainFrameIndex) {
        return new BitmapObservable(pixelPosition, mainFrameIndex)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private class BitmapObservable extends Observable<Bitmap> {
        public BitmapObservable(final float pixelPosition, final int mainFrameIndex) {
            super(new Observable.OnSubscribe<Bitmap>() {
                @Override
                public void call(Subscriber<? super Bitmap> subscriber) {
                    Log.d("DEBUG", "pixel position " + pixelPosition + ", mainFrameIndex " + mainFrameIndex);
                    boolean isMainFrame = mainFrameIndex != -1;

                    if (isMainFrame) {
                        WeakReference<Bitmap> bitmapRef = mBitmapCache.get(mainFrameIndex);

                        if (bitmapRef != null && bitmapRef.get() != null) {
                            returnBitmap(subscriber, mBitmapCache.get(mainFrameIndex));
                            return;
                        }
                    }

                    Bitmap raw = mMetadataRetriever.getFrameAtTime((long) (1000 * pixelToSecondPosition(pixelPosition)), MediaMetadataRetriever.OPTION_CLOSEST);
                    Bitmap scaled = Bitmap.createScaledBitmap(raw, getWidth() / FRAMES_COUNT, getHeight(), false);

                    WeakReference<Bitmap> bitmapRef = new WeakReference<>(scaled);

                    if (isMainFrame) {
                        mBitmapCache.put(mainFrameIndex, bitmapRef);
                    }

                    returnBitmap(subscriber, bitmapRef);
                }

                private void returnBitmap(Subscriber<? super Bitmap> subscriber, WeakReference<Bitmap> bitmapWeakReference) {
                    subscriber.onNext(bitmapWeakReference.get());
                    subscriber.onCompleted();
                }
            });
        }
    }

    public interface OnTrimChangedListener {
        void onTrimChanged(float startTime, float endTime);
    }

    private float pixelToSecondPosition(float pixelPosition) {
        return pixelPosition / (float)getWidth() * mVideoDurationMs;
    }

    private float secondToPixelPosition(float secondPosition) {
        return secondPosition / mVideoDurationMs * getWidth();
    }
}
