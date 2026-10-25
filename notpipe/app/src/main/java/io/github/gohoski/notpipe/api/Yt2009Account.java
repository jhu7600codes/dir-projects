package io.github.gohoski.notpipe.api;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import io.github.gohoski.notpipe.NotPipe;
import io.github.gohoski.notpipe.data.Channel;
import io.github.gohoski.notpipe.data.Playlist;
import io.github.gohoski.notpipe.data.VideoInfo;
import io.github.gohoski.notpipe.http.HttpClient;
import io.github.gohoski.notpipe.http.HttpException;
import io.github.gohoski.notpipe.http.HttpRequest;

/**
 * Created by Claude on 25.10.2026.
 * YouTube account authorization through a yt2009 instance ("mobile helper").
 * The flow is the same one yt2009's patched APKs use:
 * 1. the app picks a 7-character device code and opens /gsign?device=CODE (with an Android User-Agent) to start a session;
 * 2. the user opens http://instance/gsign on a PC, enters the code and signs in with Google there;
 * 3. after that, every request with the header X-GData-Device: device-id="CODE" is made as that YouTube account.
 * Only sign in through instances you trust: the instance owner gets access to your YouTube account.
 */

public class Yt2009Account {
    private static final String PREFS_NAME = "notPipe_account";
    private static final String KEY_INSTANCE = "instance";
    private static final String KEY_DEVICE = "device";
    private static final String KEY_LINKED = "linked";
    private static final String DEVICE_CHARS = "qwertyuiopasdfghjklzxcvbnm1234567890";

    private static Yt2009Account instance;
    private SharedPreferences prefs;

    private Yt2009Account(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static Yt2009Account getInstance() {
        if (instance == null) {
            instance = new Yt2009Account(NotPipe.getAppContext());
        }
        return instance;
    }

    public boolean isSignedIn() {
        return prefs.getBoolean(KEY_LINKED, false) && getInstanceUrl() != null;
    }

    public String getInstanceUrl() {
        return prefs.getString(KEY_INSTANCE, null);
    }

    public String getDeviceCode() {
        return prefs.getString(KEY_DEVICE, null);
    }

    public String getInstanceHost() {
        String url = getInstanceUrl();
        return url == null ? "" : url.replace("https://", "").replace("http://", "");
    }

    /**
     * Starts a sign-in session on the instance.
     * @param baseUrl yt2009 instance URL
     * @return the device code the user has to enter on http://instance/gsign
     */
    public String startSignIn(String baseUrl) throws IOException {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        while (sb.length() < 7) {
            sb.append(DEVICE_CHARS.charAt(random.nextInt(DEVICE_CHARS.length())));
        }
        String device = sb.toString();
        HttpRequest req = new HttpRequest(baseUrl, "/gsign?device=" + device);
        // yt2009 only registers the session for mobile User-Agents
        req.addHeader("User-Agent", HttpClient.USER_AGENT + " Android");
        HttpClient.executeToString(req);
        prefs.edit()
                .putString(KEY_INSTANCE, baseUrl)
                .putString(KEY_DEVICE, device)
                .putBoolean(KEY_LINKED, false)
                .commit();
        return device;
    }

    /**
     * Checks whether the user has finished signing in on the PC.
     * @return true if the device code is now linked to a YouTube account
     */
    public boolean checkLinked() throws IOException {
        String baseUrl = getInstanceUrl();
        if (baseUrl == null || getDeviceCode() == null) return false;
        boolean linked;
        try {
            HttpClient.executeToString(authorized("/pull_pchelper"));
            linked = true;
        } catch (HttpException e) {
            if (e.getStatusCode() != 401) throw e;
            linked = false;
        }
        prefs.edit().putBoolean(KEY_LINKED, linked).commit();
        return linked;
    }

    public void signOut() {
        final String baseUrl = getInstanceUrl();
        final String device = getDeviceCode();
        prefs.edit().clear().commit();
        if (baseUrl == null || device == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpClient.executeToString(new HttpRequest(baseUrl, "/unlinkhelper?device=" + device));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public List<VideoInfo> getRecommendations() throws IOException {
        return GdataParser.parseVideos(getInstanceUrl(),
                HttpClient.executeToString(authorized("/feeds/api/users/default/recommendations"), HttpClient.VIDEO_TIMEOUT));
    }

    public List<VideoInfo> getWatchLater() throws IOException {
        return GdataParser.parseVideos(getInstanceUrl(),
                HttpClient.executeToString(authorized("/feeds/api/users/default/watch_later"), HttpClient.VIDEO_TIMEOUT));
    }

    public List<Channel> getSubscriptions() throws IOException {
        return GdataParser.parseSubscriptions(getInstanceUrl(),
                HttpClient.executeToString(authorized("/feeds/api/users/default/subscriptions"), HttpClient.VIDEO_TIMEOUT));
    }

    public List<Playlist> getPlaylists() throws IOException {
        return GdataParser.parsePlaylists(getInstanceUrl(),
                HttpClient.executeToString(authorized("/feeds/api/users/default/playlists"), HttpClient.VIDEO_TIMEOUT));
    }

    /** Works for private playlists too since it's requested as the signed-in user */
    public Playlist getPlaylist(String id, String title) throws IOException {
        String xml = HttpClient.executeToString(authorized("/feeds/api/users/you/playlists/" + id), HttpClient.VIDEO_TIMEOUT);
        List<VideoInfo> videos = GdataParser.parseVideos(getInstanceUrl(), xml);
        return new Playlist(id, title, "", videos.isEmpty() ? "" : videos.get(0).thumbnail, videos.size(), videos);
    }

    private HttpRequest authorized(String endpoint) {
        HttpRequest req = new HttpRequest(getInstanceUrl(), endpoint);
        req.addHeader("X-GData-Device", "device-id=\"" + getDeviceCode() + "\"");
        return req;
    }
}
