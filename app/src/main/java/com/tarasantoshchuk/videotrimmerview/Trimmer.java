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
import android.widget.ImageView;
import android.widget.LinearLayout;

public class Trimmer extends LinearLayout {
    private static final int RECT_MARGIN_DP = 25;
    private static final int CIRCLE_RADIUS_DP = 30;
    private static final int BORDER_WIDTH_DP = 5;

    private float mLeftRectPosition;
    private float mRightRectPosition;

    private float mMinLeftRectPosition;
    private float mMaxRightRectPosition;
    private float mMinRectWidth;

    private float mCircleRadius;
    private float mBorderWidth;

    private final Paint mFramePaint = new Paint();
    private final Paint mControllersPaint = new Paint();

    private final RectF mBorderRectangle = new RectF();

    GestureDetector mDetector;
    MediaMetadataRetriever mMetadataRetriever = new MediaMetadataRetriever();
    private GestureTarget mGestureTarget;

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
        //inflate(context, R.layout.trimmer, this);
        initGestureDetector(context);

//        mMetadataRetriever.setDataSource("/storage/emulated/0/video.mp4");
//        int duration = Integer.parseInt(mMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
//        ((ImageView) findViewById(R.id.image1)).setImageBitmap(mMetadataRetriever.getFrameAtTime(0));
//        ((ImageView) findViewById(R.id.image2)).setImageBitmap(mMetadataRetriever.getFrameAtTime(duration / 2));
//        ((ImageView) findViewById(R.id.image3)).setImageBitmap(mMetadataRetriever.getFrameAtTime(duration));
    }

    private void initDimens(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        mMinLeftRectPosition = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, RECT_MARGIN_DP, displayMetrics);
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
                switch(mGestureTarget == null ? setAndReturnGestureTarget(e1.getX(), e1.getY()) : mGestureTarget) {
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

                invalidate();
                return true;
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

    private void moveRightControl(float distanceX) {
        mRightRectPosition = limit(mLeftRectPosition + mMinRectWidth, mMaxRightRectPosition, mRightRectPosition - distanceX);
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
        mLeftRectPosition = limit(mMinLeftRectPosition, mRightRectPosition - mMinRectWidth, mLeftRectPosition - distanceX);
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
        int color = getContext().getResources().getColor(android.R.color.holo_red_light);
        mFramePaint.setColor(color);
        mFramePaint.setStrokeWidth(mBorderWidth);
        mFramePaint.setStyle(Paint.Style.STROKE);

        mControllersPaint.setColor(color);
        mControllersPaint.setStrokeWidth(mBorderWidth);
        mControllersPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mBorderRectangle.set(getCurrentLeft(), getTop(), getCurrentRight(), getBottom());
        //canvas.drawBitmap(mMetadataRetriever.getFrameAtTime(), 0, 0, null);
        canvas.drawRect(mBorderRectangle, mFramePaint);
        canvas.drawCircle(getLeftCircleX(), getCircleY(), mCircleRadius, mControllersPaint);
        canvas.drawCircle(getRightCircleX(), getCircleY(), mCircleRadius, mControllersPaint);
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
        mLeftRectPosition = getCurrentLeft();
        mRightRectPosition = getCurrentRight();

        mGestureTarget = null;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mMaxRightRectPosition = getRight() - mMinLeftRectPosition;
        mLeftRectPosition = mMinLeftRectPosition;
        mRightRectPosition = mMaxRightRectPosition;
        mMinRectWidth = mMinLeftRectPosition;
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
}
