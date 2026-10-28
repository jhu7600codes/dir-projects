package io.github.gohoski.notpipe.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.github.gohoski.notpipe.LibraryActivity;
import io.github.gohoski.notpipe.MainActivity;
import io.github.gohoski.notpipe.R;
import io.github.gohoski.notpipe.ShortsActivity;
import io.github.gohoski.notpipe.VideoListActivity;

/**
 * Created by Claude on 28.10.2026.
 * Bottom navigation bar shared by the main screens. Wraps whatever layout the activity set,
 * so the phone, tablet and landscape layouts don't need their own copies.
 */

public class NavBar {
    public static final int NONE = -1, HOME = 0, SHORTS = 1, SUBSCRIPTIONS = 2, LIBRARY = 3;
    private static final int[] IDS = {R.id.nav_home, R.id.nav_shorts, R.id.nav_subs, R.id.nav_library};

    private NavBar() {}

    /** Call right after setContentView() */
    public static void attach(final Activity activity, int selected) {
        ViewGroup content = (ViewGroup) activity.findViewById(android.R.id.content);
        View page = content.getChildAt(0);
        content.removeView(page);
        LinearLayout wrapper = new LinearLayout(activity);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(page, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 0, 1f));
        View bar = activity.getLayoutInflater().inflate(R.layout.nav_bar, wrapper, false);
        wrapper.addView(bar);
        content.addView(wrapper, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        for (int i = 0; i < IDS.length; i++) {
            final int tab = i;
            TextView item = (TextView) bar.findViewById(IDS[i]);
            // Icons are white; tint them like the text so the current tab stands out
            int color = i == selected ? 0xFFFFFFFF : 0xFFAAAAAA;
            item.setTextColor(color);
            if (i == selected) item.setTypeface(null, Typeface.BOLD);
            android.graphics.drawable.Drawable icon = item.getCompoundDrawables()[1];
            if (icon != null) {
                icon.mutate().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
                int size = (int) (26 * activity.getResources().getDisplayMetrics().density);
                icon.setBounds(0, 0, size, size);
                item.setCompoundDrawables(null, icon, null, null);
            }
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    open(activity, tab);
                }
            });
        }
    }

    private static boolean isTab(Activity activity) {
        if (activity instanceof MainActivity) return false;
        if (activity instanceof ShortsActivity || activity instanceof LibraryActivity) return true;
        return activity instanceof VideoListActivity
                && VideoListActivity.SOURCE_FEED.equals(activity.getIntent().getStringExtra(VideoListActivity.EXTRA_SOURCE));
    }

    private static void open(Activity activity, int tab) {
        Intent intent;
        switch (tab) {
            case HOME:
                intent = new Intent(activity, MainActivity.class);
                break;
            case SHORTS:
                if (activity instanceof ShortsActivity) return;
                intent = new Intent(activity, ShortsActivity.class);
                break;
            case SUBSCRIPTIONS:
                intent = new Intent(activity, VideoListActivity.class);
                intent.putExtra(VideoListActivity.EXTRA_SOURCE, VideoListActivity.SOURCE_FEED);
                intent.putExtra(VideoListActivity.EXTRA_TITLE, activity.getString(R.string.subscriptions));
                break;
            default:
                intent = new Intent(activity, LibraryActivity.class);
        }
        if (intent.getComponent().getClassName().equals(activity.getClass().getName())
                && !(activity instanceof VideoListActivity)) return;
        // Tabs replace each other instead of piling up on the back stack
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
        // Tab screens replace each other; detail screens (channel, playlist…) stay so Back returns to them
        if (isTab(activity)) activity.finish();
    }
}
