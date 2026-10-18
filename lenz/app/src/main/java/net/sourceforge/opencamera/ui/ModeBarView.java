package net.sourceforge.opencamera.ui;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.sourceforge.opencamera.LenzMode;
import net.sourceforge.opencamera.R;

import java.util.ArrayList;
import java.util.List;

/** Gcam-style mode bar: a strip of mode labels with the selected one centred
 *  in a pill. Swipe along it or tap a label to change mode. Horizontal in
 *  portrait, vertical in landscape.
 */
public class ModeBarView extends View {
    public interface Listener {
        void onModeSelected(LenzMode mode);
    }

    private final List<LenzMode> modes = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private int selected = 0;
    private float offset = 0.0f; // animated position of the selected label, in units of label index
    private ValueAnimator animator;
    private boolean horizontal = true;
    private Listener listener;

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;
    private final GestureDetector gestureDetector;
    private float dragAccum = 0.0f;
    private boolean dragHandled = false;

    public ModeBarView(Context context) {
        this(context, null);
    }

    public ModeBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        textPaint.setColor(0xccffffff);
        textPaint.setTextSize(14 * density);
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        textPaint.setShadowLayer(3 * density, 0, 0, 0x80000000);
        selectedTextPaint.set(textPaint);
        selectedTextPaint.clearShadowLayer();
        selectedTextPaint.setColor(ContextCompat.getColor(context, R.color.lenz_on_accent));
        pillPaint.setColor(ContextCompat.getColor(context, R.color.lenz_accent));
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(@NonNull MotionEvent e) {
                dragAccum = 0.0f;
                dragHandled = false;
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                if( dragHandled )
                    return true;
                dragAccum += horizontal ? distanceX : distanceY;
                if( Math.abs(dragAccum) > 40 * density ) {
                    dragHandled = true;
                    step(dragAccum > 0 ? 1 : -1);
                }
                return true;
            }

            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent e) {
                int index = indexAt(horizontal ? e.getX() : e.getY());
                if( index >= 0 && index != selected )
                    select(index, true);
                return true;
            }
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setHorizontal(boolean horizontal) {
        if( this.horizontal != horizontal ) {
            this.horizontal = horizontal;
            invalidate();
        }
    }

    /** Sets the available modes and the current one, without notifying the listener.
     */
    public void setModes(List<LenzMode> newModes, LenzMode current) {
        if( !modes.equals(newModes) ) {
            modes.clear();
            modes.addAll(newModes);
            labels.clear();
            for(LenzMode mode : modes)
                labels.add(getResources().getString(mode.labelRes));
        }
        int index = modes.indexOf(current);
        if( index < 0 )
            index = Math.max(0, modes.indexOf(LenzMode.PHOTO));
        if( index != selected || animator == null ) {
            selected = index;
            animateTo(index);
        }
    }

    /** Moves the selection by the given number of modes (positive is towards the end of the list).
     */
    public void step(int delta) {
        int index = selected + delta;
        if( index >= 0 && index < modes.size() )
            select(index, true);
    }

    private void select(int index, boolean notify) {
        selected = index;
        animateTo(index);
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        if( notify && listener != null )
            listener.onModeSelected(modes.get(index));
    }

    private void animateTo(int index) {
        if( animator != null )
            animator.cancel();
        animator = ValueAnimator.ofFloat(offset, index);
        animator.setDuration(220);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            offset = (float)a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private float spacing() {
        return 20 * density;
    }

    private float labelLength(int i) {
        return horizontal ? textPaint.measureText(labels.get(i)) : textPaint.getTextSize();
    }

    /** Returns the centre position along the bar's main axis of label i.
     */
    private float centreOf(int i) {
        float mid = (horizontal ? getWidth() : getHeight()) / 2.0f;
        // position of each label relative to the first one
        float[] centres = new float[labels.size()];
        float pos = 0.0f;
        for(int j=0;j<labels.size();j++) {
            float len = labelLength(j);
            centres[j] = pos + len/2.0f;
            pos += len + spacing();
        }
        // interpolate the position of the (fractional) offset
        int lo = (int)Math.floor(offset);
        int hi = Math.min(lo + 1, labels.size() - 1);
        lo = Math.max(0, Math.min(lo, labels.size() - 1));
        float frac = offset - lo;
        float focus = centres[lo] + (centres[hi] - centres[lo]) * frac;
        return mid + centres[i] - focus;
    }

    private int indexAt(float pos) {
        for(int i=0;i<labels.size();i++) {
            float half = labelLength(i)/2.0f + spacing()/2.0f;
            if( Math.abs(pos - centreOf(i)) <= half )
                return i;
        }
        return -1;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if( labels.isEmpty() )
            return;
        float cross = (horizontal ? getHeight() : getWidth()) / 2.0f;
        float padX = 14 * density;
        float padY = 7 * density;
        float textHalfHeight = (textPaint.descent() + textPaint.ascent()) / 2.0f;
        for(int i=0;i<labels.size();i++) {
            String label = labels.get(i);
            float textWidth = textPaint.measureText(label);
            float c = centreOf(i);
            float cx = horizontal ? c : cross;
            float cy = horizontal ? cross : c;
            boolean isSelected = i == selected;
            if( isSelected ) {
                float h = textPaint.getTextSize()/2.0f + padY;
                rect.set(cx - textWidth/2.0f - padX, cy - h, cx + textWidth/2.0f + padX, cy + h);
                canvas.drawRoundRect(rect, h, h, pillPaint);
            }
            canvas.drawText(label, cx - textWidth/2.0f, cy - textHalfHeight, isSelected ? selectedTextPaint : textPaint);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }
}
