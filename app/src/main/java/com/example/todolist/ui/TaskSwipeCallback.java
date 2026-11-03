package com.example.todolist.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;

/**
 * Swipe a task card to the right to finish it, to the left to delete it. The colour and the icon appear
 * behind the card while it moves; the screen reacts in {@link Listener} (and offers "Undo").
 */
public final class TaskSwipeCallback extends ItemTouchHelper.SimpleCallback {

    public interface Listener {
        void onSwipedRight(int position);

        void onSwipedLeft(int position);
    }

    private final Listener listener;
    private final Paint donePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint deletePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Drawable doneIcon;
    private final Drawable deleteIcon;
    private final float cornerRadius;
    private final int iconSize;
    private final int iconMargin;
    private final RectF background = new RectF();

    public TaskSwipeCallback(@NonNull Context context, @NonNull Listener listener) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.listener = listener;
        float density = context.getResources().getDisplayMetrics().density;
        cornerRadius = 20 * density;
        iconSize = (int) (26 * density);
        iconMargin = (int) (24 * density);
        donePaint.setColor(ContextCompat.getColor(context, R.color.green));
        deletePaint.setColor(ContextCompat.getColor(context, R.color.red));
        doneIcon = whiteIcon(context, R.drawable.baseline_check_24);
        deleteIcon = whiteIcon(context, R.drawable.ic_baseline_delete_24);
    }

    private static Drawable whiteIcon(Context context, int resId) {
        Drawable drawable = ContextCompat.getDrawable(context, resId);
        if (drawable == null) return null;
        Drawable mutable = drawable.mutate();
        mutable.setTint(0xFFFFFFFF);
        return mutable;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return 0.4f;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION) return;
        if (direction == ItemTouchHelper.RIGHT) {
            listener.onSwipedRight(position);
        } else {
            listener.onSwipedLeft(position);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState,
                            boolean isCurrentlyActive) {
        View item = viewHolder.itemView;
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX != 0) {
            boolean toRight = dX > 0;
            if (toRight) {
                background.set(item.getLeft(), item.getTop(), item.getLeft() + dX, item.getBottom());
            } else {
                background.set(item.getRight() + dX, item.getTop(), item.getRight(), item.getBottom());
            }
            canvas.drawRoundRect(background, cornerRadius, cornerRadius, toRight ? donePaint : deletePaint);

            Drawable icon = toRight ? doneIcon : deleteIcon;
            if (icon != null && Math.abs(dX) > iconMargin + iconSize) {
                int top = item.getTop() + (item.getHeight() - iconSize) / 2;
                int left = toRight ? item.getLeft() + iconMargin : item.getRight() - iconMargin - iconSize;
                icon.setBounds(left, top, left + iconSize, top + iconSize);
                icon.draw(canvas);
            }
        }
        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
