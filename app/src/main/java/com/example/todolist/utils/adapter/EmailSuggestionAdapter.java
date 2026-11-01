package com.example.todolist.utils.adapter;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.todolist.core.EmailSuggestions;
import java.util.ArrayList;
import java.util.List;

/**
 * Drop-down for the e-mail field: after the "@" it offers the typed name with the common providers, so
 * tapping a suggestion completes the address. The default ArrayAdapter filter only matches prefixes of
 * a fixed list, which cannot express "name + domain", hence the own filter.
 */
public class EmailSuggestionAdapter extends ArrayAdapter<String> {

    private final List<String> shown = new ArrayList<>();

    private final Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(@Nullable CharSequence constraint) {
            List<String> suggestions = EmailSuggestions.suggest(constraint == null ? "" : constraint.toString());
            FilterResults results = new FilterResults();
            results.values = suggestions;
            results.count = suggestions.size();
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            shown.clear();
            if (results != null && results.values != null) shown.addAll((List<String>) results.values);
            if (shown.isEmpty()) notifyDataSetInvalidated();
            else notifyDataSetChanged();
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            return resultValue == null ? "" : resultValue.toString();
        }
    };

    public EmailSuggestionAdapter(@NonNull Context context) {
        super(context, android.R.layout.simple_dropdown_item_1line);
    }

    @Override
    public int getCount() {
        return shown.size();
    }

    @Nullable
    @Override
    public String getItem(int position) {
        return shown.get(position);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return filter;
    }
}
