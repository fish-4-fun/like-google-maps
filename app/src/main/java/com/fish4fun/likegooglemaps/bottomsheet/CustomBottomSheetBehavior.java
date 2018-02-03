package com.fish4fun.likegooglemaps.bottomsheet;

/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.fish4fun.likegooglemaps.R;
import com.fish4fun.likegooglemaps.helpers.MaxHeightRecyclerView;
import com.fish4fun.likegooglemaps.helpers.ViewGroupUtils;

import java.lang.ref.WeakReference;

@SuppressWarnings({"WeakerAccess", "unused"})
public class CustomBottomSheetBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {

    public abstract static class BottomSheetCallback {

        public abstract void onStateChanged(@NonNull View bottomSheet, int newState);

        public abstract void onSlide(@NonNull View bottomSheet, float slideOffset);
    }

    public static final int STATE_DRAGGING = 1;

    public static final int STATE_SETTLING = 2;

    public static final int STATE_EXPANDED = 3;

    public static final int STATE_COLLAPSED = 4;

    public static final int STATE_HIDDEN = 5;

    public static final int PEEK_HEIGHT_AUTO = -1;

    public static final int PEEK_HEIGHT_HEADER = -2;

    private static final float HIDE_THRESHOLD = 0.5f;

    private static final float HIDE_FRICTION = 0.1f;

    private float mMaximumVelocity;

    private int collapsedHeight;

    private int mPeekHeightMin;

    private int mMinOffset;

    private int mMaxOffset;

    private int mLastNestedScrollDy;

    private int topOffset;

    private int mParentHeight;

    private int mActivePointerId;

    private int mState = STATE_COLLAPSED;

    private boolean mPeekHeightAuto;

    private boolean mPeekHeightHeader;

    private boolean mHideable;

    private boolean mNestedScrolled;

    private boolean snap;

    private boolean shouldIgnoreScrollEvents;

    private ViewDragHelper mViewDragHelper;

    private BottomSheetCallback mCallback;

    private VelocityTracker mVelocityTracker;

    private WeakReference<V> mViewRef;

    private View topShadow;

    private View bottomShadow;

    public CustomBottomSheetBehavior() {
    }

