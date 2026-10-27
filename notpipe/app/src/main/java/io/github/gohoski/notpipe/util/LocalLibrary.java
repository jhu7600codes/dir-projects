package io.github.gohoski.notpipe.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import io.github.gohoski.notpipe.NotPipe;
import io.github.gohoski.notpipe.data.Channel;
import io.github.gohoski.notpipe.data.VideoInfo;

/**
 * Created by Claude on 25.10.2026.
 * Local account (NewPipe-like): channel subscriptions and playlists stored on the device only.
 * Everything is kept as JSON in SharedPreferences, so it survives instance changes and needs no network.
 */

public class LocalLibrary {
    private static final String PREFS_NAME = "notPipe_library";
    private static final String KEY_SUBSCRIPTIONS = "subscriptions";
    private static final String KEY_PLAYLISTS = "playlists";
    private static final String KEY_HISTORY = "history";
    private static final int MAX_HISTORY = 200;

    private static LocalLibrary instance;
    private SharedPreferences prefs;
    private JSONArray subscriptions;
    private JSONArray playlists;
    private JSONArray history;

    private LocalLibrary(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        subscriptions = load(KEY_SUBSCRIPTIONS);
        playlists = load(KEY_PLAYLISTS);
        history = load(KEY_HISTORY);
    }

    public static synchronized LocalLibrary getInstance() {
        if (instance == null) {
            instance = new LocalLibrary(NotPipe.getAppContext());
        }
        return instance;
    }

    /* ---------- Subscriptions ---------- */

    public synchronized boolean isSubscribed(String channelId) {
        return indexOfChannel(channelId) >= 0;
    }

    public synchronized void subscribe(String channelId, String title, String thumbnail) {
        if (channelId == null || channelId.length() == 0 || isSubscribed(channelId)) return;
        JSONObject channel = new JSONObject();
        channel.put("id", channelId);
        channel.put("title", title == null ? channelId : title);
        channel.put("thumbnail", thumbnail == null ? "" : thumbnail);
        subscriptions.add(channel);
        save(KEY_SUBSCRIPTIONS, subscriptions);
    }

    public synchronized void unsubscribe(String channelId) {
        int i = indexOfChannel(channelId);
        if (i >= 0) {
            subscriptions.remove(i);
            save(KEY_SUBSCRIPTIONS, subscriptions);
        }
    }

    /**
     * @return number of channels that were not subscribed before
     */
    public synchronized int importSubscriptions(List<Channel> channels) {
        int added = 0;
        for (int i = 0; i < channels.size(); i++) {
            Channel c = channels.get(i);
            if (!isSubscribed(c.id)) {
                subscribe(c.id, c.title, c.thumbnail);
                added++;
            }
        }
        return added;
    }

    /**
     * Imports subscriptions from a Google Takeout "subscriptions.csv" (Channel Id,Channel Url,Channel Title)
     * or a NewPipe export ({"subscriptions":[{"url":"…/channel/UC…","name":"…"}]}).
     * @return number of newly added channels, -1 if the format wasn't recognized
     */
    public int importSubscriptionsFile(String content) {
        List<Channel> channels = new ArrayList<Channel>();
        String trimmed = content.trim();
        if (trimmed.startsWith("{")) {
            try {
                JSONArray arr = JSON.getObject(trimmed).getArray("subscriptions");
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject j = arr.getObject(i);
                    String url = j.getString("url", "");
                    int at = url.indexOf("/channel/");
                    if (at < 0) continue;
                    String id = url.substring(at + 9).split("[/?]")[0];
                    channels.add(new Channel(id, j.getString("name", id), "", "", "", -1, null));
                }
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        } else {
            String[] lines = trimmed.split("\r?\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (!line.startsWith("UC")) continue; // skips the header
                String[] cols = line.split(",", 3);
                String title = cols.length > 2 ? cols[2].trim() : cols[0];
                if (title.startsWith("\"") && title.endsWith("\"") && title.length() > 1) {
                    title = title.substring(1, title.length() - 1).replace("\"\"", "\"");
                }
                channels.add(new Channel(cols[0].trim(), title, "", "", "", -1, null));
            }
        }
        if (channels.isEmpty()) return -1;
        return importSubscriptions(channels);
    }

    public synchronized List<Channel> getSubscriptions() {
        List<Channel> result = new ArrayList<Channel>();
        for (int i = 0; i < subscriptions.size(); i++) {
            JSONObject j = subscriptions.getObject(i);
            result.add(new Channel(j.getString("id"), j.getString("title", ""), j.getString("thumbnail", ""),
                    "", "", -1, null));
        }
        return result;
    }

