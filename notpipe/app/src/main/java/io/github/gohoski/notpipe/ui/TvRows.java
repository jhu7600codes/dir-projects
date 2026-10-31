package io.github.gohoski.notpipe.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.github.gohoski.notpipe.R;
import io.github.gohoski.notpipe.ShortsActivity;
import io.github.gohoski.notpipe.VideoActivity;
import io.github.gohoski.notpipe.api.Manager;
import io.github.gohoski.notpipe.api.Metadata;
import io.github.gohoski.notpipe.data.VideoInfo;
import io.github.gohoski.notpipe.util.Feeds;
import io.github.gohoski.notpipe.util.ImageLoader;
import io.github.gohoski.notpipe.util.LocalLibrary;
import io.github.gohoski.notpipe.util.PlayerExtras;

/**
 * Created by Claude on 31.10.2026.
 * The TV Home page, laid out like the YouTube TV app: rows you scroll sideways (Continue watching,
 * Recommended, Subscriptions, Popular, Shorts), cards that grow when the remote lands on them, and the
 * focused video's title shown big at the top.
 */

public class TvRows {
    public interface Listener {
        /** The remote moved onto a card */
        void onCardFocused(VideoInfo video);
    }

    private static final int ROW_LIMIT = 20;
    private static final int CARD_WIDTH = 240, CARD_HEIGHT = 135, SHORT_WIDTH = 120, SHORT_HEIGHT = 213;

    private final Activity activity;
    private final LinearLayout root;
    private final Listener listener;
    private final int menuId;
    private final List<AsyncTask<?, ?, ?>> tasks = new ArrayList<AsyncTask<?, ?, ?>>();

    /** @param menuId the side menu, where pressing left on a row's first card goes */
    public TvRows(Activity activity, LinearLayout root, int menuId, Listener listener) {
        this.activity = activity;
        this.root = root;
        this.menuId = menuId;
        this.listener = listener;
        root.setClipChildren(false);
    }

    /** Clears the page and loads every row in parallel; each row shows up once it has videos */
    public void load() {
        cancel();
        root.removeAllViews();
        addRow(activity.getString(R.string.recommended_for_you), new Loader() {
            public List<VideoInfo> load() throws Exception {
                return Feeds.loadRecommendations();
            }
        }, false);
        addRow(activity.getString(R.string.continue_watching), new Loader() {
            public List<VideoInfo> load() {
                List<VideoInfo> history = LocalLibrary.getInstance().getHistory();
                List<VideoInfo> started = new ArrayList<VideoInfo>();
                for (int i = 0; i < history.size() && started.size() < ROW_LIMIT; i++) {
                    if (PlayerExtras.getResumePosition(history.get(i).id) > 0) started.add(history.get(i));
                }
                return started;
            }
        }, false);
        if (!LocalLibrary.getInstance().getSubscriptions().isEmpty()) {
            addRow(activity.getString(R.string.subscriptions), new Loader() {
                public List<VideoInfo> load() throws Exception {
                    return Feeds.loadSubscriptions();
                }
            }, false);
        }
        addRow(activity.getString(R.string.shorts), new Loader() {
            public List<VideoInfo> load() throws Exception {
                return Feeds.loadShorts(null);
            }
        }, true);
    }

    public void cancel() {
        for (int i = 0; i < tasks.size(); i++) tasks.get(i).cancel(true);
        tasks.clear();
    }

    /** Puts the remote's focus on the first card, if any row has loaded */
    public boolean focusFirstCard() {
        for (int i = 0; i < root.getChildCount(); i++) {
            View row = root.getChildAt(i);
            if (row.getVisibility() != View.VISIBLE) continue;
            LinearLayout cards = (LinearLayout) row.getTag();
            if (cards != null && cards.getChildCount() > 0) return cards.getChildAt(0).requestFocus();
        }
        return false;
    }

    private interface Loader {
        List<VideoInfo> load() throws Exception;
    }

