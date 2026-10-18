package net.sourceforge.opencamera.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.sourceforge.opencamera.R;

/** Lenz shutter: a white ring with a filled centre. The centre is the accent
 *  color for photos, red for video, and turns into a rounded square while
 *  recording.
 */
public class ShutterButton extends View {
    public enum State { PHOTO, VIDEO, RECORDING }

    private static final int RED = Color.rgb(234, 67, 53);

    private State state = State.PHOTO;
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float morph = 0.0f; // 0 = circle, 1 = recording square
    private float press = 0.0f; // 0..1, shrinks the centre while pressed / capturing
    private ValueAnimator morphAnimator;
    private ValueAnimator pressAnimator;
    private final int accent;

    public ShutterButton(Context context) {
        this(context, null);
    }

    public ShutterButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        accent = ContextCompat.getColor(context, R.color.lenz_accent);
        ringPaint.setColor(Color.WHITE);
        ringPaint.setStyle(Paint.Style.STROKE);
        fillPaint.setStyle(Paint.Style.FILL);
        setClickable(true);
        setFocusable(true);
    }

    public void setState(State state) {
        if( this.state == state )
            return;
        this.state = state;
        float target = state == State.RECORDING ? 1.0f : 0.0f;
        if( morphAnimator != null )
            morphAnimator.cancel();
        morphAnimator = ValueAnimator.ofFloat(morph, target);
        morphAnimator.setDuration(250);
        morphAnimator.setInterpolator(new DecelerateInterpolator());
        morphAnimator.addUpdateListener(a -> {
            morph = (float)a.getAnimatedValue();
            invalidate();
        });
        morphAnimator.start();
    }

    /** Short "squeeze" animation when a photo is taken.
     */
    public void pulse() {
        if( pressAnimator != null )
            pressAnimator.cancel();
        pressAnimator = ValueAnimator.ofFloat(0.0f, 1.0f, 0.0f);
        pressAnimator.setDuration(260);
        pressAnimator.addUpdateListener(a -> {
            press = (float)a.getAnimatedValue();
            invalidate();
        });
        pressAnimator.start();
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        animate().scaleX(pressed ? 0.92f : 1.0f).scaleY(pressed ? 0.92f : 1.0f).setDuration(120).start();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float cx = getWidth() / 2.0f;
        float cy = getHeight() / 2.0f;
        float outer = Math.min(cx, cy);
        float stroke = 4 * density;
        ringPaint.setStrokeWidth(stroke);
        canvas.drawCircle(cx, cy, outer - stroke / 2.0f, ringPaint);

        fillPaint.setColor(state == State.PHOTO ? accent : RED);
        float inner = outer - stroke - 5 * density;
        inner *= 1.0f - 0.12f * press;
        // morph from a circle to a smaller rounded square
        float half = inner * (1.0f - 0.45f * morph);
        float corner = half * (1.0f - 0.7f * morph);
        rect.set(cx - half, cy - half, cx + half, cy + half);
        canvas.drawRoundRect(rect, corner, corner, fillPaint);
    }
}
