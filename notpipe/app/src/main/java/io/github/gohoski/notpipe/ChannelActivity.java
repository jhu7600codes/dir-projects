package io.github.gohoski.notpipe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import io.github.gohoski.notpipe.api.ChannelApi;
import io.github.gohoski.notpipe.api.ContentUnavailableException;
import io.github.gohoski.notpipe.api.Manager;
import io.github.gohoski.notpipe.api.Metadata;
import io.github.gohoski.notpipe.api.Yt2009Account;
import io.github.gohoski.notpipe.data.Channel;
import io.github.gohoski.notpipe.data.VideoInfo;
import io.github.gohoski.notpipe.ui.VideoActions;
import io.github.gohoski.notpipe.ui.VideoAdapter;
import io.github.gohoski.notpipe.util.Compat;
import io.github.gohoski.notpipe.util.ImageLoader;
import io.github.gohoski.notpipe.util.LocalLibrary;

/**
 * Created by Gleb on 12.03.2026.
 */

public class ChannelActivity extends Activity {
    Metadata api;
    ChannelApi channelApi;
    String channelId;
    Channel channel;
    LinearLayout channelLayout;
    Context context;
    ImageView banner;
    boolean isDestroyedFlag = false;

    TabHost tabHost;
    AbsListView forYouList;
    AbsListView videosList;
    VideoAdapter forYouAdapter;
    VideoAdapter videosAdapter;

    Spinner sortingSpinner;
    int currentSort = 0; // 0 = Latest, 1 = Popular
    boolean metadataGetVideosFailed = false;

    List<VideoInfo> forYouVideos = null;
    List<VideoInfo> latestVideos = null;
    List<VideoInfo> popularVideos = null;

