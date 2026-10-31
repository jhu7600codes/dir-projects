package io.github.gohoski.notpipe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.github.gohoski.notpipe.api.Manager;
import io.github.gohoski.notpipe.api.Metadata;
import io.github.gohoski.notpipe.api.Yt2009Account;
import io.github.gohoski.notpipe.data.VideoInfo;
import io.github.gohoski.notpipe.ui.TvRows;
import io.github.gohoski.notpipe.ui.VideoAdapter;
import io.github.gohoski.notpipe.util.Feeds;
import io.github.gohoski.notpipe.util.ImageLoader;
import io.github.gohoski.notpipe.util.InstancesUpdater;
import io.github.gohoski.notpipe.util.LocalLibrary;

/**
 * Created by Claude on 25.10.2026.
 * Home screen for TVs (Android TV launcher entry): a side menu and a grid of video cards, made for the remote.
 * Moving through the menu loads its section, like the YouTube TV app.
 */

public class TvActivity extends Activity implements InstancesUpdater.OnInstancesUpdatedListener {
    /** Home is the recommendations (rows), like YouTube's own TV app */
    private static final int HOME = 0, SEARCH = 1, POPULAR = 2, SUBSCRIPTIONS = 3, SHORTS = 4,
            HISTORY = 5, LIBRARY = 6, SETTINGS = 7, RECOMMENDED = -2;
    private static final int[] MENU_TITLES = {R.string.home, R.string.search, R.string.popular,
            R.string.subscriptions, R.string.shorts, R.string.history, R.string.library, R.string.settings};
    private static final int[] MENU_ICONS = {R.drawable.nav_home, android.R.drawable.ic_menu_search,
            android.R.drawable.ic_menu_compass, R.drawable.nav_subs,
            R.drawable.nav_shorts, android.R.drawable.ic_menu_recent_history, R.drawable.nav_library,
            android.R.drawable.ic_menu_preferences};
    private static final int SECTION_LOAD_DELAY = 400;

