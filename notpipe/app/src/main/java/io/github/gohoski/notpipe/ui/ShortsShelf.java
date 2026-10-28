package io.github.gohoski.notpipe.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import io.github.gohoski.notpipe.R;
import io.github.gohoski.notpipe.ShortsActivity;
import io.github.gohoski.notpipe.data.VideoInfo;
import io.github.gohoski.notpipe.util.Feeds;
import io.github.gohoski.notpipe.util.ImageLoader;

/**
 * Created by Claude on 28.10.2026.
 * Horizontal row of Shorts shown in the home feed, like YouTube's Shorts shelf.
 * Tapping one opens the Shorts player starting at that Short.
 */

public class ShortsShelf {
    private static final int MAX_ITEMS = 12;

    private final Activity activity;
    private final LinearLayout root;
    private final LinearLayout row;
    private boolean loaded = false;
    private TextView title;
    private HorizontalScrollView scroll;

    /** A GONE ListView header still leaves a gap on old Android, so the contents are collapsed instead */
    private void setShown(boolean shown) {
        root.setPadding(0, shown ? dp(8) : 0, 0, shown ? dp(16) : 0);
        if (title != null) title.setVisibility(shown ? View.VISIBLE : View.GONE);
        if (scroll != null) scroll.setVisibility(shown ? View.VISIBLE : View.GONE);
    }

    public ShortsShelf(Activity activity) {
        this.activity = activity;
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        setShown(false);

        TextView title = new TextView(activity);
        title.setText(R.string.shorts);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(dp(12), 0, dp(12), dp(8));
        android.graphics.drawable.Drawable icon = activity.getResources().getDrawable(R.drawable.nav_shorts).mutate();
        icon.setColorFilter(0xFFFF0000, android.graphics.PorterDuff.Mode.SRC_IN);
        icon.setBounds(0, 0, dp(24), dp(24));
        title.setCompoundDrawables(icon, null, null, null);
        title.setCompoundDrawablePadding(dp(8));
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.startActivity(new Intent(activity, ShortsActivity.class));
            }
        });
        this.title = title;
        root.addView(title);

        scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        row = new LinearLayout(activity);
        row.setPadding(dp(8), 0, dp(8), 0);
        scroll.addView(row);
        root.addView(scroll);
    }

    public View getView() {
        return root;
    }

    /** Shows the shelf, loading it the first time */
    public void show() {
        if (loaded) {
            if (row.getChildCount() > 0) setShown(true);
            return;
        }
        loaded = true;
        new AsyncTask<Void, Void, List<VideoInfo>>() {
            @Override
            protected List<VideoInfo> doInBackground(Void... params) {
                try {
                    return Feeds.loadShorts(null);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<VideoInfo> shorts) {
                if (activity.isFinishing() || shorts == null || shorts.isEmpty()) return;
                for (int i = 0; i < shorts.size() && i < MAX_ITEMS; i++) row.addView(card(shorts.get(i)));
                setShown(true);
            }
        }.execute();
    }

    public void hide() {
        setShown(false);
    }

    private View card(final VideoInfo video) {
        FrameLayout card = new FrameLayout(activity);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(130), dp(230));
        params.leftMargin = params.rightMargin = dp(4);
        card.setLayoutParams(params);
        card.setBackgroundColor(0xFF222222);

        ImageView thumb = new ImageView(activity);
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(thumb, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
        ImageLoader.loadImage(video.thumbnail, thumb, false);

        TextView title = new TextView(activity);
        title.setText(video.title);
        title.setTextColor(Color.WHITE);
        title.setTextSize(12);
        title.setTypeface(null, Typeface.BOLD);
        title.setMaxLines(3);
        title.setShadowLayer(4, 0, 0, Color.BLACK);
        title.setPadding(dp(6), dp(6), dp(6), dp(6));
        card.addView(title, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

        card.setClickable(true);
        card.setForeground(activity.getResources().getDrawable(R.drawable.tv_focus));
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, ShortsActivity.class);
                intent.putExtra(ShortsActivity.EXTRA_START_ID, video.id);
                intent.putExtra(ShortsActivity.EXTRA_START_TITLE, video.title);
                intent.putExtra(ShortsActivity.EXTRA_START_THUMBNAIL, video.thumbnail);
                intent.putExtra(ShortsActivity.EXTRA_START_CHANNEL, video.channel);
                intent.putExtra(ShortsActivity.EXTRA_START_CHANNEL_ID, video.channelId);
                activity.startActivity(intent);
            }
        });
        return card;
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, activity.getResources().getDisplayMetrics());
    }
}