    public CustomBottomSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomBottomSheetBehavior);
        TypedValue value = a.peekValue(R.styleable.CustomBottomSheetBehavior_cbs_behavior_peekHeight);
        if (value != null && (value.data == PEEK_HEIGHT_AUTO || value.data == PEEK_HEIGHT_HEADER)) {
            setPeekHeight(value.data);
        } else {
            setPeekHeight(a.getDimensionPixelSize(R.styleable.CustomBottomSheetBehavior_cbs_behavior_peekHeight, PEEK_HEIGHT_AUTO));
        }
        setHideable(a.getBoolean(R.styleable.CustomBottomSheetBehavior_cbs_behavior_hideable, false));

        topOffset = a.getDimensionPixelSize(R.styleable.CustomBottomSheetBehavior_cbs_behavior_topOffset,0);
        snap = a.getBoolean(R.styleable.CustomBottomSheetBehavior_cbs_behavior_snap, false);

        a.recycle();
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    @Override
    public Parcelable onSaveInstanceState(CoordinatorLayout parent, V child) {
        return new CustomBottomSheetBehavior.SavedState(super.onSaveInstanceState(parent, child), mState);
    }

    @Override
    public void onRestoreInstanceState(CoordinatorLayout parent, V child, Parcelable state) {
        SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(parent, child, ss.getSuperState());
        // Intermediate states are restored as collapsed state
        if (ss.state == STATE_DRAGGING || ss.state == STATE_SETTLING) {
            mState = STATE_COLLAPSED;
        } else {
            mState = ss.state;
        }
    }

    private boolean isTouchInChildBounds(
            ViewGroup parent, View child, MotionEvent ev) {
        return ViewGroupUtils.isPointInChildBounds(
                parent, child, (int) ev.getX(), (int) ev.getY());
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, final V child, int layoutDirection) {

        if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
            child.setFitsSystemWindows(true);
        }

        //find our header container and inflate header if required
        ViewGroup headerContainer = child.findViewById(R.id.headerContainer);
        topShadow = child.findViewById(R.id.card_header_shadow_top);
        bottomShadow = child.findViewById(R.id.card_header_shadow);

        int savedTop = child.getTop();
        // First let the parent lay it out
        parent.onLayoutChild(child, layoutDirection);

        // Offset the bottom sheet
        mParentHeight = parent.getHeight();

        //adjust total height if required
        if (topOffset > 0) {
            final CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) child.getLayoutParams();
            layoutParams.height = parent.getHeight() - topOffset;
        }

        //get the height of the shadow for determining our collapsed height
        int shadowHeight = topShadow.getHeight();

        if (getPeekHeight() == PEEK_HEIGHT_HEADER) {

            collapsedHeight = headerContainer.getHeight() + shadowHeight;

        } else if (getPeekHeight() == PEEK_HEIGHT_AUTO) {

            collapsedHeight = Math.max(mPeekHeightMin, mParentHeight - parent.getWidth() * 9 / 16);

        }

        mMinOffset = Math.max(0, mParentHeight - child.getHeight());

        mMaxOffset = Math.max(mParentHeight - collapsedHeight, mMinOffset);

        // Give the RecyclerView a maximum height to ensure proper bottom sheet height
        final int rvMaxHeight =
                child.getHeight()
                        - shadowHeight
                        - headerContainer.getHeight();

        MaxHeightRecyclerView rv = child.findViewById(R.id.recyclerView);
        rv.setMaxHeight(rvMaxHeight);

        if (mState == STATE_EXPANDED) {
            ViewCompat.offsetTopAndBottom(child, mMinOffset);
        } else if (mHideable && mState == STATE_HIDDEN) {
            ViewCompat.offsetTopAndBottom(child, mParentHeight);
        } else if (mState == STATE_COLLAPSED) {
            ViewCompat.offsetTopAndBottom(child, mMaxOffset);
        } else if (mState == STATE_DRAGGING || mState == STATE_SETTLING) {
            ViewCompat.offsetTopAndBottom(child, savedTop - child.getTop());
        }

        if (mViewDragHelper == null) {
            mViewDragHelper = ViewDragHelper.create(parent, mDragCallback);
        }
        mViewRef = new WeakReference<>(child);
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {

        if (!child.isShown()) {
            return false;
        }

        int action = event.getActionMasked();
        // Record the velocity
        if (action == MotionEvent.ACTION_DOWN) {
            reset();
        }

        if (mState == STATE_SETTLING) {
            return true;
        }

        shouldIgnoreScrollEvents = false;

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }

        mVelocityTracker.addMovement(event);
        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mActivePointerId = MotionEvent.INVALID_POINTER_ID;
                break;
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = event.getPointerId(event.getActionIndex());
                break;
        }

        return mViewDragHelper.shouldInterceptTouchEvent(event) || event.getActionMasked() == MotionEvent.ACTION_DOWN && isTouchInChildBounds(parent, child, event) && !isTouchInChildBounds(parent, child.findViewById(R.id.headerContainer), event) && !isTouchInChildBounds(parent, child.findViewById(R.id.recyclerView), event);

    }

    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {

        if (!mNestedScrolled) {
            return;
        }

        if (dyUnconsumed != 0) {
            RecyclerView rv = coordinatorLayout.findViewById(R.id.recyclerView);
            if (dyUnconsumed > 0) {
                if (rv.canScrollVertically(1)) {
                    dyConsumed += dyUnconsumed;
                    rv.scrollBy(0, dyUnconsumed);
                    dyUnconsumed = 0;
                }
            } else if (dyUnconsumed < 0) {
                if (rv.canScrollVertically(-1)) {
                    dyConsumed += dyUnconsumed;
                    rv.scrollBy(0, dyUnconsumed);
                    dyUnconsumed = 0;
                }
            }
        }
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type);
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View directTargetChild, @NonNull View target, int nestedScrollAxes, int viewType) {

        mLastNestedScrollDy = 0;
        mNestedScrolled = false;

        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }


    @Override
    public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {

        //catch a case when non-touch scroll events are still firing and the user
        //manually sets the state, we want to ignore all events until the next
        //user scroll or fling
        if (shouldIgnoreScrollEvents) {
            return;
        }

        int currentTop = child.getTop();
        int newTop = currentTop - dy;

        if (dy > 0) { // Upward
            if (newTop < mMinOffset) {

                consumed[1] = currentTop - mMinOffset;
                ViewCompat.offsetTopAndBottom(child, -consumed[1]);
                setStateInternal(STATE_EXPANDED);

            } else {

                consumed[1] = dy;
                ViewCompat.offsetTopAndBottom(child, -dy);
                setStateInternal(STATE_DRAGGING);
            }
        } else if (dy < 0) { // Downward

            RecyclerView rv = coordinatorLayout.findViewById(R.id.recyclerView);

            if (!rv.canScrollVertically(-1)) {

                int bottomThreshold = mMaxOffset;

                if (isHideable()) {
                    bottomThreshold += collapsedHeight;
                }

                if (newTop <= bottomThreshold) {
                    consumed[1] = dy;
                    ViewCompat.offsetTopAndBottom(child, -dy);
                    setStateInternal(STATE_DRAGGING);
                } else {
                    consumed[1] = currentTop - bottomThreshold;
                    ViewCompat.offsetTopAndBottom(child, -consumed[1]);
                    if (bottomThreshold == mMaxOffset) {
                        setStateInternal(STATE_COLLAPSED);
                    } else {
                        setStateInternal(STATE_HIDDEN);
                    }
                }
            }

        }

        dispatchOnSlide(child.getTop());
        mLastNestedScrollDy = dy;
        mNestedScrolled = true;
    }

    @Override
    public void onStopNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull V child, @NonNull View target, int viewType) {

        if (!mNestedScrolled || shouldIgnoreScrollEvents) {
            return;
        }

        if (child.getTop() == mMinOffset) {
            setStateInternal(STATE_EXPANDED);
            return;
        }

        int top;
        int targetState;
        int currentTop = child.getTop();

        //if we are hideable, let adjust the bottom snap threshold to be calculated by the parent height
        int mMaxSnapOffset = mMaxOffset;
        if (isHideable()) {
            mMaxSnapOffset = mParentHeight;
        }

        //snap at 1/4 from max (bottom)
        boolean isBelowBottomSnapThreshold = snap && currentTop > mMaxSnapOffset - (mMaxSnapOffset / 4);

        //snap at 1/4 from min (top)
        boolean isAboveTopSnapThreshold = snap && currentTop - mMinOffset < (mMaxSnapOffset - mMinOffset) / 4;

        if (mLastNestedScrollDy > 0) {

            if (isAboveTopSnapThreshold) {
                top = mMinOffset;
                targetState = STATE_EXPANDED;
            } else if (isBelowBottomSnapThreshold) {
                top = currentTop;
                targetState = STATE_DRAGGING;
            } else {
                top = currentTop;
                targetState = STATE_DRAGGING;
            }

        } else if (isBelowBottomSnapThreshold) {

            if (mHideable) {
                top = mParentHeight;
                targetState = STATE_HIDDEN;
            } else {
                top = mMaxOffset;
                targetState = STATE_COLLAPSED;
            }

        } else {
            targetState = STATE_DRAGGING;
            top = currentTop;
        }

        if (mViewDragHelper.smoothSlideViewTo(child, child.getLeft(), top)) {
            setStateInternal(STATE_SETTLING);
            ViewCompat.postOnAnimation(child, new SettleRunnable(child, targetState));
        } else {
            setStateInternal(targetState);
        }
        mNestedScrolled = false;
    }

    public final void setPeekHeight(int peekHeight) {
        boolean layout = false;
        if (peekHeight == PEEK_HEIGHT_AUTO) {
            if (!mPeekHeightAuto) {
                mPeekHeightAuto = true;
                layout = true;
            }
        } else if (peekHeight == PEEK_HEIGHT_HEADER) {
            if (!mPeekHeightHeader) {
                mPeekHeightHeader = true;
                layout = true;
            }
        } else if (mPeekHeightHeader || collapsedHeight != peekHeight) {
            mPeekHeightAuto = false;
            mPeekHeightHeader = false;
            collapsedHeight = Math.max(0, peekHeight);
            mMaxOffset = mParentHeight - peekHeight;
            layout = true;
        }
        if (layout && mState == STATE_COLLAPSED && mViewRef != null) {
            V view = mViewRef.get();
            if (view != null) {
                view.requestLayout();
            }
        }
    }

    public final int getPeekHeight() {
        if (mPeekHeightHeader) {
            return PEEK_HEIGHT_HEADER;
        } else return mPeekHeightAuto ? PEEK_HEIGHT_AUTO : collapsedHeight;
    }

    public void setHideable(boolean hideable) {
        mHideable = hideable;
    }

    public boolean isHideable() {
        return mHideable;
    }

    public void setBottomSheetCallback(BottomSheetCallback callback) {
        mCallback = callback;
    }

    @SuppressWarnings("SameParameterValue")
    public final void setState(final int state) {

        if (state == mState) {
            return;
        }

        if (mViewRef == null) {
            // The view is not laid out yet; modify mState and let onLayoutChild handle it later
            if (state == STATE_COLLAPSED || state == STATE_EXPANDED ||
                    (mHideable && state == STATE_HIDDEN)) {
                mState = state;
            }
            return;
        }
        final V child = mViewRef.get();
        if (child == null) {
            return;
        }

        shouldIgnoreScrollEvents = true;

        // Start the animation; wait until a pending layout if there is one.
        ViewParent parent = child.getParent();
        if (parent != null && parent.isLayoutRequested() && ViewCompat.isAttachedToWindow(child)) {
            child.post(new Runnable() {
                @Override
                public void run() {
                    startSettlingAnimation(child, state);
                }
            });
        } else {
            startSettlingAnimation(child, state);
        }
    }


    public final int getState() {
        return mState;
    }

    void setStateInternal(int state) {
        if (mState == state) {
            return;
        }
        mState = state;
        View bottomSheet = mViewRef.get();
        if (bottomSheet != null && mCallback != null) {
            mCallback.onStateChanged(bottomSheet, state);
        }

        if (mState == STATE_EXPANDED) {

            bottomShadow.animate().alpha(1.0f).setDuration(75);
            topShadow.animate().alpha(0.0f).setDuration(75);

        } else if (mState == STATE_DRAGGING) {

            bottomShadow.animate().alpha(0.0f).setDuration(75);
            topShadow.animate().alpha(1.0f).setDuration(75);
        }

    }

    private void reset() {
        mActivePointerId = ViewDragHelper.INVALID_POINTER;
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    boolean shouldHide(View child, float yVel) {
        if (child.getTop() < mMaxOffset) {
            // It should not hide, but collapse.
            return false;
        }
        final float newTop = child.getTop() + yVel * HIDE_FRICTION;
        return Math.abs(newTop - mMaxOffset) / (float) collapsedHeight > HIDE_THRESHOLD;
    }

    private float getYVelocity() {
        mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
        return mVelocityTracker.getYVelocity(mActivePointerId);
    }


    void startSettlingAnimation(View child, int state) {

        int top;
        if (state == STATE_COLLAPSED) {
            top = mMaxOffset;
        } else if (state == STATE_EXPANDED) {
            top = mMinOffset;
        } else if (mHideable && state == STATE_HIDDEN) {
            top = mParentHeight;
        } else {
            throw new IllegalArgumentException("Illegal state argument: " + state);
        }
        if (mViewDragHelper.smoothSlideViewTo(child, child.getLeft(), top)) {
            setStateInternal(STATE_SETTLING);
            ViewCompat.postOnAnimation(child, new SettleRunnable(child, state));
        } else {
            setStateInternal(state);
        }
    }

    private final ViewDragHelper.Callback mDragCallback = new ViewDragHelper.Callback() {

        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            return false;
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            dispatchOnSlide(top);
        }

        @Override
        public int getViewVerticalDragRange(@NonNull View child) {
            if (mHideable) {
                return mParentHeight - mMinOffset;
            } else {
                return mMaxOffset - mMinOffset;
            }
        }
    };

    void dispatchOnSlide(int top) {
        View bottomSheet = mViewRef.get();
        if (bottomSheet != null && mCallback != null) {
            if (top > mMaxOffset) {
                mCallback.onSlide(bottomSheet, (float) (mMaxOffset - top) /
                        (mParentHeight - mMaxOffset));
            } else {
                mCallback.onSlide(bottomSheet,
                        (float) (mMaxOffset - top) / ((mMaxOffset - mMinOffset)));
            }
        }
    }

    @VisibleForTesting
    int getPeekHeightMin() {
        return mPeekHeightMin;
    }

    private class SettleRunnable implements Runnable {

        private final View mView;

        private final int mTargetState;

        SettleRunnable(View view, int targetState) {
            mView = view;
            mTargetState = targetState;
        }

        @Override
        public void run() {
            if (mViewDragHelper != null && mViewDragHelper.continueSettling(true)) {
                ViewCompat.postOnAnimation(mView, this);
            } else {
                setStateInternal(mTargetState);
            }
        }
    }

    protected static class SavedState extends AbsSavedState {

        final int state;

        public SavedState(Parcel source) {
            this(source, null);
        }

        public SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            //noinspection ResourceType
            state = source.readInt();
        }

        public SavedState(Parcelable superState, int state) {
            super(superState);
            this.state = state;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(state);
        }

        public static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

}


