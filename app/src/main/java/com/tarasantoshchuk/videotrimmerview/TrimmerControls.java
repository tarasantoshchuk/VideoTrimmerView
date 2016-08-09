package com.tarasantoshchuk.videotrimmerview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.MediaMetadataRetriever;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.Toast;

public class TrimmerControls extends LinearLayout {
    private static final int CIRCLE_RADIUS_DP = 15;
    private static final int BORDER_WIDTH_DP = 5;

    private float mLeftRectPosition;
    private float mRightRectPosition;

    private float mCurrentVideoPosition = 0;
    private boolean mIsVideoPositionShown = false;

    private float mMinLeftRectPosition;
    private float mMaxRightRectPosition;
    private float mMinRectWidth;
    private float mMaxRectWidth;

    private float mCircleRadius;
    private float mBorderWidth;

    private final Paint mFramePaint = new Paint();
    private final Paint mControllersPaint = new Paint();
    private final Paint mCurrentPositionPaint = new Paint();

    private final RectF mBorderRectangle = new RectF();

    private Callback mCallback;
    private Listener mListener;

    GestureDetector mDetector;
    MediaMetadataRetriever mMetadataRetriever = new MediaMetadataRetriever();
    private GestureTarget mGestureTarget;

    public TrimmerControls(Context context) {
        this(context, null);
    }

    public TrimmerControls(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TrimmerControls(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    void setCallback(Callback callback) {
        mCallback = callback;
    }

    void setTrimListener(Listener listener) {
        mListener = listener;
    }

    private void init(Context context) {
        setWillNotDraw(false);

        initDimens(context);
        initPaints();
        initGestureDetector(context);
    }

    private void initDimens(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        mMinLeftRectPosition = 0;
        mBorderWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, BORDER_WIDTH_DP, displayMetrics);
        mCircleRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CIRCLE_RADIUS_DP, displayMetrics);
    }

