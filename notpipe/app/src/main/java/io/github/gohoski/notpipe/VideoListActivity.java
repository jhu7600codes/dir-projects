package io.github.gohoski.notpipe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.gohoski.notpipe.api.ContentUnavailableException;
import io.github.gohoski.notpipe.api.Manager;
import io.github.gohoski.notpipe.api.Yt2009Account;
import io.github.gohoski.notpipe.data.Playlist;
import io.github.gohoski.notpipe.data.VideoInfo;
import io.github.gohoski.notpipe.ui.NavBar;
import io.github.gohoski.notpipe.ui.VideoActions;
import io.github.gohoski.notpipe.ui.VideoAdapter;
import io.github.gohoski.notpipe.util.Compat;
import io.github.gohoski.notpipe.util.Feeds;
import io.github.gohoski.notpipe.util.ImageLoader;
import io.github.gohoski.notpipe.util.LocalLibrary;

/**
 * Created by Claude on 25.10.2026.
 * Shows a list of videos from one source: a YouTube playlist, a local playlist, the local subscriptions feed
 * or one of the yt2009 account feeds. Every list can be played as audio (music mode) or downloaded.
 */

public class VideoListActivity extends Activity {
    public static final String EXTRA_SOURCE = "source";
    public static final String EXTRA_ARG = "arg";
    public static final String EXTRA_TITLE = "title";

    public static final String SOURCE_PLAYLIST = "playlist";
    public static final String SOURCE_LOCAL = "local";
    public static final String SOURCE_FEED = "feed";
    public static final String SOURCE_ACCOUNT_PLAYLIST = "account_playlist";
    public static final String SOURCE_RECOMMENDATIONS = "recommendations";
    public static final String SOURCE_WATCH_LATER = "watch_later";
    public static final String SOURCE_HISTORY = "history";
    public static final String SOURCE_FOR_YOU = "for_you";
    public static final String SOURCE_UPLOADS = "uploads";
    public static final String SOURCE_CHANNEL_PLAYLISTS = "channel_playlists";


    private String source, arg, title;
    private List<VideoInfo> videos = new ArrayList<VideoInfo>();
    private VideoAdapter adapter;
    private ListView listView;
    private Context context;
    private boolean isDestroyedFlag = false;
    private LoadTask loadTask;

    public static void open(Context context, String source, String arg, String title) {
        Intent intent = new Intent(context, VideoListActivity.class);
        intent.putExtra(EXTRA_SOURCE, source);
        intent.putExtra(EXTRA_ARG, arg);
        intent.putExtra(EXTRA_TITLE, title);
        context.startActivity(intent);
    }

    /**
     * Extracts a playlist ID from youtube.com/playlist?list=… or watch?v=…&list=… links, or a bare ID.
     * @return null if the text doesn't look like a playlist
     */
    public static String extractPlaylistId(String text) {
        if (text == null) return null;
        text = text.trim();
        int listIndex = text.indexOf("list=");
        if (listIndex >= 0 && (text.contains("youtube.com") || text.contains("youtu.be"))) {
            int end = text.indexOf('&', listIndex);
            return end >= 0 ? text.substring(listIndex + 5, end) : text.substring(listIndex + 5);
        }
        if (text.matches("(PL|OL|UU|FL|LL|RD)[a-zA-Z0-9_-]{10,}")) return text;
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        context = this;

        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri != null) {
            source = SOURCE_PLAYLIST;
            arg = uri.getQueryParameter("list");
            title = getString(R.string.playlist);
        } else {
            source = intent.getStringExtra(EXTRA_SOURCE);
            arg = intent.getStringExtra(EXTRA_ARG);
            title = intent.getStringExtra(EXTRA_TITLE);
        }
        if (source == null || (arg == null && (SOURCE_PLAYLIST.equals(source) || SOURCE_LOCAL.equals(source)
                || SOURCE_ACCOUNT_PLAYLIST.equals(source)))) {
            finish();
            return;
        }
        if (title != null) setTitle(title);
        if (SOURCE_FEED.equals(source)) NavBar.attach(this, NavBar.SUBSCRIPTIONS);

