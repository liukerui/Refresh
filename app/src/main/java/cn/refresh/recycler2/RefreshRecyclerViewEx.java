/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.refresh.recycler2;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import cn.refresh.recycler.RecyclerViewEx;
import cn.refresh.recycler.RefreshRecyclerView;

public class RefreshRecyclerViewEx extends RecyclerViewEx {


    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int INVALID_POINTER = -1;
    private static final float DRAG_RATE = .5f;

    private static final int ANIMATE_TO_TRIGGER_DURATION = 200;

    // Default offset in dips from the top of the view to where the progress spinner should stop
    //private static final int DEFAULT_CIRCLE_TARGET = 100;
    private View mTarget; // the target of the gesture
    private int mTargetHeight; // the target of the gesture
    OnRefreshListener mListener;
    boolean mRefreshing = false;
    private int mTouchSlop;

    private float mSpinnerFinalOffset = 800;

    // If nested scrolling is enabled, the total amount that needed to be
    // consumed by this as the nested scrolling parent is used in place of the
    // overscroll determined by MOVE events in the onTouch handler
    private float mTotalUnconsumed;
    private final int[] mParentScrollConsumed;
    private final int[] mParentOffsetInWindow;

    int mCurrentTargetOffsetTop;

    // Target is returning to its start offset because it was cancelled or a
    // refresh was triggered.

    private final DecelerateInterpolator mDecelerateInterpolator;
    private static final int[] LAYOUT_ATTRS = new int[]{
            android.R.attr.enabled
    };

    protected int mFrom;
    protected int mTo;

    boolean mNotify;

    void reset() {
        ensureTarget();

        if (mTarget == null) {
            return;
        }
        if (mTarget instanceof RefreshRecyclerView.State) {
            ((RefreshRecyclerView.State) mTarget).onReset();
        }
        mTarget.clearAnimation();

        setTargetOffsetTopAndBottom(-mCurrentTargetOffsetTop);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            reset();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        reset();
    }

    /**
     * Simple constructor to use when creating a SwipeRefreshLayout from code.
     *
     * @param context
     */
    public RefreshRecyclerViewEx(Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating SwipeRefreshLayout from XML.
     *
     * @param context
     * @param attrs
     */
    public RefreshRecyclerViewEx(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mParentScrollConsumed = new int[2];
        this.mParentOffsetInWindow = new int[2];
        ensureTarget();

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

        mCurrentTargetOffsetTop = 0;

        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setEnabled(a.getBoolean(0, true));
        a.recycle();
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mListener = listener;
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setRefreshing(boolean refreshing) {
        if (refreshing && mRefreshing != refreshing) {
            // scale and show
            mRefreshing = refreshing;
            mNotify = true;
            setTargetOffsetTopAndBottom(mTargetHeight - mCurrentTargetOffsetTop);
            mAnimatorListener.onAnimationEnd(null);
        } else {
            setRefreshing(refreshing, false /* notify */);
        }
    }

    private void setRefreshing(boolean refreshing, final boolean notify) {
        if (mRefreshing != refreshing) {
            mNotify = notify;
            mRefreshing = refreshing;
            if (mRefreshing) {
                animateOffsetToCorrectPosition(mAnimatorListener);
            } else {
                animateOffsetToStartPosition(mAnimatorListener);
            }
        }
    }

    /**
     * @return Whether the SwipeRefreshWidget is actively showing refresh
     * progress.
     */
    public boolean isRefreshing() {
        return mRefreshing;
    }

    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid
        // out yet.
        if (mTarget == null) {
            mTarget = getHeaderView();
        }
        if (mTarget instanceof RefreshRecyclerView.State) {
            mTargetHeight = ((RefreshRecyclerView.State) mTarget).getViewHeight();
        }
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow, int type) {
        if (!isRefreshing()) {
            Log.e("lkr", "dy=" + dy
                    + "-consumed[1]=" + (consumed == null ? "null" : consumed[1])
                    + "-offsetInWindow[1]=" + (offsetInWindow == null ? "null" : offsetInWindow[1])
                    + "-type=" + type);
            if (dy > 0 && this.mTotalUnconsumed > 0.0F) {
                if ((float) dy > this.mTotalUnconsumed) {
                    consumed[1] = dy - (int) this.mTotalUnconsumed;
                    this.mTotalUnconsumed = 0.0F;
                } else {
                    this.mTotalUnconsumed -= (float) dy;
                    consumed[1] = dy;
                }

                this.moveSpinner(this.mTotalUnconsumed);
                Log.e("lkr", "A moveSpinner");
                return true;
            }
        }

//        int[] parentConsumed = this.mParentScrollConsumed;
//        if (this.dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, (int[])null)) {
//            consumed[0] += parentConsumed[0];
//            consumed[1] += parentConsumed[1];
//        }
        return super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow, int type) {
        if (!isRefreshing()) {
            Log.e("lkr", "dyConsumed=" + dyConsumed + "-dyUnconsumed=" + dyUnconsumed
                    + "-offsetInWindow[1]=" + (offsetInWindow == null ? "null" : offsetInWindow[1])
                    + "-type=" + type);
//        super.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type);
//        int dy = dyUnconsumed + this.mParentOffsetInWindow[1];
            int dy = dyUnconsumed;
//        if (dy < 0 && !canScrollVertically(-1)) {  // 由于第一个高度为0的Item不能正确判断。
            if (dy < 0 && type == 0) {  // type==0 拖动状态，非Fly状态
                mTotalUnconsumed += (float) Math.abs(dy);
                moveSpinner(mTotalUnconsumed);
                Log.e("lkr", "B moveSpinner");
            }
        }
//        return dxConsumed != 0 || dyConsumed != 0 || dxUnconsumed != 0 || dyUnconsumed != 0;
        return super.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type);
    }

