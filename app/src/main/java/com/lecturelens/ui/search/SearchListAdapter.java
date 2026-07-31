package com.lecturelens.ui.search;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lecturelens.R;
import com.lecturelens.data.local.SearchHit;
import com.lecturelens.databinding.ItemSearchHitBinding;

import java.util.ArrayList;
import java.util.List;

/** Rubric ListView adapter for search results. */
public class SearchListAdapter extends BaseAdapter {

    public interface Listener {
        void onHitClicked(@NonNull SearchHit hit);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_HIT = 1;

    @NonNull private final Listener listener;
    @NonNull private final List<SearchResultsAdapter.ListItem> items = new ArrayList<>();

    public SearchListAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submit(@Nullable List<SearchResultsAdapter.ListItem> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof SearchResultsAdapter.HeaderItem
                ? TYPE_HEADER
                : TYPE_HIT;
    }

    @Override
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        Context context = parent.getContext();
        SearchResultsAdapter.ListItem item = items.get(position);
        if (item instanceof SearchResultsAdapter.HeaderItem) {
            TextView tv = convertView instanceof TextView
                    ? (TextView) convertView
                    : (TextView) LayoutInflater.from(context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            tv.setText(((SearchResultsAdapter.HeaderItem) item).title);
            return tv;
        }
        ItemSearchHitBinding binding = convertView != null && !(convertView instanceof TextView)
                ? ItemSearchHitBinding.bind(convertView)
                : ItemSearchHitBinding.inflate(LayoutInflater.from(context), parent, false);
        SearchResultsAdapter.HitItem hitItem = (SearchResultsAdapter.HitItem) item;
        SearchHit hit = hitItem.hit;
        binding.textHitType.setText(hit.sourceType != null ? hit.sourceType : "");
        binding.textHitSnippet.setText(hit.snippet != null ? hit.snippet : "");
        binding.getRoot().setOnClickListener(v -> listener.onHitClicked(hit));
        return binding.getRoot();
    }
}
