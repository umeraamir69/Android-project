package com.lecturelens.ui.util;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;

/** Expands a ListView inside NestedScrollView so all rows are measurable. */
public final class ListViewHeight {

    private ListViewHeight() {
    }

    public static void expand(@NonNull ListView listView) {
        ListAdapter adapter = listView.getAdapter();
        if (adapter == null) {
            return;
        }
        int total = 0;
        int width = listView.getWidth() > 0
                ? View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.EXACTLY)
                : View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        for (int i = 0; i < adapter.getCount(); i++) {
            View item = adapter.getView(i, null, listView);
            item.measure(width, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            total += item.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = total + (listView.getDividerHeight() * Math.max(0, adapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}