    @Override
    public void stopNestedScroll(int type) {
        super.stopNestedScroll(type);
        Log.e("lkr1", "stopNestedScroll type=" + type);
        if (!mRefreshing) {
            if (mTotalUnconsumed > 0) {
                // finishSpinner(mTotalUnconsumed);
                finishSpinner();
                mTotalUnconsumed = 0;
            }
        }
    }

    private void moveSpinner(float overscrollTop) {
        float originalDragPercent = overscrollTop / mTargetHeight;
        float dragPercent = Math.min(1f, Math.abs(originalDragPercent));

        float extraOS = Math.abs(overscrollTop) - mTargetHeight;
        float slingshotDist = mSpinnerFinalOffset - mTargetHeight;
        float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, slingshotDist * 2) / slingshotDist);
        float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow((tensionSlingshotPercent / 4), 2)) * 2f;
        float extraMove = (slingshotDist) * tensionPercent * 2;

        int targetY = (int) ((mTargetHeight * dragPercent) + extraMove);

        if (mTarget instanceof RefreshRecyclerView.State) {
            ((RefreshRecyclerView.State) mTarget).onMove(dragPercent);
        }

        Log.e("lkr", "targetY=" + targetY);
        setTargetOffsetTopAndBottom(targetY - mCurrentTargetOffsetTop);
    }

    private void finishSpinner() {
        if (mTargetHeight > mTouchSlop && mCurrentTargetOffsetTop > mTargetHeight) {
            setRefreshing(true, true /* notify */);
        } else {
            // cancel refresh
            mRefreshing = false;
            animateOffsetToStartPosition(null);
        }
    }

    private void animateOffsetToCorrectPosition(Animator.AnimatorListener listener) {
        mFrom = mCurrentTargetOffsetTop;
        mTo = mTargetHeight;

        animateOffsetToPosition(mFrom, mTo, listener);
    }

    private void animateOffsetToStartPosition(Animator.AnimatorListener listener) {
        mFrom = mCurrentTargetOffsetTop;
        mTo = 0;

        animateOffsetToPosition(mFrom, mTo, listener);
    }

    private void animateOffsetToPosition(int from, int to, Animator.AnimatorListener listener) {
        mFrom = from;
        mTo = to;

        mTarget.animate()
                .setDuration(ANIMATE_TO_TRIGGER_DURATION)
                .setInterpolator(mDecelerateInterpolator)
                .setUpdateListener(mAnimatorUpdateListener)
                .setListener(mAnimatorListener)
                .start();
    }

    private final ValueAnimator.AnimatorUpdateListener mAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            float interpolatedTime = valueAnimator.getAnimatedFraction();
            int targetTop = mFrom + (int) ((mTo - mFrom) * interpolatedTime);

            int offset = targetTop - mCurrentTargetOffsetTop;
            setTargetOffsetTopAndBottom(offset);
        }
    };

    private final Animator.AnimatorListener mAnimatorListener = new Animator.AnimatorListener() {

        @Override
        public void onAnimationStart(Animator animator) {

        }

        @Override
        public void onAnimationEnd(Animator animator) {
            if (mRefreshing) {
                if (mNotify) {
                    if (mTarget instanceof RefreshRecyclerView.State) {
                        ((RefreshRecyclerView.State) mTarget).onRefreshing();
                    }
                    if (mListener != null) {
                        mListener.onRefresh();
                    }
                }
                ensureTarget();
                if (mTarget == null) {
                    return;
                }
                mCurrentTargetOffsetTop = mTarget.getLayoutParams().height;
            } else {
                reset();
            }
        }

        @Override
        public void onAnimationCancel(Animator animator) {
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
        }
    };

    void setTargetOffsetTopAndBottom(int offset) {
        ensureTarget();

        if (mTarget == null) {
            return;
        }

        ViewGroup.LayoutParams layoutParams = mTarget.getLayoutParams();
        layoutParams.height += offset;
        mTarget.requestLayout();

        mCurrentTargetOffsetTop = layoutParams.height;
    }


    /**
     * Classes that wish to be notified when the swipe gesture correctly
     * triggers a refresh should implement this interface.
     */
    public interface OnRefreshListener {
        /**
         * Called when a swipe gesture triggers a refresh.
         */
        void onRefresh();
    }

    public interface OnLoadMoreListener {

        void onLoadMore();
    }
}