    /**
     * Big YouTube-style cards on phones (same margins as Home), compact rows in tablet grids.
     * Also hides the banner while the list is scrolled so there's more room for videos.
     */
    private int cardLayout(AbsListView view) {
        if (!(view instanceof ListView)) return R.layout.item_video_compact;
        ListView list = (ListView) view;
        int side = (int) (24 * getResources().getDisplayMetrics().density);
        list.setPadding(side, 0, side, 0);
        list.setDivider(null);
        list.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScrollStateChanged(AbsListView v, int state) {}

            public void onScroll(AbsListView v, int first, int visible, int total) {
                if (banner == null || channel == null || channel.banner == null || channel.banner.length() == 0) return;
                if (first >= 2 && banner.getVisibility() == View.VISIBLE) banner.setVisibility(View.GONE);
                else if (first == 0 && banner.getVisibility() != View.VISIBLE) banner.setVisibility(View.VISIBLE);
            }
        });
        return R.layout.item_video;
    }

    /** Subscribe / Shorts / Playlists buttons under the channel header, instead of only in the menu */
    private void addActionRow() {
        if (actionRow != null || !(channelLayout.getParent() instanceof ViewGroup)) return;
        final float density = getResources().getDisplayMetrics().density;
        actionRow = new LinearLayout(context);
        actionRow.setPadding((int) (16 * density), 0, (int) (16 * density), (int) (8 * density));
        actionRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        subscribeButton = actionButton(getString(R.string.subscribe));
        subscribeButton.setBackgroundResource(R.drawable.subscribe_button);
        subscribeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleSubscribe();
            }
        });
        subscribeButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                if (!io.github.gohoski.notpipe.api.Yt2009Account.getInstance().isSignedIn()) return false;
                showYoutubeSubscribe();
                return true;
            }
        });
        actionRow.addView(subscribeButton);

        TextView shorts = actionButton(getString(R.string.shorts));
        shorts.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(context, ShortsActivity.class);
                intent.putExtra(ShortsActivity.EXTRA_CHANNEL, channel != null ? channel.id : channelId);
                startActivity(intent);
            }
        });
        actionRow.addView(shorts);

        TextView playlists = actionButton(getString(R.string.playlists));
        playlists.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                VideoListActivity.open(ChannelActivity.this, VideoListActivity.SOURCE_CHANNEL_PLAYLISTS,
                        channel != null ? channel.id : channelId, getString(R.string.playlists));
            }
        });
        actionRow.addView(playlists);

        ViewGroup parent = (ViewGroup) channelLayout.getParent();
        parent.addView(actionRow, parent.indexOfChild(channelLayout) + 1);
        updateSubscribeButton();
    }

    private TextView actionButton(String text) {
        float density = getResources().getDisplayMetrics().density;
        TextView button = new TextView(context);
        button.setText(text);
        button.setTextColor(0xFFFFFFFF);
        button.setTextSize(14);
        button.setTypeface(null, android.graphics.Typeface.BOLD);
        button.setSingleLine(true);
        button.setPadding((int) (14 * density), (int) (7 * density), (int) (14 * density), (int) (7 * density));
        button.setBackgroundResource(R.drawable.channel_button);
        button.setClickable(true);
        button.setFocusable(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.rightMargin = (int) (8 * density);
        button.setLayoutParams(params);
        return button;
    }

    private void toggleSubscribe() {
        if (channel == null) return;
        LocalLibrary library = LocalLibrary.getInstance();
        if (library.isSubscribed(channel.id)) {
            library.unsubscribe(channel.id);
            Toast.makeText(context, getString(R.string.unsubscribed, channel.title), Toast.LENGTH_SHORT).show();
        } else {
            library.subscribe(channel.id, channel.title, channel.thumbnail);
            Toast.makeText(context, getString(R.string.subscribed, channel.title), Toast.LENGTH_SHORT).show();
        }
        updateSubscribeButton();
        Compat.invalidateOptionsMenu(this);
    }

    private void updateSubscribeButton() {
        if (subscribeButton == null || channel == null) return;
        boolean subscribed = LocalLibrary.getInstance().isSubscribed(channel.id);
        subscribeButton.setSelected(subscribed);
        subscribeButton.setText(subscribed ? R.string.subscribed_short : R.string.subscribe);
    }

    /** Subscribe or unsubscribe on the yt2009 YouTube account */
    private void showYoutubeSubscribe() {
        if (channel == null) return;
        new AlertDialog.Builder(context)
                .setTitle(R.string.youtube_account)
                .setItems(new String[]{getString(R.string.subscribe), getString(R.string.unsubscribe)},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final boolean subscribe = which == 0;
                                final String id = channel.id;
                                VideoActions.runAccountAction(ChannelActivity.this,
                                        new VideoActions.AccountAction() {
                                            public void run(Yt2009Account account) throws Exception {
                                                account.setSubscribed(id, subscribe);
                                            }
                                        }, subscribe ? R.string.subscribed_youtube : R.string.unsubscribed_youtube);
                            }
                        }).show();
    }

    private LinearLayout actionRow;
    private TextView subscribeButton;

    private void setAdapterForView(AbsListView view, VideoAdapter adapter) {
        if (view instanceof ListView) {
            ((ListView) view).setAdapter(adapter);
        } else if (view instanceof GridView) {
            ((GridView) view).setAdapter(adapter);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel);
        io.github.gohoski.notpipe.ui.NavBar.attach(this, io.github.gohoski.notpipe.ui.NavBar.NONE);

        channelId = getIntent().getStringExtra("ID");
        channelLayout = (LinearLayout) findViewById(R.id.channel);
        banner = (ImageView) findViewById(R.id.banner);
        context = this;

        channelLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (channel == null) return;

                TextView text = new TextView(context);
                text.setText(getString(R.string.subscribers, NumberFormat.getNumberInstance().format(channel.subscriberCount))
                        + "\n\n" + channel.description);
                text.setPadding(15,15,15,15);

                ScrollView scroll = new ScrollView(context);
                scroll.addView(text);

                new AlertDialog.Builder(context).setTitle(channel.title).setView(scroll).show();
            }
        });

        new LoadChannelTask().execute(channelId);
    }

    private void setupTabs(final boolean hasForYou, final boolean hasLatestFallback) {
        tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setup();

        if (hasForYou) {
            tabHost.addTab(tabHost.newTabSpec("for_you").setIndicator(getString(R.string.for_you)).setContent(R.id.for_you));

            forYouList = (AbsListView) findViewById(R.id.for_you_list);
            findViewById(R.id.for_you_loading).setVisibility(View.GONE);
            forYouList.setVisibility(View.VISIBLE);

            forYouAdapter = new VideoAdapter(context, cardLayout(forYouList), forYouVideos, true);
            setAdapterForView(forYouList, forYouAdapter);

            forYouList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    ImageLoader.clearCache(); System.gc();
                    Intent intent = new Intent(ChannelActivity.this, VideoActivity.class);
                    intent.putExtra("ID", forYouVideos.get(position).id);
                    startActivity(intent);
                }
            });
        }

        tabHost.addTab(tabHost.newTabSpec("videos").setIndicator(getString(R.string.videos)).setContent(R.id.videos));

        sortingSpinner = (Spinner) findViewById(R.id.sorting_spinner);
        if (NotPipe.SDK < 11 && sortingSpinner != null) {
            sortingSpinner.setVisibility(View.GONE);
        } else {
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(context,
                    android.R.layout.simple_spinner_item,
                    new String[] {
                            context.getString(R.string.latest),
                            context.getString(R.string.popular)
                    });
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sortingSpinner.setAdapter(spinnerAdapter);
            sortingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (currentSort != position) {
                        currentSort = position;
                        new LoadVideosTask().execute(channelId);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                if ("videos".equals(tabId)) {
                    if (currentSort == 0 && hasLatestFallback && latestVideos != null) {
                        displayVideos(latestVideos);
                    } else {
                        if (currentSort == 0 && latestVideos != null) {
                            displayVideos(latestVideos);
                        } else if (currentSort == 1 && popularVideos != null) {
                            displayVideos(popularVideos);
                        } else {
                            new LoadVideosTask().execute(channelId);
                        }
                    }
                }
            }
        });

        if (!hasForYou) {
            tabHost.setCurrentTabByTag("videos");
            if (hasLatestFallback && latestVideos != null && currentSort == 0) {
                displayVideos(latestVideos);
            } else {
                new LoadVideosTask().execute(channelId);
            }
        }

        if (NotPipe.SDK < 11) {
            int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 33f, getResources().getDisplayMetrics());
            TabWidget widget = tabHost.getTabWidget();
            for (int i = 0; i < widget.getChildCount(); i++) {
                View child = widget.getChildAt(i);
                if (child != null && child.getLayoutParams() != null) {
                    child.getLayoutParams().height = height;
                }
            }
        }
    }

    private void displayVideos(final List<VideoInfo> fetchedVideos) {
        findViewById(R.id.videos_loading).setVisibility(View.GONE);
        TextView noVideosView = (TextView) findViewById(R.id.no_videos);
        videosList = (AbsListView) findViewById(R.id.videos_list);
        videosList.setVisibility(View.VISIBLE);
        if (noVideosView != null) {
            if (fetchedVideos == null || fetchedVideos.isEmpty()) {
                noVideosView.setVisibility(View.VISIBLE);
            } else {
                noVideosView.setVisibility(View.GONE);
            }
        }
        if (fetchedVideos != null) {
            videosAdapter = new VideoAdapter(context, cardLayout(videosList), fetchedVideos, true);
            setAdapterForView(videosList, videosAdapter);
            videosList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    ImageLoader.clearCache(); System.gc();
                    Intent intent = new Intent(ChannelActivity.this, VideoActivity.class);
                    intent.putExtra("ID", fetchedVideos.get(position).id);
                    startActivity(intent);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (forYouAdapter != null) {
            forYouAdapter.notifyDataSetChanged();
        }
        if (videosAdapter != null) {
            videosAdapter.notifyDataSetChanged();
        }
        if (channel != null && banner != null) {
            ImageLoader.loadImage(channel.banner, banner, true);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ImageLoader.clearCache();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_channel, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem subscribeItem = menu.findItem(R.id.action_subscribe);
        if (subscribeItem != null) {
            subscribeItem.setVisible(channel != null);
            subscribeItem.setTitle(channel != null && LocalLibrary.getInstance().isSubscribed(channel.id)
                    ? R.string.unsubscribe : R.string.subscribe);
        }
        MenuItem youtubeItem = menu.findItem(R.id.action_subscribe_youtube);
        if (youtubeItem != null) {
            youtubeItem.setVisible(channel != null && Yt2009Account.getInstance().isSignedIn());
        }
        MenuItem latestItem = menu.findItem(R.id.action_sort_latest);
        MenuItem popularItem = menu.findItem(R.id.action_sort_popular);
        if (latestItem != null) {
            latestItem.setVisible(NotPipe.SDK < 11 && currentSort != 0);
        }
        if (popularItem != null) {
            popularItem.setVisible(NotPipe.SDK < 11 && currentSort != 1);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_subscribe) {
            toggleSubscribe();
            return true;
        }
        if (item.getItemId() == R.id.action_channel_playlists) {
            VideoListActivity.open(this, VideoListActivity.SOURCE_CHANNEL_PLAYLISTS,
                    channel != null ? channel.id : channelId, getString(R.string.playlists));
            return true;
        }
        if (item.getItemId() == R.id.action_shorts) {
            Intent intent = new Intent(this, ShortsActivity.class);
            intent.putExtra(ShortsActivity.EXTRA_CHANNEL, channel != null ? channel.id : channelId);
            startActivity(intent);
            return true;
        }
        if (item.getItemId() == R.id.action_subscribe_youtube) {
            showYoutubeSubscribe();
            return true;
        }
        if (NotPipe.SDK < 11) {
            if (item.getItemId() == R.id.action_sort_latest) {
                if (currentSort != 0) {
                    currentSort = 0;
                    new LoadVideosTask().execute(channelId);
                }
                return true;
            } else if (item.getItemId() == R.id.action_sort_popular) {
                if (currentSort != 1) {
                    currentSort = 1;
                    new LoadVideosTask().execute(channelId);
                }
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyedFlag = true;
        if (isFinishing()) {
            ImageLoader.clearCache();
        }
        if (banner != null) {
            banner.setImageDrawable(null);
        }
    }

    private class LoadChannelTask extends AsyncTask<String, Void, Channel> {
        private boolean contentUnavailable;

        @Override
        protected Channel doInBackground(String... strings) {
            try {
                if (isCancelled()) return null;
                api = Manager.getInstance().getMetadata();
                channelApi = Manager.getInstance().getChannelApi();
                return api.getChannel(strings[0]);
            } catch (ContentUnavailableException e) {
                e.printStackTrace();
                contentUnavailable = true;
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(final Channel fetched) {
            if (isCancelled() || isDestroyedFlag) return;
            if (fetched == null) {
                findViewById(R.id.loading).setVisibility(View.GONE);
                if (contentUnavailable) {
                    Toast.makeText(context, R.string.content_unavailable, Toast.LENGTH_SHORT).show();
                }
                return;
            }
            channel = fetched;
            channelId = channel.id;
            Compat.invalidateOptionsMenu(ChannelActivity.this);

            ((TextView) findViewById(R.id.title)).setText(channel.title);
            setTitle(channel.title);
            addActionRow();
            ((TextView) findViewById(R.id.subscribers)).setText(getString(R.string.subscribers, Utils.formatNumber(context, channel.subscriberCount)));
            ((TextView) findViewById(R.id.description)).setText(channel.description);
            ImageLoader.loadImage(channel.thumbnail, ((ImageView) findViewById(R.id.thumbnail)), false);
            ImageLoader.loadImage(channel.banner, banner, false);

            // Immediately make the channel metadata and banner visible as requested
            channelLayout.setVisibility(View.VISIBLE);
            if (banner != null) {
                banner.setVisibility(View.VISIBLE);
            }

            if (channel.videos != null && !channel.videos.isEmpty()) {
                forYouVideos = channel.videos;
                findViewById(R.id.loading).setVisibility(View.GONE);
                setupTabs(true, false);
                if (tabHost != null) {
                    tabHost.setVisibility(View.VISIBLE);
                }
            } else {
                new LoadChannelVideosFallbackTask().execute(channelId, channel.title);
            }
        }
    }

    private class LoadChannelVideosFallbackTask extends AsyncTask<String, Void, Integer> {
        @Override
        protected void onPreExecute() {
            findViewById(R.id.loading).setVisibility(View.VISIBLE);
            // Kept visible so metadata and banner do not disappear when fetching videos
            channelLayout.setVisibility(View.VISIBLE);
            if (banner != null) {
                banner.setVisibility(View.VISIBLE);
            }
            if (tabHost != null) {
                tabHost.setVisibility(View.GONE);
            }
        }

        @Override
        protected Integer doInBackground(String... strings) {
            String id = strings[0];
            String title = strings[1];

            List<VideoInfo> videos = null;
            try {
                videos = api.getChannelVideos(id, 0);
            } catch (Exception e) {
                metadataGetVideosFailed = true;
            }

            if (videos != null && !videos.isEmpty()) {
                latestVideos = videos;
                metadataGetVideosFailed = false;
                return 0;
            }

            metadataGetVideosFailed = true;
            try {
                List<VideoInfo> searchResults = api.search(title);
                List<VideoInfo> filtered = new ArrayList<VideoInfo>();
                for (int i = 0; i < searchResults.size(); i++) {
                    VideoInfo video = searchResults.get(i);
                    if (id.equals(video.channelId)) {
                        filtered.add(video);
                    }
                }
                if (!filtered.isEmpty()) {
                    forYouVideos = filtered;
                    return 1;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return 2;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (isCancelled() || isDestroyedFlag) return;
            findViewById(R.id.loading).setVisibility(View.GONE);
            channelLayout.setVisibility(View.VISIBLE);

            if (result == 0) {
                setupTabs(false, true);
            } else if (result == 1) {
                setupTabs(true, false);
            } else {
                setupTabs(false, false);
            }

            if (tabHost != null) {
                tabHost.setVisibility(View.VISIBLE);
            }
        }
    }

    private class LoadVideosTask extends AsyncTask<String, Void, List<VideoInfo>> {
        @Override
        protected void onPreExecute() {
            findViewById(R.id.videos_loading).setVisibility(View.VISIBLE);
            AbsListView vList = (AbsListView) findViewById(R.id.videos_list);
            if (vList != null) {
                vList.setVisibility(View.GONE);
            }
            View noVideosView = findViewById(R.id.no_videos);
            if (noVideosView != null) {
                noVideosView.setVisibility(View.GONE);
            }
        }

        @Override
        protected List<VideoInfo> doInBackground(String... strings) {
            String id = strings[0];
            List<VideoInfo> result = null;

            if (!metadataGetVideosFailed) {
                try {
                    result = api.getChannelVideos(id, currentSort);
                } catch (Exception e) {
                    metadataGetVideosFailed = true;
                }
                if (result == null || result.isEmpty()) {
                    metadataGetVideosFailed = true;
                }
            }

            if (metadataGetVideosFailed) {
                try {
                    result = channelApi.getChannelVideos(id, currentSort);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(final List<VideoInfo> fetchedVideos) {
            if (isCancelled() || isDestroyedFlag) return;

            if (currentSort == 0) {
                latestVideos = fetchedVideos;
            } else {
                popularVideos = fetchedVideos;
            }

            displayVideos(fetchedVideos);
        }
    }
}