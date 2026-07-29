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

/**
 * Scrolling amplitude waveform while recording. Driven by
 * {@link #setActive(boolean)} + {@link #setAmplitude(int)} from UploadFragment.
 */
public class RecordingIndicatorView extends View {

    private static final int MAX_AMPLITUDE = 32_767;
    private static final int BAR_COUNT = 48;
    private static final float MIN_BAR_FRACTION = 0.08f;

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] levels = new float[BAR_COUNT];

    private boolean active;
    private float smoothed;

    public RecordingIndicatorView(@NonNull Context context) {
        this(context, null);
    }

    public RecordingIndicatorView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecordingIndicatorView(@NonNull Context context,
                                  @Nullable AttributeSet attrs,
                                  int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        int accent = MaterialColors.getColor(
                this, androidx.appcompat.R.attr.colorError, Color.RED);
        barPaint.setColor(accent);
        barPaint.setStyle(Paint.Style.FILL);
        int track = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorSurfaceVariant, Color.GRAY);
        trackPaint.setColor(track);
        trackPaint.setStyle(Paint.Style.FILL);
        trackPaint.setAlpha(90);
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            smoothed = 0f;
            for (int i = 0; i < levels.length; i++) {
                levels[i] = 0f;
            }
        }
        invalidate();
    }

    /** @param amplitude peak amplitude {@code 0..32767} from the recorder. */
    public void setAmplitude(int amplitude) {
        if (!active) {
            return;
        }
        int clamped = Math.max(0, Math.min(MAX_AMPLITUDE, amplitude));
        float target = clamped / (float) MAX_AMPLITUDE;
        // Emphasize quiet speech: soft curve so mid levels still move bars.
        target = (float) Math.sqrt(target);
        smoothed += (target - smoothed) * 0.45f;

        System.arraycopy(levels, 1, levels, 0, levels.length - 1);
        levels[levels.length - 1] = smoothed;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (!active) {
            return;
        }

        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        float height = getHeight() - getPaddingTop() - getPaddingBottom();
        float left = getPaddingLeft();
        float top = getPaddingTop();
        float gap = Math.max(2f, width / (BAR_COUNT * 3.2f));
        float barWidth = (width - gap * (BAR_COUNT - 1)) / BAR_COUNT;
        float midY = top + height / 2f;

        for (int i = 0; i < BAR_COUNT; i++) {
            float level = Math.max(MIN_BAR_FRACTION, levels[i]);
            float barH = height * level * 0.92f;
            float x = left + i * (barWidth + gap);
            float y = midY - barH / 2f;
            canvas.drawRoundRect(x, y, x + barWidth, y + barH,
                    barWidth / 2f, barWidth / 2f, barPaint);
            // Faint full-height guide so empty bars still read as a waveform lane.
            if (levels[i] < MIN_BAR_FRACTION + 0.01f) {
                float guide = height * MIN_BAR_FRACTION;
                canvas.drawRoundRect(x, midY - guide / 2f, x + barWidth, midY + guide / 2f,
                        barWidth / 2f, barWidth / 2f, trackPaint);
            }
        }
    }
}