        listView = (ListView) findViewById(R.id.list);
        adapter = new VideoAdapter(this, R.layout.item_video_compact, videos);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                VideoActions.openItem(VideoListActivity.this, videos.get(position));
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (videos.get(position).id.contains(":")) return false;
                showItemDialog(position);
                return true;
            }
        });

        load();
    }

    private void load() {
        if (loadTask != null) loadTask.cancel(true);
        findViewById(R.id.loading).setVisibility(View.VISIBLE);
        findViewById(R.id.empty).setVisibility(View.GONE);
        loadTask = new LoadTask();
        loadTask.execute();
    }

    private void showItemDialog(final int position) {
        final VideoInfo video = videos.get(position);
        List<String> items = new ArrayList<String>();
        items.add(getString(R.string.play_audio_from_here));
        items.add(getString(R.string.download));
        items.add(getString(R.string.add_to_playlist));
        if (video.channelId != null && video.channelId.length() > 0) items.add(getString(R.string.channel));
        if (SOURCE_LOCAL.equals(source) || SOURCE_ACCOUNT_PLAYLIST.equals(source) || SOURCE_WATCH_LATER.equals(source)) {
            items.add(getString(R.string.remove_from_playlist));
        }
        final String[] array = items.toArray(new String[items.size()]);
        new AlertDialog.Builder(this)
                .setTitle(video.title)
                .setItems(array, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String item = array[which];
                        if (item.equals(getString(R.string.play_audio_from_here))) {
                            MusicService.play(context, videos, position);
                        } else if (item.equals(getString(R.string.download))) {
                            VideoActions.showDownloadDialog(VideoListActivity.this, video);
                        } else if (item.equals(getString(R.string.add_to_playlist))) {
                            VideoActions.showAddToPlaylistDialog(VideoListActivity.this, video);
                        } else if (item.equals(getString(R.string.channel))) {
                            Intent intent = new Intent(context, ChannelActivity.class);
                            intent.putExtra("ID", video.channelId);
                            startActivity(intent);
                        } else if (item.equals(getString(R.string.remove_from_playlist))) {
                            if (!SOURCE_LOCAL.equals(source)) {
                                final String playlistId = SOURCE_WATCH_LATER.equals(source) ? "WL" : arg;
                                VideoActions.runAccountAction(VideoListActivity.this, new VideoActions.AccountAction() {
                                    public void run(Yt2009Account account) throws Exception {
                                        account.removeFromPlaylist(playlistId, video.id);
                                    }
                                }, R.string.removed);
                            } else {
                                LocalLibrary.getInstance().removeFromPlaylist(arg, video.id);
                            }
                            videos.remove(position);
                            adapter.notifyDataSetChanged();
                            updateEmpty();
                        }
                    }
                }).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_list, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean remote = !SOURCE_LOCAL.equals(source) && !SOURCE_FEED.equals(source) && !SOURCE_HISTORY.equals(source)
                && !SOURCE_FOR_YOU.equals(source) && !SOURCE_CHANNEL_PLAYLISTS.equals(source);
        menu.findItem(R.id.action_save_playlist).setVisible(remote);
        menu.findItem(R.id.action_delete_playlist).setVisible(SOURCE_LOCAL.equals(source));
        menu.findItem(R.id.action_clear_history).setVisible(SOURCE_HISTORY.equals(source));
        boolean playable = !SOURCE_CHANNEL_PLAYLISTS.equals(source);
        menu.findItem(R.id.action_play_all).setVisible(playable);
        menu.findItem(R.id.action_shuffle).setVisible(playable);
        menu.findItem(R.id.action_download_all).setVisible(playable);
        MenuItem notify = menu.findItem(R.id.action_notify_uploads);
        notify.setVisible(SOURCE_FEED.equals(source));
        notify.setTitle(NewVideosService.isEnabled(this) ? R.string.notifications_off : R.string.notifications_on);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            load();
            return true;
        }
        if (videos.isEmpty() && id != R.id.action_delete_playlist && id != R.id.action_clear_history
                && id != R.id.action_notify_uploads) {
            Toast.makeText(this, R.string.no_videos, Toast.LENGTH_SHORT).show();
            return true;
        }
        if (id == R.id.action_play_all) {
            MusicService.play(this, videos, 0);
            Toast.makeText(this, R.string.playing_in_background, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_shuffle) {
            List<VideoInfo> shuffled = new ArrayList<VideoInfo>(videos);
            Collections.shuffle(shuffled);
            MusicService.play(this, shuffled, 0);
            Toast.makeText(this, R.string.playing_in_background, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_download_all) {
            VideoActions.downloadAllAudio(this, videos);
            return true;
        } else if (id == R.id.action_save_playlist) {
            savePlaylistLocally();
            return true;
        } else if (id == R.id.action_notify_uploads) {
            boolean enable = !NewVideosService.isEnabled(this);
            NewVideosService.setEnabled(this, enable);
            Toast.makeText(this, enable ? R.string.notifications_enabled : R.string.notifications_disabled, Toast.LENGTH_SHORT).show();
            Compat.invalidateOptionsMenu(this);
            return true;
        } else if (id == R.id.action_clear_history) {
            LocalLibrary.getInstance().clearHistory();
            videos.clear();
            adapter.notifyDataSetChanged();
            updateEmpty();
            return true;
        } else if (id == R.id.action_delete_playlist) {
            new AlertDialog.Builder(this)
                    .setTitle(arg)
                    .setMessage(R.string.delete_playlist_confirm)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LocalLibrary.getInstance().deletePlaylist(arg);
                            finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void savePlaylistLocally() {
        LocalLibrary library = LocalLibrary.getInstance();
        String base = title == null || title.length() == 0 ? getString(R.string.playlist) : title;
        String name = base;
        for (int i = 2; !library.createPlaylist(name); i++) {
            name = base + " (" + i + ")";
        }
        for (int i = 0; i < videos.size(); i++) {
            library.addToPlaylist(name, videos.get(i));
        }
        Toast.makeText(this, getString(R.string.playlist_saved, name), Toast.LENGTH_SHORT).show();
    }

    private void updateEmpty() {
        TextView empty = (TextView) findViewById(R.id.empty);
        if (SOURCE_FEED.equals(source) && LocalLibrary.getInstance().getSubscriptions().isEmpty()) {
            empty.setText(R.string.no_subscriptions);
        } else if (SOURCE_FOR_YOU.equals(source)) {
            empty.setText(R.string.no_recommendations);
        } else {
            empty.setText(R.string.no_videos);
        }
        empty.setVisibility(videos.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) adapter.notifyDataSetChanged();
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
        if (loadTask != null) loadTask.cancel(true);
        if (isFinishing()) ImageLoader.clearCache();
    }

    private class LoadTask extends AsyncTask<Void, Void, List<VideoInfo>> {
        private Exception error;
        private String loadedTitle;

        @Override
        protected List<VideoInfo> doInBackground(Void... params) {
            try {
                if (SOURCE_LOCAL.equals(source)) {
                    return LocalLibrary.getInstance().getPlaylistVideos(arg);
                } else if (SOURCE_FEED.equals(source)) {
                    return Feeds.loadSubscriptions();
                } else if (SOURCE_HISTORY.equals(source)) {
                    return LocalLibrary.getInstance().getHistory();
                } else if (SOURCE_FOR_YOU.equals(source)) {
                    return Feeds.loadRecommendations();
                } else if (SOURCE_CHANNEL_PLAYLISTS.equals(source)) {
                    return io.github.gohoski.notpipe.api.Discovery.channelPlaylists(arg);
                } else if (SOURCE_UPLOADS.equals(source)) {
                    return Yt2009Account.getInstance().getUploads();
                } else if (SOURCE_RECOMMENDATIONS.equals(source)) {
                    return Yt2009Account.getInstance().getRecommendations();
                } else if (SOURCE_WATCH_LATER.equals(source)) {
                    return Yt2009Account.getInstance().getWatchLater();
                } else if (SOURCE_ACCOUNT_PLAYLIST.equals(source)) {
                    return Yt2009Account.getInstance().getPlaylist(arg, title).videos;
                } else {
                    Playlist playlist = Manager.getInstance().getPlaylistApi().getPlaylist(arg);
                    loadedTitle = playlist.title;
                    return playlist.videos;
                }
            } catch (Exception e) {
                e.printStackTrace();
                error = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<VideoInfo> result) {
            if (isCancelled() || isDestroyedFlag) return;
            findViewById(R.id.loading).setVisibility(View.GONE);
            if (result == null) {
                Toast.makeText(context, error instanceof ContentUnavailableException ? getString(R.string.content_unavailable)
                        : getString(R.string.load_failed), Toast.LENGTH_SHORT).show();
                result = new ArrayList<VideoInfo>();
            }
            if (loadedTitle != null && loadedTitle.length() > 0) {
                title = loadedTitle;
                setTitle(title);
            }
            videos.clear();
            videos.addAll(result);
            adapter.notifyDataSetChanged();
            updateEmpty();
        }
    }
}
