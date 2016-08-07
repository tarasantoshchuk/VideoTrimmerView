package com.tarasantoshchuk.videotrimmerview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.MediaMetadataRetriever;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import java.io.IOException;

public class Trimmer extends FrameLayout {
    private static final int RECT_MARGIN_DP = 25;
    private static final int CIRCLE_RADIUS_DP = 15;
    private static final int BORDER_WIDTH_DP = 5;

    private float mLeftRectPosition;
    private float mRightRectPosition;

    private float mLeftRectTranslation = 0;
    private float mRightRectTranslation = 0;

    private float mMinLeftRectPosition;
    private float mMaxRightRectPosition;

    private float mVerticalPadding;

    private float mCircleRadius;
    private float mBorderWidth;

    private final Paint mRectPaint = new Paint();
    private final Paint mCirclePaint = new Paint();

    private final RectF mBorderRectangle = new RectF();

    GestureDetector mDetector;
    MediaMetadataRetriever mMetadataRetriever = new MediaMetadataRetriever();

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
        setWillNotDraw(false);

        initDimens(context);
        initPaints();
        inflate(context, R.layout.trimmer, this);
        initGestureDetector(context);

        mMetadataRetriever.setDataSource(context.getResources().openRawResourceFd(R.raw.video).getFileDescriptor());
    }

    private void initDimens(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        mMinLeftRectPosition = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, RECT_MARGIN_DP, displayMetrics);
        mVerticalPadding = mMinLeftRectPosition;
        mBorderWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, BORDER_WIDTH_DP, displayMetrics);
        mCircleRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CIRCLE_RADIUS_DP, displayMetrics);
    }

    private void initGestureDetector(Context context) {
        mDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (Math.abs(e1.getY() - (getBottom() + getTop()) / 2) < mCircleRadius &&
                        Math.abs(e1.getX() - mLeftRectPosition) < mCircleRadius) {
                    mLeftRectTranslation -= distanceX;
                    invalidate();
                    return true;
                }

                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false;
            }
        });
    }

    private void initPaints() {
        int color = getContext().getResources().getColor(android.R.color.holo_red_light);
        mRectPaint.setColor(color);
        mRectPaint.setStrokeWidth(mBorderWidth);
        mRectPaint.setStyle(Paint.Style.STROKE);

        mCirclePaint.setColor(color);
        mCirclePaint.setStrokeWidth(mBorderWidth);
        mCirclePaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mBorderRectangle.set(getCurrentLeft(), getTop() + mVerticalPadding, getCurrentRight(), getBottom() - mVerticalPadding);
        canvas.drawRect(mBorderRectangle, mRectPaint);
        canvas.drawCircle(getCurrentLeft(), (getTop() + getBottom()) / 2f, mCircleRadius, mRectPaint);
        canvas.drawCircle(getCurrentRight(), (getTop() + getBottom()) / 2f, mCircleRadius, mRectPaint);
        canvas.drawBitmap(mMetadataRetriever.getFrameAtTime(), 0, 0, null);
    }

    private float getCurrentLeft() {
        return limit(mMinLeftRectPosition, mRightRectPosition, mLeftRectPosition + mLeftRectTranslation);
    }

    private float getCurrentRight() {
        return limit(mLeftRectPosition, mMaxRightRectPosition, mRightRectPosition + mRightRectTranslation);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        boolean result =  mDetector.onTouchEvent(event);
        if(event.getAction() == MotionEvent.ACTION_UP) {
            onUp(event);
        }

        return result;
    }

    private void onUp(MotionEvent event) {
        mLeftRectPosition = getCurrentLeft();
        mLeftRectTranslation = 0;

        mRightRectPosition = getCurrentRight();
        mRightRectTranslation = 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mMaxRightRectPosition = getRight() - mMinLeftRectPosition;
        mLeftRectPosition = mMinLeftRectPosition;
        mRightRectPosition = mMaxRightRectPosition;
    }

    private static final float limit(float min, float max, float value) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        } else {
            return value;
        }
    }
}
