package io.github.gohoski.notpipe.ui;

/**
 * Created by Gleb on 18.01.2026.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

// This class forces the ImageView to always be 16:9, cropping the black bars of 4:3 thumbnails
public class AspectRatioImageView extends ImageView {

    public AspectRatioImageView(Context context) {
        super(context);
        setScaleType(ScaleType.CENTER_CROP);
    }

    public AspectRatioImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.CENTER_CROP);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) (width * (9.0 / 16.0));
        setMeasuredDimension(width, height);
    }
}