    private Context context;
    private ListView menu;
    private GridView grid;
    private final List<VideoInfo> videos = new ArrayList<VideoInfo>();
    private VideoAdapter adapter;
    private Handler handler = new Handler();
    private int currentSection = -1;
    private int pendingSection = -1;
    private String lastQuery = "";
    private LoadTask loadTask;
    private boolean isDestroyedFlag = false;
    private TvRows rows;
    private boolean rowsLoaded = false;

    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            TextView clock = (TextView) findViewById(R.id.tv_clock);
            if (clock != null) clock.setText(io.github.gohoski.notpipe.util.PlayerExtras.formatClock(context, new java.util.Date()));
            handler.postDelayed(this, 10000);
        }
    };

    private final Runnable loadPending = new Runnable() {
        @Override
        public void run() {
            showSection(pendingSection);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_tv);
        context = this;

        menu = (ListView) findViewById(R.id.tv_menu);
        grid = (GridView) findViewById(R.id.tv_grid);
        adapter = new VideoAdapter(this, R.layout.item_tv_card, videos);
        grid.setAdapter(adapter);

        menu.setAdapter(new MenuAdapter());
        rows = new TvRows(this, (LinearLayout) findViewById(R.id.tv_rows), R.id.tv_menu, new TvRows.Listener() {
            public void onCardFocused(VideoInfo video) {
                ((TextView) findViewById(R.id.tv_focus_title)).setText(video.title);
                String meta = video.channel == null ? "" : video.channel;
                if (video.views > 0) meta += " · " + getString(R.string.views, Utils.formatNumber(context, video.views));
                ((TextView) findViewById(R.id.tv_focus_meta)).setText(meta);
            }
        });
        menu.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Sections that open another screen only open on click, the rest preview while moving
                if (position == SEARCH || position == SHORTS || position == LIBRARY || position == SETTINGS) return;
                if (position == currentSection) return;
                pendingSection = position;
                handler.removeCallbacks(loadPending);
                handler.postDelayed(loadPending, SECTION_LOAD_DELAY);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        menu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                handler.removeCallbacks(loadPending);
                switch (position) {
                    case SEARCH:
                        showSearch();
                        break;
                    case SHORTS:
                        startActivity(new Intent(context, ShortsActivity.class));
                        break;
                    case LIBRARY:
                        startActivity(new Intent(context, LibraryActivity.class));
                        break;
                    case SETTINGS:
                        startActivity(new Intent(context, SettingsActivity.class));
                        break;
                    case HOME:
                        if (position != currentSection) showSection(position);
                        rows.focusFirstCard();
                        break;
                    default:
                        if (position != currentSection) showSection(position);
                        if (!videos.isEmpty()) grid.requestFocus();
                }
            }
        });
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ImageLoader.clearCache();
                System.gc();
                Intent intent = new Intent(context, VideoActivity.class);
                intent.putExtra("ID", videos.get(position).id);
                startActivity(intent);
            }
        });

        int start = HOME;
        menu.setSelection(start);
        menu.requestFocus();
        if (InstancesUpdater.isUpdateDue()) {
            currentSection = start;
            new InstancesUpdater(this, this).updateInstances();
        } else {
            showSection(start);
        }
    }

    @Override
    public void onInstancesUpdated() {
        if (!isDestroyedFlag) showSection(currentSection);
    }

    private void showSection(int section) {
        if (section < 0) return;
        currentSection = section;
        ((BaseAdapter) menu.getAdapter()).notifyDataSetChanged();
        boolean home = section == HOME;
        findViewById(R.id.tv_rows_scroll).setVisibility(home ? View.VISIBLE : View.GONE);
        findViewById(R.id.tv_focus_info).setVisibility(home ? View.VISIBLE : View.GONE);
        findViewById(R.id.tv_section).setVisibility(home ? View.GONE : View.VISIBLE);
        grid.setVisibility(home ? View.GONE : View.VISIBLE);
        if (home) {
            if (loadTask != null) loadTask.cancel(true);
            findViewById(R.id.tv_empty).setVisibility(View.GONE);
            findViewById(R.id.tv_loading).setVisibility(View.GONE);
            if (!rowsLoaded) {
                rowsLoaded = true;
                rows.load();
            }
            return;
        }
        ((TextView) findViewById(R.id.tv_section)).setText(section == SEARCH
                ? getString(R.string.search) + ": " + lastQuery : getString(MENU_TITLES[section]));
        if (loadTask != null) loadTask.cancel(true);
        videos.clear();
        adapter.notifyDataSetChanged();
        findViewById(R.id.tv_empty).setVisibility(View.GONE);
        findViewById(R.id.tv_loading).setVisibility(View.VISIBLE);
        loadTask = new LoadTask(section);
        loadTask.execute(lastQuery);
    }

    private void showSearch() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(lastQuery);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.search)
                .setView(input)
                .setPositiveButton(R.string.search, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        search(input.getText().toString());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null).create();
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                dialog.dismiss();
                search(input.getText().toString());
                return true;
            }
        });
        dialog.show();
    }

    private void search(String query) {
        query = query.trim();
        if (query.length() == 0) return;
        lastQuery = query;
        showSection(SEARCH);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // The search button on remotes
        if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            showSearch();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK && (grid.hasFocus() || findViewById(R.id.tv_rows).hasFocus())) {
            menu.requestFocus();
            return true;
        }
        // From the menu into the Home rows (the grid is hidden there, so default focus search finds nothing)
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && menu.hasFocus() && currentSection == HOME && rows.focusFirstCard()) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(clockTick);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(clockTick);
        handler.post(clockTick);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ImageLoader.clearCache();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyedFlag = true;
        handler.removeCallbacks(loadPending);
        if (loadTask != null) loadTask.cancel(true);
        rows.cancel();
        if (isFinishing()) ImageLoader.clearCache();
    }

    private class LoadTask extends AsyncTask<String, Void, List<VideoInfo>> {
        private final int section;

        LoadTask(int section) {
            this.section = section;
        }

        @Override
        protected List<VideoInfo> doInBackground(String... params) {
            try {
                switch (section) {
                    case SEARCH:
                        return Manager.getInstance().getMetadata().search(params[0]);
                    case RECOMMENDED:
                        return Feeds.loadRecommendations();
                    case SUBSCRIPTIONS:
                        return Feeds.loadSubscriptions();
                    case HISTORY:
                        return LocalLibrary.getInstance().getHistory();
                    default:
                        Metadata popular = Manager.getInstance().getPopularMetadata();
                        if (popular != null) {
                            try {
                                return popular.getPopularVideos();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        return Manager.getInstance().getMetadata().getPopularVideos();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<VideoInfo> result) {
            if (isCancelled() || isDestroyedFlag || section != currentSection) return;
            findViewById(R.id.tv_loading).setVisibility(View.GONE);
            videos.clear();
            if (result != null) videos.addAll(result);
            adapter.notifyDataSetChanged();
            TextView empty = (TextView) findViewById(R.id.tv_empty);
            if (videos.isEmpty()) {
                empty.setText(result == null ? R.string.load_failed
                        : section == SUBSCRIPTIONS ? R.string.no_subscriptions
                        : section == RECOMMENDED ? R.string.no_recommendations : R.string.no_videos);
                empty.setVisibility(View.VISIBLE);
            }
        }
    }

    private class MenuAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return MENU_TITLES.length;
        }

        @Override
        public Object getItem(int position) {
            return MENU_TITLES[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView item = convertView instanceof TextView ? (TextView) convertView : new TextView(context);
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics());
            item.setPadding(padding * 2, padding, padding, padding);
            item.setTextSize(18);
            item.setSingleLine(true);
            item.setEllipsize(android.text.TextUtils.TruncateAt.END);
            boolean current = position == currentSection;
            item.setTextColor(current ? 0xFF33B5E5 : 0xFFFFFFFF);
            item.setTypeface(null, current ? Typeface.BOLD : Typeface.NORMAL);
            item.setText(MENU_TITLES[position]);
            int icon = MENU_ICONS[position];
            if (icon != 0) {
                android.graphics.drawable.Drawable d = getResources().getDrawable(icon).mutate();
                d.setColorFilter(current ? 0xFF33B5E5 : 0xFFFFFFFF, android.graphics.PorterDuff.Mode.SRC_IN);
                int size = padding * 2;
                d.setBounds(0, 0, size, size);
                item.setCompoundDrawables(d, null, null, null);
            } else {
                item.setCompoundDrawables(null, null, null, null);
            }
            item.setCompoundDrawablePadding(padding);
            return item;
        }
    }
}
