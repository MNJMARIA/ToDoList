package com.example.todolist.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import com.example.todolist.R;
import com.example.todolist.core.Initials;

/**
 * Round profile picture with the user's initials and a small badge with the current rank in the corner.
 * Used in the side menu and in Settings; before, the rank badge alone stood in for the avatar.
 */
public class AvatarView extends FrameLayout {

    private static final float TEXT_SHARE = 0.38f;
    private static final float BADGE_SHARE = 0.38f;

    private final TextView initialsView;
    private final ImageView badgeView;

    public AvatarView(Context context) {
        this(context, null);
    }

    public AvatarView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AvatarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialsView = new TextView(context);
        initialsView.setGravity(Gravity.CENTER);
        initialsView.setTypeface(Typeface.DEFAULT_BOLD);
        initialsView.setTextColor(Color.WHITE);
        initialsView.setIncludeFontPadding(false);
        initialsView.setBackgroundResource(R.drawable.avatar_bg);
        addView(initialsView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        badgeView = new ImageView(context);
        badgeView.setVisibility(GONE);
        badgeView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        addView(badgeView, new LayoutParams(0, 0, Gravity.BOTTOM | Gravity.END));

        setUser(null, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int size = Math.min(w, h);
        initialsView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * TEXT_SHARE);
        LayoutParams params = (LayoutParams) badgeView.getLayoutParams();
        params.width = Math.round(size * BADGE_SHARE);
        params.height = params.width;
        badgeView.setLayoutParams(params);
    }

    /** @param name the display name, may be empty; @param email used for the letter when there is no name */
    public void setUser(@Nullable String name, @Nullable String email) {
        initialsView.setText(Initials.of(name, email));
    }

    public void setRank(@DrawableRes int rankImage) {
        badgeView.setImageResource(rankImage);
        badgeView.setVisibility(VISIBLE);
    }
}
