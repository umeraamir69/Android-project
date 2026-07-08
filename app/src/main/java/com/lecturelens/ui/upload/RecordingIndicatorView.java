package com.lecturelens.ui.upload;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.color.MaterialColors;
import com.lecturelens.R;
/**
 * Track 3 — lightweight recording level indicator: a filled dot that pulses with
 * the mic amplitude while active. Self-contained (no XML attrs); themed with the
 * app's {@code colorError} role so it reads as the familiar "recording" red.
 *
 * <p>Drive it from {@code UploadFragment} as {@link RecordingState.Recording}
 * arrives: {@link #setActive(boolean)} + {@link #setAmplitude(int)}.
 */
public class RecordingIndicatorView extends View {
    private static final int MAX_AMPLITUDE = 32_767;
    private static final float BASE_RADIUS_FRACTION = 0.35f; // of half-min-dimension
    private static final float PULSE_RADIUS_FRACTION = 0.55f; // additional, scaled by level
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean active;
    private float level;         // 0..1 smoothed
    private float targetLevel;   // 0..1 latest
    public RecordingIndicatorView(@NonNull Context context) {
        this(context, null);
    }
    public RecordingIndicatorView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public RecordingIndicatorView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        int color = MaterialColors.getColor(
                this, androidx.appcompat.R.attr.colorError, Color.RED);
        dotPaint.setColor(color);
        dotPaint.setStyle(Paint.Style.FILL);
    }
    /** Show/hide the pulse. When inactive the view collapses to nothing. */
    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            level = 0f;
            targetLevel = 0f;
        }
        invalidate();
    }
    /** @param amplitude peak amplitude {@code 0..32767} from the recorder. */
    public void setAmplitude(int amplitude) {
        int clamped = Math.max(0, Math.min(MAX_AMPLITUDE, amplitude));
        targetLevel = clamped / (float) MAX_AMPLITUDE;
        invalidate();
    }
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (!active) {
            return;
        }
        // Ease the drawn level toward the target for a smooth pulse.
        level += (targetLevel - level) * 0.25f;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float halfMin = Math.min(getWidth(), getHeight()) / 2f;
        float radius = halfMin * (BASE_RADIUS_FRACTION + PULSE_RADIUS_FRACTION * level);
        canvas.drawCircle(cx, cy, radius, dotPaint);
        // Keep easing until settled.
        if (Math.abs(targetLevel - level) > 0.01f) {
            postInvalidateOnAnimation();
        }
    }
}