    private void initGestureDetector(Context context) {
        mDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                mGestureTarget = getGestureTarget(e.getX(), e.getY());
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
                switch(mGestureTarget) {
                    case LEFT_CONTROL:
                        moveLeftControl(distanceX);
                        break;
                    case RIGHT_CONTROL:
                        moveRightControl(distanceX);
                        break;
                    case FRAME:
                        moveFrame(distanceX);
                        break;
                    case NONE:
                    default:
                        return false;
                }

                mListener.onTrimPositionChanged(mLeftRectPosition, mRightRectPosition);

                invalidate();
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                switch (mGestureTarget) {
                    case LEFT_CONTROL:
                        TrimmerControls.this.onLongPress(mLeftRectPosition);
                        break;
                    case RIGHT_CONTROL:
                        TrimmerControls.this.onLongPress(mRightRectPosition);
                        break;
                    case FRAME:
                    case NONE:
                    default:
                        break;
                }

                invalidate();
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false;
            }
        });
    }

    private void onLongPress(float pivotX) {
        mListener.onLongClick(pivotX);
        Toast.makeText(getContext(), "long click at position " + pivotX, Toast.LENGTH_SHORT).show();
    }

    private void moveRightControl(float distanceX) {
        mRightRectPosition = limit(mLeftRectPosition + mMinRectWidth, mMaxRightRectPosition, Math.min(mLeftRectPosition + mMaxRectWidth, mRightRectPosition - distanceX));
    }

    private void moveFrame(float distanceX) {
        float newLeftPosition = limit(mMinLeftRectPosition, mRightRectPosition - mMinRectWidth, mLeftRectPosition - distanceX);
        float newRightPosition = limit(mLeftRectPosition + mMinRectWidth, mMaxRightRectPosition, mRightRectPosition - distanceX);

        int dxSign = distanceX > 0 ? 1 : -1;

        float allowedDx = Math.min(Math.abs(newLeftPosition - mLeftRectPosition), Math.abs(newRightPosition - mRightRectPosition)) * dxSign;

        mRightRectPosition -= allowedDx;
        mLeftRectPosition -= allowedDx;
    }

    private void moveLeftControl(float distanceX) {
        mLeftRectPosition = limit(Math.max(mRightRectPosition - mMaxRectWidth, mMinLeftRectPosition), mRightRectPosition - mMinRectWidth, mLeftRectPosition - distanceX);
    }

    private enum GestureTarget {
        LEFT_CONTROL,
        RIGHT_CONTROL,
        FRAME,
        NONE
    }

    private GestureTarget setAndReturnGestureTarget(float x, float y) {
        mGestureTarget = getGestureTarget(x, y);
        return mGestureTarget;
    }

    @NonNull
    private GestureTarget getGestureTarget(float x, float y) {
        if (x < mLeftRectPosition - mCircleRadius || x > mRightRectPosition + mCircleRadius) {
            return GestureTarget.NONE;
        }

        if (x > mLeftRectPosition + mCircleRadius && x < mRightRectPosition - mCircleRadius) {
            return GestureTarget.FRAME;
        }

        if (Math.abs(y - getCircleY()) < mCircleRadius) {
            if (x < mLeftRectPosition + mCircleRadius) {
                return GestureTarget.LEFT_CONTROL;
            } else {
                return GestureTarget.RIGHT_CONTROL;
            }
        }

        return GestureTarget.NONE;
    }

    private void initPaints() {
        int colorRed = getContext().getResources().getColor(android.R.color.holo_red_light);
        int colorBlue = getContext().getResources().getColor(android.R.color.holo_blue_bright);

        mFramePaint.setColor(colorRed);
        mFramePaint.setStrokeWidth(mBorderWidth);
        mFramePaint.setStyle(Paint.Style.STROKE);

        mControllersPaint.setColor(colorRed);
        mControllersPaint.setStrokeWidth(mBorderWidth);
        mControllersPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        mCurrentPositionPaint.setColor(colorBlue);
        mCurrentPositionPaint.setStrokeWidth(mBorderWidth);
        mCurrentPositionPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mBorderRectangle.set(getCurrentLeft(), getTop(), getCurrentRight(), getBottom());
        canvas.drawRect(mBorderRectangle, mFramePaint);
        canvas.drawCircle(getLeftCircleX(), getCircleY(), mCircleRadius, mControllersPaint);
        canvas.drawCircle(getRightCircleX(), getCircleY(), mCircleRadius, mControllersPaint);

        if (mIsVideoPositionShown) {
            canvas.drawLine(mCurrentVideoPosition, 0, mCurrentVideoPosition, getBottom(), mCurrentPositionPaint);
        }
    }

    private float getRightCircleX() {
        return getCurrentRight();
    }

    private float getLeftCircleX() {
        return getCurrentLeft();
    }

    private float getCircleY() {
        return (getTop() + getBottom()) / 2f;
    }

    private float getCurrentLeft() {
        return mLeftRectPosition;
    }

    private float getCurrentRight() {
        return mRightRectPosition;
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
        mListener.onLongClickRelease();
        mGestureTarget = null;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mMaxRightRectPosition = getRight() - mMinLeftRectPosition;
        mLeftRectPosition = mMinLeftRectPosition;
        mRightRectPosition = Math.min(mLeftRectPosition + mMaxRectWidth, mMaxRightRectPosition);
        mMinRectWidth = mCallback.minTrimWidth();
        mMaxRectWidth = mCallback.maxTrimWidth();
    }

    private static float limit(float min, float max, float value) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        } else {
            return value;
        }
    }

    interface Callback {
        float minTrimWidth();
        float maxTrimWidth();
    }

    interface Listener {
        void onTrimPositionChanged(float left, float right);
        void onLongClick(float pivotX);

        void onLongClickRelease();
    }

    void hideVideoPositionIndicator() {
        mIsVideoPositionShown = false;

        invalidate();
    }

    void updateVideoPositionIndicator(float currentPosition) {
        mIsVideoPositionShown = true;
        mCurrentVideoPosition = currentPosition;

        invalidate();
    }
}
