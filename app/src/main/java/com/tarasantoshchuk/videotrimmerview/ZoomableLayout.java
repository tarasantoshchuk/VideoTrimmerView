package com.tarasantoshchuk.videotrimmerview;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;

import rx.Observable;
import rx.Subscriber;

public class ZoomableLayout extends ViewGroup {
    public static final int MAIN_FRAMES_COUNT = 5;
    public static final int EXPANSION_FACTOR = 3;
    private int mPreviousZoomPivotMainFrame;
    private float mPreviousZoomPivotX;

    public ZoomableLayout(Context context) {
        this(context, null);
    }

    public ZoomableLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomableLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        for(int i = 0; i < MAIN_FRAMES_COUNT + (EXPANSION_FACTOR - 1) * (MAIN_FRAMES_COUNT - 1); i++) {
            addView(new ImageView(context));
        }
    }

    private Callback mCallback;

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed) {
            for(int i = 0; i < getChildCount(); i++) {
                getChildAt(i).layout(0, getTop(), 0, getBottom());
            }
        }


        positionMainFrames();
        positionAdditionalFrames();
    }

    private void positionAdditionalFrames() {
        int childCount = getChildCount();
        int width = getWidth();

        //trace additional frames
        for(int frameIndex = 0; frameIndex < childCount; frameIndex++) {
            if (frameIndex % EXPANSION_FACTOR == 0) {
                //skip main frame
                continue;
            }

            int previousMainFrameIndex = (frameIndex - frameIndex % EXPANSION_FACTOR) / EXPANSION_FACTOR;

            ImageView view = getChildAt(frameIndex);

            int mainFrameRightSide = width / MAIN_FRAMES_COUNT * (previousMainFrameIndex + 1);

            view.setLeft(0);
            view.setRight(width / MAIN_FRAMES_COUNT);
            view.setScaleX(0);
            view.setTranslationX(mainFrameRightSide - width/MAIN_FRAMES_COUNT / 2);
        }
    }

    private void positionMainFrames() {
        int childCount = getChildCount();
        int width = getWidth();

        //trace main frames
        int mainFrameIndex = 0;
        for(int frameIndex = 0; frameIndex < childCount; frameIndex += EXPANSION_FACTOR) {
            ImageView view = getChildAt(frameIndex);

            int leftSide = width / MAIN_FRAMES_COUNT * mainFrameIndex;

            setMainFrameSpanBitmap(leftSide, mainFrameIndex);

            view.setLeft(0);
            view.setRight(width / MAIN_FRAMES_COUNT);
            view.setTranslationX(leftSide);

            mainFrameIndex++;
        }
    }

    private void setSingleFrameBitmap(int leftSide, final ImageView view) {
        mCallback.getBitmapAt(leftSide, -1).subscribe(new Subscriber<Bitmap>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Bitmap bitmap) {
                view.setImageBitmap(bitmap);
            }
        });
    }

    private void setMainFrameSpanBitmap(int leftSide, final int mainFrameIndex) {
        mCallback.getBitmapAt(leftSide, mainFrameIndex).subscribe(new Subscriber<Bitmap>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Bitmap bitmap) {
                int mainFrameCommonIndex = mainFrameIndex * EXPANSION_FACTOR;

                for (int offset = 0; offset < EXPANSION_FACTOR; offset++) {
                    getChildAt(mainFrameCommonIndex + offset).setImageBitmap(bitmap);
                }
            }
        });
    }

    public void revertAnimation() {
        AnimatorSet animatorSet = new AnimatorSet();

        int mainFrameIndex = 0;
        int childCount = getChildCount();
        int width = getWidth();
        float viewWidth = width / MAIN_FRAMES_COUNT;

        ArrayList<Animator> animatorsList = new ArrayList<>();

        //trace main frames
        for(int frameIndex = 0; frameIndex < childCount; frameIndex += EXPANSION_FACTOR) {
            ImageView view = getChildAt(frameIndex);

            ObjectAnimator translationAnimator = new ObjectAnimator();
            translationAnimator.setTarget(view);
            translationAnimator.getDuration();
            translationAnimator.setProperty(View.TRANSLATION_X);
            float mainFrameLeft = mainFrameIndex * viewWidth;
            translationAnimator.setFloatValues(mainFrameLeft);

            animatorsList.add(translationAnimator);

            mainFrameIndex++;
        }


        //trace additional frames
        for(int frameIndex = 0; frameIndex < childCount; frameIndex++) {
            if (frameIndex % EXPANSION_FACTOR == 0) {
                //skip main frames
                continue;
            }

            int previousMainFrameIndex = (frameIndex - frameIndex % EXPANSION_FACTOR) / EXPANSION_FACTOR;

            ImageView view = getChildAt(frameIndex);

            float mainFrameLeft = previousMainFrameIndex * viewWidth;

            float leftSide = mainFrameLeft + viewWidth / 2f;

            ObjectAnimator translationAnimator = new ObjectAnimator();
            translationAnimator.setTarget(view);
            translationAnimator.setProperty(View.TRANSLATION_X);
            translationAnimator.setFloatValues(leftSide);

            ObjectAnimator scaleAnimator = new ObjectAnimator();
            scaleAnimator.setTarget(view);
            scaleAnimator.setProperty(View.SCALE_X);
            scaleAnimator.setFloatValues(0f);

            animatorsList.add(scaleAnimator);
            animatorsList.add(translationAnimator);

        }

        animatorSet.playTogether(animatorsList);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                onAnimationZoomOutEnd();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorSet.start();
    }

    public void animateViews(float pivotX) {
        AnimatorSet animatorSet = new AnimatorSet();

        int mainFrameIndex = 0;
        int childCount = getChildCount();
        int width = getWidth();
        float viewWidth = width / MAIN_FRAMES_COUNT;

        ArrayList<Animator> animatorsList = new ArrayList<>();

        int pivotMainFrameIndex = (int) Math.floor(pivotX / viewWidth);

        if (pivotMainFrameIndex >= MAIN_FRAMES_COUNT) {
            pivotMainFrameIndex = MAIN_FRAMES_COUNT - 1;
        } else if (pivotMainFrameIndex < 0) {
            pivotMainFrameIndex = 0;
        }

        float pivotMainFrameLeft = pivotMainFrameIndex * viewWidth;

        mPreviousZoomPivotMainFrame = pivotMainFrameIndex;
        mPreviousZoomPivotX = pivotX;

        //trace main frames
        for(int frameIndex = 0; frameIndex < childCount; frameIndex += EXPANSION_FACTOR) {
            ImageView view = getChildAt(frameIndex);

            ObjectAnimator translationAnimator = new ObjectAnimator();
            translationAnimator.setTarget(view);
            translationAnimator.setProperty(View.TRANSLATION_X);
            float mainFrameLeft = pivotMainFrameLeft + view.getWidth() * EXPANSION_FACTOR * (mainFrameIndex - pivotMainFrameIndex);
            translationAnimator.setFloatValues(mainFrameLeft);

            animatorsList.add(translationAnimator);

            mainFrameIndex++;
        }


        //trace additional frames
        for(int frameIndex = 0; frameIndex < childCount; frameIndex++) {
            if (frameIndex % EXPANSION_FACTOR == 0) {
                //skip main frames
                continue;
            }

            int previousMainFrameIndex = (frameIndex - frameIndex % EXPANSION_FACTOR) / EXPANSION_FACTOR;

            ImageView view = getChildAt(frameIndex);

            float mainFrameLeft = pivotMainFrameLeft + view.getWidth() * EXPANSION_FACTOR * (previousMainFrameIndex - pivotMainFrameIndex);

            float leftSide = mainFrameLeft + (frameIndex - previousMainFrameIndex * EXPANSION_FACTOR) * viewWidth;

            ObjectAnimator translationAnimator = new ObjectAnimator();
            translationAnimator.setTarget(view);
            translationAnimator.setProperty(View.TRANSLATION_X);
            translationAnimator.setFloatValues(leftSide);

            ObjectAnimator scaleAnimator = new ObjectAnimator();
            scaleAnimator.setTarget(view);
            scaleAnimator.setProperty(View.SCALE_X);
            scaleAnimator.setFloatValues(1f);

            animatorsList.add(scaleAnimator);
            animatorsList.add(translationAnimator);

        }

        animatorSet.playTogether(animatorsList);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                onAnimationZoomInEnd();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorSet.start();
    }

    private void onAnimationZoomInEnd() {
        int zoomPivotFrameIndex = mPreviousZoomPivotMainFrame * EXPANSION_FACTOR;

        int firstFrameOnScreen = zoomPivotFrameIndex - mPreviousZoomPivotMainFrame;
        int lastFrameOnScreen = firstFrameOnScreen + MAIN_FRAMES_COUNT;

        for (int i = firstFrameOnScreen; i < lastFrameOnScreen; i++) {
            ImageView view = getChildAt(i);
            setSingleFrameBitmap((int) (mPreviousZoomPivotX + (view.getX() - mPreviousZoomPivotX) / EXPANSION_FACTOR), view);
        }
    }

    private void onAnimationZoomOutEnd() {
        int zoomPivotFrameIndex = mPreviousZoomPivotMainFrame * EXPANSION_FACTOR;

        int firstFrameOnScreen = zoomPivotFrameIndex - mPreviousZoomPivotMainFrame;
        int lastFrameOnScreen = firstFrameOnScreen + MAIN_FRAMES_COUNT;

        for (int i = firstFrameOnScreen; i < lastFrameOnScreen; i++) {
            ImageView view = getChildAt(i);
            int mainFrameCommonIndex = (i - i % EXPANSION_FACTOR);
            ImageView mainFrame = getChildAt(mainFrameCommonIndex);
            setSingleFrameBitmap((int) mainFrame.getX(), view);
        }
    }

    @Override
    public ImageView getChildAt(int index) {
        return (ImageView) super.getChildAt(index);
    }

    public interface Callback {
        Observable<Bitmap> getBitmapAt(float pixelPosition, int mainFramePosition);
    }
}
