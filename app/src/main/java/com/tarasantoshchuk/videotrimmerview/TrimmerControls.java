package com.tarasantoshchuk.videotrimmerview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.Toast;

public class TrimmerControls extends LinearLayout {
    private static final int CIRCLE_RADIUS_DP = 15;
    private static final int BORDER_WIDTH_DP = 5;

    private static final int ANIMATION_DURATION_MS = 300_000_000;
    private static final int LONG_PRESS_EXPANSION = 3;
    private final Runnable mInvalidateRunnable = new Runnable() {
        @Override
        public void run() {
            invalidate();
        }
    };

    private float mLeftRectPosition;
    private float mRightRectPosition;

    private float mCurrentVideoPosition = 0;
    private boolean mIsVideoPositionShown = false;

    private float mMinLeftRectPosition;
    private float mMaxRightRectPosition;

    private float mCircleRadius;
    private float mBorderWidth;

    private final Paint mFramePaint = new Paint();
    private final Paint mControllersPaint = new Paint();
    private final Paint mCurrentPositionPaint = new Paint();

    private final RectF mBorderRectangle = new RectF();

    private Callback mCallback;
    private Listener mListener;

    DragGestureDetector mDetector;
    private GestureTarget mGestureTarget;

    private long mAnimationStartTime;
    private float mAnimationStartLeft;
    private float mAnimationStartRight;
    private float mAnimationEndLeft;
    private float mAnimationEndRight;

    private boolean mIsAnimating;
    private boolean mIsInLongPressMode;
    private float mLongPressModePivotX;

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
        mDetector = new DragGestureDetector(context, new DragGestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                mGestureTarget = getGestureTarget(e.getX(), e.getY());
                return true;
            }

