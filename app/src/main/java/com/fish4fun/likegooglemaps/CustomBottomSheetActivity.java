package com.fish4fun.likegooglemaps;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.fish4fun.likegooglemaps.bottomsheet.CustomBottomSheetBehavior;
import com.fish4fun.likegooglemaps.helpers.SpaceItemDecoration;

public class CustomBottomSheetActivity extends AppCompatActivity {

    private CustomBottomSheetBehavior bottomSheetBehavior;

    private View scrim;

    private int expandedScrimColor;
    private int collapsedScrimColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bottom_sheet);

        scrim = findViewById(R.id.scrim);

        expandedScrimColor = ContextCompat.getColor(this, R.color.toolbarScrim);
        collapsedScrimColor = ContextCompat.getColor(this, android.R.color.transparent);

        FloatingActionButton floatingActionButton = findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetBehavior.setState(CustomBottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if ((position %3 ) == 0)
                    return 2;
                else {
                    return 1;
                }
            }
        });

        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(new SimpleRecyclerViewAdapter(20));

        recyclerView.addItemDecoration(new SpaceItemDecoration(getResources().getDimensionPixelSize(R.dimen.list_spacing)));

        View bottomSheet = findViewById(R.id.bottomSheet);
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) bottomSheet.getLayoutParams();
        bottomSheetBehavior  = (CustomBottomSheetBehavior) layoutParams.getBehavior();

        assert bottomSheetBehavior != null;

        if (savedInstanceState == null) {
            bottomSheetBehavior.setState(CustomBottomSheetBehavior.STATE_HIDDEN);
        }


        bottomSheetBehavior.setBottomSheetCallback(new CustomBottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == CustomBottomSheetBehavior.STATE_EXPANDED) {

                    scrim.setBackgroundColor(expandedScrimColor);

                } else {

                    scrim.setBackgroundColor(collapsedScrimColor);

                }

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (bottomSheetBehavior.getState() == CustomBottomSheetBehavior.STATE_EXPANDED) {
            scrim.setBackgroundColor(expandedScrimColor);
        } else {
            scrim.setBackgroundColor(collapsedScrimColor);
        }

    }
}
