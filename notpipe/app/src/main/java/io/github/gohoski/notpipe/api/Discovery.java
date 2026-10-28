package io.github.gohoski.notpipe.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import io.github.gohoski.notpipe.NotPipe;
import io.github.gohoski.notpipe.R;
import io.github.gohoski.notpipe.Utils;
import io.github.gohoski.notpipe.config.ConfigManager;
import io.github.gohoski.notpipe.data.VideoInfo;
import io.github.gohoski.notpipe.http.HttpClient;
import io.github.gohoski.notpipe.http.HttpRequest;

/**
 * Created by Claude on 28.10.2026.
 * Channel and playlist search plus channel playlists, through Invidious.
 * Results are VideoInfo rows so the existing lists can show them; their IDs carry a prefix
 * ({@link #CHANNEL_PREFIX} / {@link #PLAYLIST_PREFIX}) so a click can open the right screen.
 */

public class Discovery {
    public static final String CHANNEL_PREFIX = "channel:";
    public static final String PLAYLIST_PREFIX = "playlist:";

    private Discovery() {}

    public static List<VideoInfo> searchChannels(String query) throws IOException {
        return run("/api/v1/search?type=channel&q=" + encode(query), true);
    }

    public static List<VideoInfo> searchPlaylists(String query) throws IOException {
        return run("/api/v1/search?type=playlist&q=" + encode(query), false);
    }

    public static List<VideoInfo> channelPlaylists(String channelId) throws IOException {
        return run("/api/v1/channels/" + channelId + "/playlists", false);
    }

    private static List<VideoInfo> run(String endpoint, boolean channels) throws IOException {
        String[] result = fetchFromInvidious(endpoint);
        String base = result[0], body = result[1];
        try {
            JSONArray arr = body.trim().startsWith("{") ? JSON.getObject(body).getArray("playlists") : JSON.getArray(body);
            return channels ? parseChannels(base, arr) : parsePlaylists(base, arr);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    private static volatile String lastGood;
    private static final java.util.Set<String> FAILED = java.util.Collections.synchronizedSet(new java.util.HashSet<String>());
    private static final java.util.concurrent.ExecutorService POOL = java.util.concurrent.Executors.newCachedThreadPool();

    /**
     * Sends the request to several Invidious instances at once (many public ones are dead or rate-limited)
     * and returns the first JSON answer that isn't an error.
     * @return {instance base URL, response body}
     */
    public static String[] fetchFromInvidious(final String endpoint) throws IOException {
        List<String> instances = new ArrayList<String>(ConfigManager.getInstance().getConfig().getInvidiousInstances());
        if (instances.isEmpty()) throw new IOException("Needs an Invidious instance");
        java.util.Collections.shuffle(instances);
        // Instances that failed lately go last, the last one that worked goes first
        List<String> failed = new ArrayList<String>();
        for (int i = instances.size() - 1; i >= 0; i--) {
            if (FAILED.contains(instances.get(i))) failed.add(instances.remove(i));
        }
        instances.addAll(failed);
        if (lastGood != null && instances.remove(lastGood)) instances.add(0, lastGood);
        java.util.concurrent.CompletionService<String[]> race = new java.util.concurrent.ExecutorCompletionService<String[]>(POOL);
        int next = 0, running = 0;
        final int width = 6;
        while (next < instances.size() && running < width) {
            submit(race, instances.get(next++), endpoint);
            running++;
        }
        long deadline = System.currentTimeMillis() + 45000;
        IOException last = new IOException("All instances failed");
        while (running > 0) {
            long left = deadline - System.currentTimeMillis();
            if (left <= 0) break;
            java.util.concurrent.Future<String[]> done;
            try {
                done = race.poll(left, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                break;
            }
            if (done == null) break;
            running--;
            try {
                String[] result = done.get();
                lastGood = result[0];
                FAILED.remove(result[0]);
                return result;
            } catch (Exception e) {
                last = new IOException(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            }
            if (next < instances.size()) {
                submit(race, instances.get(next++), endpoint);
                running++;
            }
        }
        throw last;
    }

    private static void submit(java.util.concurrent.CompletionService<String[]> race, final String base, final String endpoint) {
        race.submit(new java.util.concurrent.Callable<String[]>() {
            @Override
            public String[] call() throws Exception {
                try {
                    String body = HttpClient.executeToString(new HttpRequest(base, endpoint)).trim();
                    if (!(body.startsWith("[") || body.startsWith("{")) || body.startsWith("{\"error\"")) {
                        throw new IOException("Bad response from " + base);
                    }
                    return new String[]{base, body};
                } catch (IOException e) {
                    FAILED.add(base);
                    throw e;
                }
            }
        });
    }

    private static List<VideoInfo> parseChannels(String base, JSONArray arr) {
        List<VideoInfo> result = new ArrayList<VideoInfo>();
        for (int i = 0; i < arr.size(); i++) {
            JSONObject j = arr.getObject(i);
            if (!"channel".equals(j.getString("type", ""))) continue;
            String avatar = "";
            try {
                JSONArray thumbs = j.getArray("authorThumbnails");
                avatar = image(base + "/ggpht", thumbs.getObject(thumbs.size() - 1).getString("url"));
            } catch (Exception ignored) {}
            String subs = NotPipe.getAppContext().getString(R.string.subscribers,
                    Utils.formatNumber(NotPipe.getAppContext(), j.getLong("subCount", 0)));
            result.add(new VideoInfo(CHANNEL_PREFIX + j.getString("authorId"), j.getString("author"), avatar,
                    subs, avatar, j.getString("authorId"), "", -1, null));
        }
        return result;
    }

    private static List<VideoInfo> parsePlaylists(String base, JSONArray arr) {
        List<VideoInfo> result = new ArrayList<VideoInfo>();
        for (int i = 0; i < arr.size(); i++) {
            JSONObject j = arr.getObject(i);
            if (!"playlist".equals(j.getString("type", "playlist"))) continue;
            String thumb = j.getString("playlistThumbnail", "");
            if (thumb.startsWith("/")) thumb = image(base, thumb);
            int count = j.getInt("videoCount", 0);
            result.add(new VideoInfo(PLAYLIST_PREFIX + j.getString("playlistId"), j.getString("title"), thumb,
                    j.getString("author", "") + " · " + NotPipe.getAppContext().getResources()
                            .getQuantityString(R.plurals.video_count_short, count, count),
                    "", j.getString("authorId", ""), "", -1, null));
        }
        return result;
    }

    private static String image(String base, String url) {
        if (url == null || url.length() == 0) return "";
        if (url.startsWith("//")) url = "https:" + url;
        return Utils.parseUrl(base, url);
    }

    private static String encode(String s) throws IOException {
        return java.net.URLEncoder.encode(s, "UTF-8");
    }
}