            @Override
            public void onUp() {
                TrimmerControls.this.onUp();
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY, boolean isDrag) {
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

                notifyTrimPositionChanged();

                invalidate();
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                TrimmerControls.this.onLongPress();
            }
        });
    }

    private void onLongPress() {
        float pivotPoint;

        switch (mGestureTarget) {
            case LEFT_CONTROL:
                pivotPoint = mLeftRectPosition;
                break;
            case RIGHT_CONTROL:
                pivotPoint = mRightRectPosition;
                break;
            case FRAME:
            case NONE:
            default:
                return;
        }

        startLongPressAnimation(pivotPoint);
        TrimmerControls.this.onLongPress(pivotPoint);
    }

    private void notifyTrimPositionChanged() {
        float leftPosition;
        float rightPosition;

        if (mIsInLongPressMode) {
            leftPosition = mLongPressModePivotX + (mLeftRectPosition - mLongPressModePivotX) / (float) LONG_PRESS_EXPANSION;
            rightPosition = mLongPressModePivotX + (mRightRectPosition - mLongPressModePivotX) / (float) LONG_PRESS_EXPANSION;
        } else {
            leftPosition = mLeftRectPosition;
            rightPosition = mRightRectPosition;
        }



        mListener.onTrimPositionChanged(leftPosition, rightPosition);
    }



    private void startLongPressAnimation(float pivotPoint) {
        mAnimationStartTime = System.nanoTime();

        if (!mIsAnimating) {
            mAnimationEndLeft = mLeftRectPosition + (mLeftRectPosition - pivotPoint) * (LONG_PRESS_EXPANSION - 1);
            mAnimationEndRight = mRightRectPosition + (mRightRectPosition - pivotPoint) * (LONG_PRESS_EXPANSION - 1);
        } else {
            mAnimationEndLeft = mAnimationStartLeft;
            mAnimationEndRight = mAnimationStartRight;
        }

        mAnimationStartLeft = mLeftRectPosition;
        mAnimationStartRight = mRightRectPosition;

        mIsAnimating = true;
        mIsInLongPressMode = true;
        mLongPressModePivotX = pivotPoint;

        invalidate();
    }

    private void revertLongPressAnimation() {
        removeCallbacks(mInvalidateRunnable);

        mAnimationStartTime = System.nanoTime();

        if (mIsAnimating) {
            mAnimationEndLeft = mAnimationStartLeft;
            mAnimationEndRight = mAnimationStartRight;
        } else {
            mAnimationEndLeft = mLongPressModePivotX + (mLeftRectPosition - mLongPressModePivotX) / (float) LONG_PRESS_EXPANSION;
            mAnimationEndRight = mLongPressModePivotX + (mRightRectPosition - mLongPressModePivotX) / (float) LONG_PRESS_EXPANSION;
        }

        mAnimationStartLeft = mLeftRectPosition;
        mAnimationStartRight = mRightRectPosition;

        mIsAnimating = true;
        mIsInLongPressMode = false;

        invalidate();
    }

    private void onLongPress(float pivotX) {
        mListener.onLongClick(pivotX);
        Toast.makeText(getContext(), "long click at position " + pivotX, Toast.LENGTH_SHORT).show();
    }

    private void moveRightControl(float distanceX) {
        mRightRectPosition = limit(Math.max(mLeftRectPosition + minTrimWidth(), mMinLeftRectPosition), Math.min(mLeftRectPosition + maxTrimWidth(), mMaxRightRectPosition), mRightRectPosition - distanceX);
    }

    private float maxTrimWidth() {
        return (mIsInLongPressMode ? LONG_PRESS_EXPANSION : 1) * mCallback.maxTrimWidth();
    }

    private float minTrimWidth() {
        return (mIsInLongPressMode ? LONG_PRESS_EXPANSION : 1) * mCallback.minTrimWidth();
    }

    private void moveFrame(float distanceX) {
        float newLeftPosition = limit(mMinLeftRectPosition, mRightRectPosition - minTrimWidth(), mLeftRectPosition - distanceX);
        float newRightPosition = limit(mLeftRectPosition + minTrimWidth(), mMaxRightRectPosition, mRightRectPosition - distanceX);

        int dxSign = distanceX > 0 ? 1 : -1;

        float allowedDx = Math.min(Math.abs(newLeftPosition - mLeftRectPosition), Math.abs(newRightPosition - mRightRectPosition)) * dxSign;

        mRightRectPosition -= allowedDx;
        mLeftRectPosition -= allowedDx;
    }

    private void moveLeftControl(float distanceX) {
        mLeftRectPosition = limit(Math.max(mRightRectPosition - maxTrimWidth(), mMinLeftRectPosition), Math.min(mMaxRightRectPosition, mRightRectPosition - minTrimWidth()), mLeftRectPosition - distanceX);
    }

    private enum GestureTarget {
        LEFT_CONTROL,
        RIGHT_CONTROL,
        FRAME,
        NONE
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

        handleAnimations();


        mBorderRectangle.set(getCurrentLeft(), getTop(), getCurrentRight(), getBottom());
        canvas.drawRect(mBorderRectangle, mFramePaint);
        canvas.drawCircle(getLeftCircleX(), getCircleY(), mCircleRadius, mControllersPaint);
        canvas.drawCircle(getRightCircleX(), getCircleY(), mCircleRadius, mControllersPaint);

        if (mIsVideoPositionShown) {
            canvas.drawLine(mCurrentVideoPosition, 0, mCurrentVideoPosition, getBottom(), mCurrentPositionPaint);
        }
    }

    private void handleAnimations() {
        if (mIsAnimating) {
            long currentAnimationTime = System.nanoTime() - mAnimationStartTime;


            if (currentAnimationTime < ANIMATION_DURATION_MS) {
                mLeftRectPosition = mAnimationStartLeft + (mAnimationEndLeft - mAnimationStartLeft) * (currentAnimationTime / (float) ANIMATION_DURATION_MS);
                mRightRectPosition = mAnimationStartRight + (mAnimationEndRight - mAnimationStartRight) * (currentAnimationTime / (float) ANIMATION_DURATION_MS);
            } else {
                mLeftRectPosition = mAnimationEndLeft;
                mRightRectPosition = mAnimationEndRight;

                mIsAnimating = false;

                notifyTrimPositionChanged();
            }

            if (mIsAnimating) {
                postDelayed(mInvalidateRunnable, 8);
            }
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
        return mDetector.onTouchEvent(event);
    }

    private void onUp() {
        if (mIsInLongPressMode) {
            revertLongPressAnimation();
            mListener.onLongClickRelease();
        }

        mGestureTarget = null;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mMaxRightRectPosition = getRight() - mMinLeftRectPosition;
        mLeftRectPosition = mMinLeftRectPosition;
        mRightRectPosition = Math.min(mLeftRectPosition + maxTrimWidth(), mMaxRightRectPosition);
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
