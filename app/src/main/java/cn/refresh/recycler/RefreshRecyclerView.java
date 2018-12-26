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
package cn.refresh.recycler;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;

public class RefreshRecyclerView extends ViewGroup implements NestedScrollingParent,
        NestedScrollingChild {

    private static final String LOG_TAG = RefreshRecyclerView.class.getSimpleName();


    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int INVALID_POINTER = -1;
    private static final float DRAG_RATE = .5f;

    private static final int ANIMATE_TO_TRIGGER_DURATION = 200;

    // Default offset in dips from the top of the view to where the progress spinner should stop
    //private static final int DEFAULT_CIRCLE_TARGET = 100;

    private RecyclerViewEx mRecyclerViewEx;
    private View mTarget; // the target of the gesture
    private int mTargetHeight; // the target of the gesture
    OnRefreshListener mListener;
    boolean mRefreshing = false;
    private int mTouchSlop;

    private float mSpinnerFinalOffset;

    // If nested scrolling is enabled, the total amount that needed to be
    // consumed by this as the nested scrolling parent is used in place of the
    // overscroll determined by MOVE events in the onTouch handler
    private int mTotalUnconsumed;
    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private final int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];

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

    private Animation.AnimationListener mRefreshListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (mRefreshing) {
                if (mNotify) {
                    if (mTarget instanceof State) {
                        ((State) mTarget).onRefreshing();
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
    };

    void reset() {
        ensureTarget();

        if (mTarget == null) {
            return;
        }
        if (mTarget instanceof State) {
            ((State) mTarget).onReset();
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
    public RefreshRecyclerView(Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating SwipeRefreshLayout from XML.
     *
     * @param context
     * @param attrs
     */
    public RefreshRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRecyclerViewEx = new RecyclerViewEx(context);
        mRecyclerViewEx.setEnableHeader(true);
        addView(mRecyclerViewEx);

        ensureTarget();

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setWillNotDraw(false);
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);

        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);

        mCurrentTargetOffsetTop = 0;

        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setEnabled(a.getBoolean(0, true));
        a.recycle();
    }

    public RecyclerViewEx getRecyclerViewEx() {
        return mRecyclerViewEx;
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
            mRefreshListener.onAnimationEnd(null);
        } else {
            setRefreshing(refreshing, false /* notify */);
        }
    }

    private void setRefreshing(boolean refreshing, final boolean notify) {
        if (mRefreshing != refreshing) {
            mNotify = notify;
            mRefreshing = refreshing;
            if (mRefreshing) {
                animateOffsetToCorrectPosition(mRefreshListener);
            } else {
                animateOffsetToStartPosition(mRefreshListener);
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

    public void onRefreshComplete() {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                setRefreshing(false);
            }
        }, 200);
    }

    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid
        // out yet.
        if (mTarget == null) {
            mTarget = mRecyclerViewEx.getHeaderView();
        }
        if (mTarget instanceof State) {
            mTargetHeight = ((State) mTarget).getViewHeight();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();

        if (getChildCount() == 0) {
            return;
        }

        if (mRecyclerViewEx == null) {
            return;
        }
        final View child = mRecyclerViewEx;

        child.layout(0, 0, width, height);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mRecyclerViewEx == null) {
            return;
        }

        mSpinnerFinalOffset = getMeasuredHeight() / 2;
        mRecyclerViewEx.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        if ((android.os.Build.VERSION.SDK_INT < 21 && mTarget instanceof AbsListView)
                || (mTarget != null && !ViewCompat.isNestedScrollingEnabled(mTarget))) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(b);
        }
    }

    // NestedScrollingParent

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return isEnabled() && !mRefreshing
                && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        // Dispatch up to the nested parent
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mTotalUnconsumed = mRefreshing ? mCurrentTargetOffsetTop : 0;
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed) {
        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll
        if (dy > 0 && mTotalUnconsumed > 0) {
            if (dy > mTotalUnconsumed) {
                consumed[1] = dy - (int) mTotalUnconsumed;
                mTotalUnconsumed = 0;
            } else {
                mTotalUnconsumed -= dy;
                consumed[1] = dy;
            }
            moveSpinner(mTotalUnconsumed);
        }

        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override
    public void onStopNestedScroll(View target) {
        mNestedScrollingParentHelper.onStopNestedScroll(target);
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        if (!mRefreshing) {
            if (mTotalUnconsumed > 0) {
                // finishSpinner(mTotalUnconsumed);
                finishSpinner();
                mTotalUnconsumed = 0;
            }
        }
        // Dispatch up our nested parent
        stopNestedScroll();
    }

    @Override
    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed,
                               final int dxUnconsumed, final int dyUnconsumed) {
        // Dispatch up to the nested parent first
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                mParentOffsetInWindow);

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
        if (dy < 0) {
            mTotalUnconsumed += Math.abs(dy);
            moveSpinner(mTotalUnconsumed);
        }
    }

    // NestedScrollingChild

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(
                dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    private void moveSpinner(float overscrollTop) {
        float originalDragPercent = overscrollTop / mTargetHeight;
        float dragPercent = Math.min(1f, Math.abs(originalDragPercent));

        float extraOS = Math.abs(overscrollTop) - mTargetHeight;
//        float slingshotDist = overscrollTop > 0 ? 1200 : 300;
        float slingshotDist = mSpinnerFinalOffset - mTargetHeight;
        float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, slingshotDist * 2) / slingshotDist);
        float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow((tensionSlingshotPercent / 4), 2)) * 2f;
        float extraMove = (slingshotDist) * tensionPercent * 2;

        int targetY = (int) ((mTargetHeight * dragPercent) + extraMove);

        if (mTarget instanceof State) {
            ((State) mTarget).onMove(dragPercent);
        }

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

    private void animateOffsetToCorrectPosition(Animation.AnimationListener listener) {
        mFrom = mCurrentTargetOffsetTop;
        mTo = mTargetHeight;

        animateOffsetToPosition(mFrom, mTo, listener);
    }

    private void animateOffsetToStartPosition(Animation.AnimationListener listener) {
        mFrom = mCurrentTargetOffsetTop;
        mTo = 0;

        animateOffsetToPosition(mFrom, mTo, listener);
    }

    private void animateOffsetToPosition(int from, int to, Animation.AnimationListener listener) {
        mFrom = from;
        mTo = to;

        mAnimateToPosition.reset();
        mAnimateToPosition.setDuration(ANIMATE_TO_TRIGGER_DURATION);
        mAnimateToPosition.setInterpolator(mDecelerateInterpolator);
        mAnimateToPosition.setAnimationListener(listener);

        mTarget.clearAnimation();
        mTarget.startAnimation(mAnimateToPosition);
    }

    private final Animation mAnimateToPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop = mFrom + (int) ((mTo - mFrom) * interpolatedTime);

            int offset = targetTop - mCurrentTargetOffsetTop;
            setTargetOffsetTopAndBottom(offset);
        }
    };

    void setTargetOffsetTopAndBottom(int offset) {
        ensureTarget();

        if (mTarget == null) {
            return;
        }

        LayoutParams layoutParams = mTarget.getLayoutParams();
        layoutParams.height += offset;
        mTarget.requestLayout();

        mCurrentTargetOffsetTop = layoutParams.height;
    }

    public static interface State {

        int getViewHeight();

        void onMove(float dragPercent);

        void onRefreshing();

        void onReset();
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

