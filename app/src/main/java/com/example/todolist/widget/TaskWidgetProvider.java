package com.example.todolist.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.example.todolist.MainActivity;
import com.example.todolist.R;
import com.example.todolist.core.WidgetData;
import java.time.ZoneId;

/** Home-screen widget: how much is due today and the next few tasks. Tapping it opens the app. */
public class TaskWidgetProvider extends AppWidgetProvider {

    private static final int[] ROW_IDS = {R.id.widgetRow1, R.id.widgetRow2, R.id.widgetRow3, R.id.widgetRow4};

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        RemoteViews views = build(context);
        for (int id : appWidgetIds) manager.updateAppWidget(id, views);
    }

    /** Redraws every placed widget; does nothing when there is none. */
    public static void refreshAll(@NonNull Context context) {
        Context app = context.getApplicationContext();
        AppWidgetManager manager = AppWidgetManager.getInstance(app);
        int[] ids = manager.getAppWidgetIds(new ComponentName(app, TaskWidgetProvider.class));
        if (ids == null || ids.length == 0) return;
        RemoteViews views = build(app);
        for (int id : ids) manager.updateAppWidget(id, views);
    }

    private static RemoteViews build(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_tasks);

        Intent open = new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pending = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetRoot, pending);

        for (int rowId : ROW_IDS) views.setViewVisibility(rowId, View.GONE);

        if (!WidgetCache.hasUser(context)) {
            views.setTextViewText(R.id.widgetSummary, context.getString(R.string.widget_signed_out));
            views.setViewVisibility(R.id.widgetEmpty, View.GONE);
            return views;
        }

        WidgetData.View data = WidgetData.build(WidgetCache.read(context), System.currentTimeMillis(),
                ZoneId.systemDefault(), ROW_IDS.length);
        views.setTextViewText(R.id.widgetSummary,
                context.getString(R.string.widget_summary, data.today, data.overdue));
        views.setViewVisibility(R.id.widgetEmpty, data.rows.isEmpty() ? View.VISIBLE : View.GONE);

        int normal = ContextCompat.getColor(context, R.color.white);
        int late = ContextCompat.getColor(context, R.color.overdue_text);
        for (int i = 0; i < data.rows.size(); i++) {
            WidgetData.Row row = data.rows.get(i);
            String text = row.when.isEmpty() ? row.title : row.when + "  " + row.title;
            views.setTextViewText(ROW_IDS[i], text);
            views.setTextColor(ROW_IDS[i], row.overdue ? late : normal);
            views.setViewVisibility(ROW_IDS[i], View.VISIBLE);
        }
        return views;
    }
}