    private void addRow(String title, final Loader loader, final boolean shorts) {
        final LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setVisibility(View.GONE);
        TextView label = new TextView(activity);
        label.setText(title);
        label.setTextColor(0xFFDDDDDD);
        label.setTextSize(20);
        label.setTypeface(null, Typeface.BOLD);
        label.setPadding(dp(8), dp(12), 0, dp(6));
        row.addView(label);
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        // Room for the focused card to grow without being clipped
        scroll.setClipToPadding(false);
        scroll.setClipChildren(false);
        scroll.setPadding(0, dp(10), 0, dp(14));
        row.setClipChildren(false);
        final LinearLayout cards = new LinearLayout(activity);
        cards.setPadding(dp(14), 0, dp(24), 0);
        cards.setClipChildren(false);
        scroll.addView(cards);
        row.addView(scroll);
        row.setTag(cards);
        root.addView(row);

        AsyncTask<Void, Void, List<VideoInfo>> task = new AsyncTask<Void, Void, List<VideoInfo>>() {
            @Override
            protected List<VideoInfo> doInBackground(Void... params) {
                try {
                    return loader.load();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<VideoInfo> videos) {
                if (isCancelled() || activity.isFinishing() || videos == null || videos.isEmpty()) return;
                for (int i = 0; i < videos.size() && i < ROW_LIMIT; i++) {
                    cards.addView(card(videos, i, shorts));
                }
                cards.getChildAt(0).setNextFocusLeftId(menuId);
                row.setVisibility(View.VISIBLE);
            }
        };
        tasks.add(task);
        task.execute();
    }

    private View card(final List<VideoInfo> list, final int index, final boolean isShort) {
        final VideoInfo video = list.get(index);
        int width = dp(isShort ? SHORT_WIDTH : CARD_WIDTH), height = dp(isShort ? SHORT_HEIGHT : CARD_HEIGHT);
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setFocusable(true);
        card.setClickable(true);
        card.setPadding(dp(4), dp(4), dp(4), dp(4));
        card.setBackgroundResource(R.drawable.tv_focus);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width + dp(8), LinearLayout.LayoutParams.WRAP_CONTENT);
        params.rightMargin = dp(10);
        card.setLayoutParams(params);

        FrameLayout frame = new FrameLayout(activity);
        frame.setBackgroundColor(0xFF2A2A2A);
        ImageView thumb = new ImageView(activity);
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(thumb, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
        ImageLoader.loadImage(video.thumbnail, thumb, false);
        if (!isShort && video.duration != null && video.duration.length() > 0) {
            TextView duration = new TextView(activity);
            duration.setText(video.duration);
            duration.setTextColor(Color.WHITE);
            duration.setTextSize(13);
            duration.setBackgroundColor(0xCC000000);
            duration.setPadding(dp(4), dp(1), dp(4), dp(1));
            FrameLayout.LayoutParams badge = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.RIGHT);
            badge.rightMargin = badge.bottomMargin = dp(5);
            frame.addView(duration, badge);
        }
        int resume = PlayerExtras.getResumePosition(video.id);
        int seconds = Feeds.parseDuration(video.duration);
        if (!isShort && resume > 0 && seconds > 0) {
            ProgressBar progress = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
            progress.setMax(1000);
            progress.setProgress((int) Math.min(1000, resume / (long) seconds));
            progress.setProgressDrawable(activity.getResources().getDrawable(R.drawable.short_progress));
            frame.addView(progress, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, dp(4), Gravity.BOTTOM));
        }
        card.addView(frame, new LinearLayout.LayoutParams(width, height));

        TextView title = new TextView(activity);
        title.setText(video.title);
        title.setTextColor(Color.WHITE);
        title.setTextSize(15);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setPadding(0, dp(6), 0, 0);
        card.addView(title, new LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.WRAP_CONTENT));
        if (!isShort) {
            TextView channel = new TextView(activity);
            channel.setText(video.channel);
            channel.setTextColor(0xFFAAAAAA);
            channel.setTextSize(13);
            channel.setSingleLine(true);
            channel.setEllipsize(TextUtils.TruncateAt.END);
            card.addView(channel, new LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                grow(v, hasFocus);
                if (hasFocus) listener.onCardFocused(video);
            }
        });
        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isShort) {
                    Intent intent = new Intent(activity, ShortsActivity.class);
                    intent.putExtra(ShortsActivity.EXTRA_START_ID, video.id);
                    intent.putExtra(ShortsActivity.EXTRA_START_TITLE, video.title);
                    intent.putExtra(ShortsActivity.EXTRA_START_THUMBNAIL, video.thumbnail);
                    intent.putExtra(ShortsActivity.EXTRA_START_CHANNEL, video.channel);
                    intent.putExtra(ShortsActivity.EXTRA_START_CHANNEL_ID, video.channelId);
                    activity.startActivity(intent);
                    return;
                }
                ImageLoader.clearCache();
                System.gc();
                Intent intent = new Intent(activity, VideoActivity.class);
                intent.putExtra("ID", video.id);
                activity.startActivity(intent);
            }
        });
        return card;
    }

    /** The focused card grows a little, like on YouTube TV (TVs are Android 5+, so animations are there) */
    private static void grow(View v, boolean grow) {
        try {
            // Grow from the left edge: the row scrolls the focused card to its left edge, where growing
            // from the middle would get cut off
            v.setPivotX(0);
            v.setPivotY(v.getHeight() / 2f);
            v.animate().scaleX(grow ? 1.08f : 1f).scaleY(grow ? 1.08f : 1f).setDuration(150).start();
        } catch (Throwable ignored) {}
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, activity.getResources().getDisplayMetrics());
    }
}
