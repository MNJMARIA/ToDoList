package com.example.todolist.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import com.example.todolist.R;
import com.example.todolist.core.TaskCategory;

/** Shows a stored category in the language the phone is set to now. */
public final class CategoryLabels {

    private CategoryLabels() {
    }

    @StringRes
    public static int labelRes(@NonNull TaskCategory category) {
        switch (category) {
            case WORK:
                return R.string.category_work;
            case PERSONAL:
                return R.string.category_personal;
            case STUDY:
                return R.string.category_study;
            case HEALTH:
                return R.string.category_health;
            case FAMILY:
                return R.string.category_family;
            case HOBBY:
            default:
                return R.string.category_hobby;
        }
    }

    /**
     * @param stored a key ("work"), a label saved by an older version in any language, or free text
     * @return the translated label for a built-in category, otherwise the text as it was typed
     */
    @NonNull
    public static String display(@NonNull Context context, String stored) {
        TaskCategory category = TaskCategory.fromStored(stored);
        if (category != null) return context.getString(labelRes(category));
        return stored == null ? "" : stored;
    }
}
