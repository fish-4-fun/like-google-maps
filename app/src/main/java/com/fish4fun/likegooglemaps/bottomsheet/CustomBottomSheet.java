package com.fish4fun.likegooglemaps.bottomsheet;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.fish4fun.likegooglemaps.R;

public class CustomBottomSheet extends NestedScrollView {
    public CustomBottomSheet(Context context) {
        super(context);
        init(context, null);
    }


    public CustomBottomSheet(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomBottomSheet(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }


    private void init(Context context, @Nullable AttributeSet attrs) {

        LayoutInflater.from(context).inflate(R.layout.custom_bottomsheet, this);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomBottomSheet);
            //find our header container and inflate header if required
            ViewGroup headerContainer = findViewById(R.id.headerContainer);
            int headerLayoutReferenceID = a.getResourceId(R.styleable.CustomBottomSheet_cbs_header_layout, 0);
            if (headerLayoutReferenceID != 0) {
                LayoutInflater.from(context).inflate(headerLayoutReferenceID, headerContainer);
            }
            a.recycle();
        }
    }
}