    private int indexOfChannel(String channelId) {
        if (channelId == null) return -1;
        for (int i = 0; i < subscriptions.size(); i++) {
            if (channelId.equals(subscriptions.getObject(i).getString("id", null))) return i;
        }
        return -1;
    }

    /* ---------- Playlists ---------- */

    public synchronized List<String> getPlaylistNames() {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < playlists.size(); i++) {
            result.add(playlists.getObject(i).getString("name", ""));
        }
        return result;
    }

    public synchronized int getPlaylistSize(String name) {
        JSONObject p = findPlaylist(name);
        return p == null ? 0 : p.getArray("videos").size();
    }

    /**
     * @return false if a playlist with this name already exists
     */
    public synchronized boolean createPlaylist(String name) {
        if (name == null || name.trim().length() == 0 || findPlaylist(name) != null) return false;
        JSONObject p = new JSONObject();
        p.put("name", name.trim());
        p.put("videos", new JSONArray());
        playlists.add(p);
        save(KEY_PLAYLISTS, playlists);
        return true;
    }

    public synchronized void deletePlaylist(String name) {
        for (int i = 0; i < playlists.size(); i++) {
            if (name.equals(playlists.getObject(i).getString("name", null))) {
                playlists.remove(i);
                save(KEY_PLAYLISTS, playlists);
                return;
            }
        }
    }

    /**
     * @return false if the video is already in the playlist
     */
    public synchronized boolean addToPlaylist(String name, VideoInfo video) {
        JSONObject p = findPlaylist(name);
        if (p == null) {
            createPlaylist(name);
            p = findPlaylist(name);
        }
        JSONArray videos = p.getArray("videos");
        for (int i = 0; i < videos.size(); i++) {
            if (video.id.equals(videos.getObject(i).getString("id", null))) return false;
        }
        videos.add(toJson(video));
        save(KEY_PLAYLISTS, playlists);
        return true;
    }

    public synchronized void removeFromPlaylist(String name, String videoId) {
        JSONObject p = findPlaylist(name);
        if (p == null) return;
        JSONArray videos = p.getArray("videos");
        for (int i = 0; i < videos.size(); i++) {
            if (videoId.equals(videos.getObject(i).getString("id", null))) {
                videos.remove(i);
                save(KEY_PLAYLISTS, playlists);
                return;
            }
        }
    }

    public synchronized List<VideoInfo> getPlaylistVideos(String name) {
        JSONObject p = findPlaylist(name);
        return p == null ? new ArrayList<VideoInfo>() : toVideos(p.getArray("videos"));
    }

    /* ---------- Watch history ---------- */

    /** Moves the video to the top of the history (newest first) */
    public synchronized void addToHistory(VideoInfo video) {
        for (int i = 0; i < history.size(); i++) {
            if (video.id.equals(history.getObject(i).getString("id", null))) {
                history.remove(i);
                break;
            }
        }
        JSONArray updated = new JSONArray();
        updated.add(toJson(video));
        for (int i = 0; i < history.size() && updated.size() < MAX_HISTORY; i++) {
            updated.add(history.getObject(i));
        }
        history = updated;
        save(KEY_HISTORY, history);
    }

    public synchronized List<VideoInfo> getHistory() {
        return toVideos(history);
    }

    public synchronized void clearHistory() {
        history = new JSONArray();
        save(KEY_HISTORY, history);
    }

    private static JSONObject toJson(VideoInfo video) {
        JSONObject v = new JSONObject();
        v.put("id", video.id);
        v.put("title", video.title == null ? "" : video.title);
        v.put("thumbnail", video.thumbnail == null ? "" : video.thumbnail);
        v.put("channel", video.channel == null ? "" : video.channel);
        v.put("channelId", video.channelId == null ? "" : video.channelId);
        v.put("duration", video.duration == null ? "" : video.duration);
        return v;
    }

    private static List<VideoInfo> toVideos(JSONArray videos) {
        List<VideoInfo> result = new ArrayList<VideoInfo>();
        for (int i = 0; i < videos.size(); i++) {
            JSONObject j = videos.getObject(i);
            result.add(new VideoInfo(j.getString("id"), j.getString("title", ""), j.getString("thumbnail", ""),
                    j.getString("channel", ""), "", j.getString("channelId", ""), j.getString("duration", ""), -1, null));
        }
        return result;
    }

    private JSONObject findPlaylist(String name) {
        if (name == null) return null;
        for (int i = 0; i < playlists.size(); i++) {
            JSONObject p = playlists.getObject(i);
            if (name.equals(p.getString("name", null))) return p;
        }
        return null;
    }

    private JSONArray load(String key) {
        String raw = prefs.getString(key, "[]");
        try {
            return JSON.getArray(raw);
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONArray();
        }
    }

    private void save(String key, JSONArray value) {
        prefs.edit().putString(key, value.toString()).commit();
    }
}
