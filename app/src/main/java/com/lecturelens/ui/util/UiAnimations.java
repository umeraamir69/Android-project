package com.lecturelens.ui.util;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.lecturelens.R;

/** Shared enter / list / press animations for the course demo. */
public final class UiAnimations {

    private UiAnimations() {
    }

    public static void applyActivityEnter(@NonNull Activity activity) {
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public static void applyActivityExit(@NonNull Activity activity) {
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    public static void animateScreenEnter(@NonNull View root) {
        Animation anim = AnimationUtils.loadAnimation(root.getContext(), R.anim.slide_up_fade_in);
        root.startAnimation(anim);
    }

    public static void animateFragmentEnter(@NonNull Fragment fragment) {
        View root = fragment.getView();
        if (root != null) {
            animateScreenEnter(root);
        }
    }

    public static void playListLayoutAnimation(@Nullable ListView listView) {
        if (listView == null) {
            return;
        }
        LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(
                listView.getContext(), R.anim.list_layout_fall_down);
        listView.setLayoutAnimation(controller);
        listView.scheduleLayoutAnimation();
    }

    public static void playRecyclerLayoutAnimation(@Nullable RecyclerView recyclerView) {
        if (recyclerView == null) {
            return;
        }
        LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(
                recyclerView.getContext(), R.anim.list_layout_fall_down);
        recyclerView.setLayoutAnimation(controller);
        recyclerView.scheduleLayoutAnimation();
    }

    /** Stagger child views (e.g. home shortcut grid) with a short cascade. */
    public static void staggerChildren(@Nullable ViewGroup parent) {
        if (parent == null) {
            return;
        }
        int count = parent.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = parent.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(24f);
            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(40L * i)
                    .setDuration(280L)
                    .start();
        }
    }

    public static void bindPressScale(@NonNull View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                    break;
                default:
                    break;
            }
            return false;
        });
    }
}
