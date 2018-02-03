package com.fish4fun.likegooglemaps.fab;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

import com.fish4fun.likegooglemaps.bottomsheet.CustomBottomSheet;

@SuppressWarnings("unused")
public class CustomFABBehavior extends FloatingActionButton.Behavior {

    private float initialElevation;

    public CustomFABBehavior() {
    }

    public CustomFABBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, FloatingActionButton child, int layoutDirection) {

        if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
            child.setFitsSystemWindows(true);
        }

        boolean layoutChild = super.onLayoutChild(parent, child, layoutDirection);
        //get a reference to our starting elevation
        initialElevation = child.getElevation();
        return layoutChild;
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        return dependency instanceof CustomBottomSheet || super.layoutDependsOn(parent, child, dependency);
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton child, View dependency) {

        if (dependency instanceof CustomBottomSheet) {
            CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) child.getLayoutParams();
            int newY = dependency.getTop() - child.getHeight() - layoutParams.bottomMargin;
            if (newY >= parent.getHeight() / 2) {
                child.setTranslationY(newY - layoutParams.bottomMargin);
                child.setElevation(initialElevation);
            } else {
                child.setTranslationY(parent.getHeight() / 2 - layoutParams.bottomMargin);
                child.setElevation(0);
            }
        }

        return super.onDependentViewChanged(parent, child, dependency);
    }
}
