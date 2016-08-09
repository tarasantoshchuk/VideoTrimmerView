package com.tarasantoshchuk.videotrimmerview;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.ArrayList;

public class ZoomableLayout extends FrameLayout {
    public static final int MAIN_FRAMES_COUNT = 3;
    public static final int EXPANSION_FACTOR = 2;
    private ArrayList<Animator> mLastAnimatorsSet;

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
        super.onLayout(changed, left, top, right, bottom);
        positionMainFrames();
        positionAdditionalFrames();
    }

    private void positionAdditionalFrames() {
        int childCount = getChildCount();
        int width = getWidth();

        //trace additional frames
        for(int frameIndex = 0; frameIndex < childCount; frameIndex += EXPANSION_FACTOR) {
            if (frameIndex % EXPANSION_FACTOR == 0) {
                //skip main frame
                continue;
            }

            int previousMainFrameIndex = frameIndex - frameIndex % EXPANSION_FACTOR;

            ImageView view = getChildAt(frameIndex);

            int mainFrameRightSide = width / MAIN_FRAMES_COUNT * (previousMainFrameIndex + 1);
            int mainFrameLeftSide = width / MAIN_FRAMES_COUNT * previousMainFrameIndex;

            view.setLeft(mainFrameRightSide - width / MAIN_FRAMES_COUNT / 2);
            view.setRight(mainFrameRightSide + width / MAIN_FRAMES_COUNT / 2);
            view.setScaleX(0);
            view.setImageBitmap(mCallback.getBitmapAt(mainFrameLeftSide));
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

            view.setLeft(0);
            view.setRight(width / MAIN_FRAMES_COUNT);
            view.setTranslationX(leftSide);
            view.setImageBitmap(mCallback.getBitmapAt(leftSide));

            mainFrameIndex++;
        }
    }

    public void revertAnimation() {
        if (mLastAnimatorsSet != null) {
            for (Animator animator: mLastAnimatorsSet) {
                ((ObjectAnimator) animator).reverse();
            }

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(mLastAnimatorsSet);
            animatorSet.start();

            mLastAnimatorsSet = null;
        }
    }

    public void animateViews() {
        AnimatorSet animatorSet = new AnimatorSet();

        int mainFrameIndex = 0;
        int childCount = getChildCount();
        int width = getWidth();

        mLastAnimatorsSet = new ArrayList<>();

        for(int frameIndex = 0; frameIndex < childCount; frameIndex += EXPANSION_FACTOR) {
            ImageView view = getChildAt(frameIndex);

            int leftSide = width / MAIN_FRAMES_COUNT * mainFrameIndex;

            ObjectAnimator translationAnimator = new ObjectAnimator();
            translationAnimator.setTarget(view);
            translationAnimator.setProperty(View.TRANSLATION_X);
            translationAnimator.setFloatValues(leftSide + view.getWidth() * (EXPANSION_FACTOR - 1) * mainFrameIndex);

            mLastAnimatorsSet.add(translationAnimator);

            mainFrameIndex++;
        }


        //trace additional frames
        for(int frameIndex = 0; frameIndex < childCount; frameIndex += EXPANSION_FACTOR) {
            if (frameIndex % EXPANSION_FACTOR == 0) {
                //skip main frames
                continue;
            }

            int previousMainFrameIndex = frameIndex - frameIndex % EXPANSION_FACTOR;

            ImageView view = getChildAt(frameIndex);

            int leftSide = width / MAIN_FRAMES_COUNT * (previousMainFrameIndex + frameIndex % EXPANSION_FACTOR);

            ObjectAnimator translationAnimator = new ObjectAnimator();
            translationAnimator.setTarget(view);
            translationAnimator.setProperty(View.TRANSLATION_X);
            translationAnimator.setFloatValues(leftSide);

            ObjectAnimator scaleAnimator = new ObjectAnimator();
            scaleAnimator.setTarget(view);
            scaleAnimator.setProperty(View.SCALE_X);
            scaleAnimator.setFloatValues(1f);

            mLastAnimatorsSet.add(scaleAnimator);
            mLastAnimatorsSet.add(translationAnimator);

        }

        animatorSet.playTogether(mLastAnimatorsSet);
        animatorSet.start();
    }

    @Override
    public ImageView getChildAt(int index) {
        return (ImageView) super.getChildAt(index);
    }
}
