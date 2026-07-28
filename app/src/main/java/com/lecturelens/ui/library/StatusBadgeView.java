package com.lecturelens.ui.library;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.android.material.color.MaterialColors;
import com.lecturelens.R;
import com.lecturelens.domain.model.LectureStatus;

/**
 * Track 2 (Daniel). Pill-shaped lecture status badge.
 *
 * Colors come from theme container roles (not raw color resources) so the
 * badge adapts to day/night automatically:
 *   READY                → primary container
 *   in-flight pipeline   → tertiary container
 *   RECORDED (queued)    → secondary container
 *   FAILED               → error container
 */
public class StatusBadgeView extends AppCompatTextView {

    public StatusBadgeView(@NonNull Context context) {
        this(context, null);
    }

    public StatusBadgeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public StatusBadgeView(@NonNull Context context, @Nullable AttributeSet attrs,
                           int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundResource(R.drawable.bg_status_badge);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        setAllCaps(true);
        int horizontal = dp(8);
        int vertical = dp(2);
        setPadding(horizontal, vertical, horizontal, vertical);
        if (isInEditMode()) {
            setStatus(LectureStatus.READY);
        }
    }

    public void setStatus(@NonNull LectureStatus status) {
        setText(labelFor(status));
        int background = MaterialColors.getColor(this, backgroundAttrFor(status));
        int foreground = MaterialColors.getColor(this, foregroundAttrFor(status));
        setBackgroundTintList(ColorStateList.valueOf(background));
        setTextColor(foreground);
    }

    @StringRes
    private static int labelFor(@NonNull LectureStatus status) {
        switch (status) {
            case RECORDED:
                return R.string.status_recorded;
            case TRANSCRIBING:
                return R.string.status_transcribing;
            case TRANSCRIBED:
                return R.string.status_transcribed;
            case SUMMARIZING:
                return R.string.status_summarizing;
            case INDEXING:
                return R.string.status_indexing;
            case READY:
                return R.string.status_ready;
            case SHARED:
                return R.string.status_shared;
            case FAILED:
            default:
                return R.string.status_failed;
        }
    }

    @AttrRes
    private static int backgroundAttrFor(@NonNull LectureStatus status) {
        switch (status) {
            case READY:
                return com.google.android.material.R.attr.colorPrimaryContainer;
            case SHARED:
                return com.google.android.material.R.attr.colorTertiaryContainer;
            case RECORDED:
                return com.google.android.material.R.attr.colorSecondaryContainer;
            case FAILED:
                return com.google.android.material.R.attr.colorErrorContainer;
            default: // in-flight pipeline states
                return com.google.android.material.R.attr.colorTertiaryContainer;
        }
    }

    @AttrRes
    private static int foregroundAttrFor(@NonNull LectureStatus status) {
        switch (status) {
            case READY:
                return com.google.android.material.R.attr.colorOnPrimaryContainer;
            case SHARED:
                return com.google.android.material.R.attr.colorOnTertiaryContainer;
            case RECORDED:
                return com.google.android.material.R.attr.colorOnSecondaryContainer;
            case FAILED:
                return com.google.android.material.R.attr.colorOnErrorContainer;
            default:
                return com.google.android.material.R.attr.colorOnTertiaryContainer;
        }
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                value, getResources().getDisplayMetrics()));
    }
}
