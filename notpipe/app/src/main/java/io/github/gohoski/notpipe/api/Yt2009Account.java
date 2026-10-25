package io.github.gohoski.notpipe.api;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String KEY_TOKEN = "token";
    private static final Pattern FLOW_TOKEN = Pattern.compile("name=\"flow-token\" value=\"([a-z]{24})\"");
    private static final Pattern UPLOADED_ID = Pattern.compile("(?:watch\\?v=|/vi/|video_id=)([a-zA-Z0-9_-]{11})");
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

    /** Google's Android device setup sign-in page, the one yt2009's gsign instructions use */
    public static final String GOOGLE_SIGN_IN_URL = "https://accounts.google.com/embedded/setup/v2/android"
            + "?source=com.google.android.gms&xoauth_display_name=Android%20Phone&lang=en&langCountry=en_us&hl=en-US"
            + "&cc=us&multilogin=1&hide_status_bar=0&use_native_navigation=0&alignment=center";

    /**
     * On-device version of yt2009's gsign: hands the oauth2_4 token that Google's sign-in page sets as the
     * "oauth_token" cookie to the instance (what the PC page asks you to copy from the dev tools).
     * @param token token with or without the "oauth2_4/" prefix
     */
    public void submitGoogleToken(String email, String token) throws IOException {
        if (token.startsWith("oauth2_4/")) token = token.substring(9);
        int quote = token.indexOf('"');
        if (quote >= 0) token = token.substring(0, quote);
        String reversed = new StringBuilder(token).reverse().toString();
        String scrambled = "\u0001" + reversed.substring(0, 4) + "\u0001" + reversed.substring(4);
        HttpURLConnection connection = open("/gsign_submit", HttpClient.CONVERSION_TIMEOUT);
        connection.setRequestProperty("device", getDeviceCode());
        connection.setRequestProperty("Content-Type", "text/plain");
        byte[] body = (email.trim() + "\u0000" + scrambled).getBytes("UTF-8");
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(body.length);
        OutputStream out = connection.getOutputStream();
        out.write(body);
        out.close();
        readResponse(connection);
        checkLinked();
    }

    /** yt2009's page for picking which channel (brand account) of the Google account to use */
    public String getAccountPickerUrl() {
        return getInstanceUrl() + "/mobile/mobilehelper/account_picker.html?device=" + getDeviceCode();
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
        List<VideoInfo> videos = GdataParser.parseVideos(getInstanceUrl(),
                HttpClient.executeToString(authorized("/feeds/api/users/default/recommendations"), HttpClient.VIDEO_TIMEOUT));
        // yt2009 puts a fake "complete your sign in" entry here when the device isn't linked
        for (int i = videos.size() - 1; i >= 0; i--) {
            if ("12345678911".equals(videos.get(i).id)) videos.remove(i);
        }
        return videos;
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

    /** Your own uploaded videos */
    public List<VideoInfo> getUploads() throws IOException {
        return GdataParser.parseVideos(getInstanceUrl(),
                HttpClient.executeToString(authorized("/feeds/api/users/default/uploads"), HttpClient.VIDEO_TIMEOUT));
    }

    /* ---------- Write actions (GData POSTs the old YouTube apps used) ---------- */

    public void addToPlaylist(String playlistId, String videoId) throws IOException {
        post("/feeds/api/users/you/playlists/" + playlistId, "<entry><id>" + videoId + "</id></entry>", ATOM);
    }

    public void removeFromPlaylist(String playlistId, String videoId) throws IOException {
        post("/pchelper_playlists", "method=remove_videos&playlist_id=" + playlistId + "&video_ids=" + videoId, FORM);
    }

    public void addToWatchLater(String videoId) throws IOException {
        post("/feeds/api/users/default/watch_later", "<entry><id>" + videoId + "</id></entry>", ATOM);
    }

    /**
     * @param videoId optional first video of the playlist, may be null
     */
    public void createPlaylist(String name, String videoId, boolean isPrivate) throws IOException {
        String clean = name.replace("<", "").replace(">", "").replace("&", "");
        post("/feeds/api/users/default/playlists", "<entry><title type='text'>" + clean + "</title>"
                + (videoId != null ? "<yt:videoid>" + videoId + "</yt:videoid>" : "")
                + (isPrivate ? "<yt:private/>" : "") + "</entry>", ATOM);
    }

    /** @param like true = like, false = dislike */
    public void rate(String videoId, boolean like) throws IOException {
        post("/feeds/api/videos/" + videoId + "/ratings",
                "<entry><yt:rating value='" + (like ? "like" : "dislike") + "'/></entry>", ATOM);
    }

    /** @param channelId UC… channel ID or @handle without the @ */
    public void setSubscribed(String channelId, boolean subscribed) throws IOException {
        post("/feeds/api/users/default/subscriptions", "<entry><yt:username>" + channelId + "</yt:username>"
                + (subscribed ? "" : "<yt:unsubscribe/>") + "</entry>", ATOM);
    }

    /* ---------- Upload (yt2009's /my_videos_upload web flow) ---------- */

    public interface UploadProgressListener {
        void onProgress(long sent, long total);
    }

    /** Thrown when the instance only lets in visitors with an access token */
    public static class TokenRequiredException extends IOException {
        public TokenRequiredException() {
            super("Instance access token required");
        }
    }

    /** Access token for instances that restrict their web UI (yt2009 "tokens" config), may be null */
    public void setInstanceToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).commit();
    }

    /**
     * Uploads a video to the linked YouTube channel. yt2009 keeps the whole file in memory,
     * so instances usually limit uploads to around 10 MB.
     * @param privacy "public", "unlisted" or "private"
     * @return ID of the uploaded video, or null if the instance didn't return one
     */
    public String upload(InputStream video, long length, String mimeType, String title, String description,
                         String tags, String privacy, UploadProgressListener listener) throws IOException {
        String form = "state=step1&title=" + enc(title) + "&description=" + enc(description) + "&tags=" + enc(tags)
                + "&privacy=" + enc(privacy) + "&comments=allow-all";
        HttpURLConnection step1 = open("/my_videos_upload", HttpClient.VIDEO_TIMEOUT);
        byte[] formBytes = form.getBytes("UTF-8");
        step1.setRequestProperty("Content-Type", FORM);
        step1.setDoOutput(true);
        step1.setFixedLengthStreamingMode(formBytes.length);
        OutputStream out = step1.getOutputStream();
        out.write(formBytes);
        out.close();
        String page = readResponse(step1);
        Matcher m = FLOW_TOKEN.matcher(page);
        if (!m.find()) throw new IOException("No upload flow token");
        String flowToken = m.group(1);

        String boundary = "----notPipe" + Long.toHexString(System.currentTimeMillis());
        byte[] head = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"flow-token\"\r\n\r\n" + flowToken + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"video\"; filename=\"video.mp4\"\r\n"
                + "Content-Type: " + (mimeType == null || !mimeType.startsWith("video/") ? "video/mp4" : mimeType) + "\r\n\r\n")
                .getBytes("UTF-8");
        byte[] tail = ("\r\n--" + boundary + "--\r\n").getBytes("UTF-8");
        HttpURLConnection step2 = open("/my_videos_upload", HttpClient.CONVERSION_TIMEOUT);
        step2.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        step2.setDoOutput(true);
        step2.setFixedLengthStreamingMode((int) (head.length + length + tail.length));
        out = step2.getOutputStream();
        out.write(head);
        byte[] buffer = new byte[16384];
        long sent = 0;
        int read;
        while ((read = video.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            sent += read;
            if (listener != null) listener.onProgress(sent, length);
        }
        out.write(tail);
        out.close();
        String result = readResponse(step2);
        Matcher id = UPLOADED_ID.matcher(result);
        return id.find() ? id.group(1) : null;
    }

    /* ---------- Watch history on YouTube (so its recommendations learn from what you watch here) ---------- */

    private static final Pattern PLAYBACK_PID = Pattern.compile("playbackIdShorthand = \"([^\"]+)\"");
    private static final Pattern WATCHTIME_PID = Pattern.compile("watchtimeIdShorthand = \"([^\"]+)\"");

    /** IDs yt2009 hands out for YouTube's playback/watchtime stats URLs of one video */
    public static class WatchSession {
        final String playbackPid, watchtimePid;

        WatchSession(String playbackPid, String watchtimePid) {
            this.playbackPid = playbackPid;
            this.watchtimePid = watchtimePid;
        }
    }

    /**
     * Tells YouTube the video started playing, the same way yt2009's own player does,
     * so it lands in the account's watch history and shapes its recommendations.
     * @return session for {@link #reportWatchtime}, or null if the instance didn't provide stats URLs
     */
    public WatchSession startWatch(String videoId) throws IOException {
        if (!isSignedIn()) return null;
        String page = readResponse(open("/watch?v=" + videoId + "&with_pchelper=1", "GET", HttpClient.VIDEO_TIMEOUT));
        Matcher playback = PLAYBACK_PID.matcher(page);
        Matcher watchtime = WATCHTIME_PID.matcher(page);
        if (!playback.find() || !watchtime.find()) return null;
        WatchSession session = new WatchSession(playback.group(1), watchtime.group(1));
        readResponse(open("/playback_progress_report?pid=" + session.playbackPid, "GET", HttpClient.VIDEO_TIMEOUT));
        return session;
    }

    /**
     * @param seconds current playback position
     * @param finalReport true when the user leaves the video
     */
    public void reportWatchtime(WatchSession session, float seconds, boolean playing, boolean finalReport) throws IOException {
        if (session == null) return;
        readResponse(open("/playback_progress_report?pid=" + session.watchtimePid + "&rt=" + System.currentTimeMillis()
                + "&st=" + String.format("%.1f", seconds).replace(',', '.') + "&state=" + (playing ? "playing" : "paused")
                + (finalReport ? "&final=1" : ""), "GET", HttpClient.VIDEO_TIMEOUT));
    }

    private HttpURLConnection open(String endpoint, int timeout) throws IOException {
        return open(endpoint, "POST", timeout);
    }

    private HttpURLConnection open(String endpoint, String method, int timeout) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(getInstanceUrl() + endpoint).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(9999);
        connection.setReadTimeout(timeout);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", HttpClient.USER_AGENT);
        String token = prefs.getString(KEY_TOKEN, null);
        connection.setRequestProperty("Cookie", "pchelper_user=" + getDeviceCode()
                + (token != null && token.length() > 0 ? "; auth=" + token : ""));
        return connection;
    }

    private static String readResponse(HttpURLConnection connection) throws IOException {
        try {
            int status = connection.getResponseCode();
            if (status >= 300 && status < 400) {
                String location = connection.getHeaderField("Location");
                if (location != null && (location.contains("unauth") || location.contains("mh_pc_intro"))) {
                    throw location.contains("unauth") ? new TokenRequiredException() : new IOException("Not signed in");
                }
                return "";
            }
            if (status == 413) throw new IOException("File is too big for this instance");
            InputStream in = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (status >= 400) throw new IOException("HTTP " + status);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int n;
            while (in != null && (n = in.read(data)) != -1) buffer.write(data, 0, n);
            return new String(buffer.toByteArray(), "UTF-8");
        } finally {
            connection.disconnect();
        }
    }

    private static String enc(String s) throws IOException {
        return URLEncoder.encode(s == null ? "" : s, "UTF-8");
    }

    private static final String ATOM = "application/atom+xml";
    private static final String FORM = "application/x-www-form-urlencoded";

    /**
     * POSTs without following redirects: yt2009 answers some of these with a redirect to a feed that only
     * works with our device header, which HttpURLConnection wouldn't resend.
     */
    private void post(String endpoint, String body, String contentType) throws IOException {
        if (!isSignedIn()) throw new IOException("Not signed in");
        HttpURLConnection connection = open(endpoint, HttpClient.VIDEO_TIMEOUT);
        connection.setRequestProperty("X-GData-Device", "device-id=\"" + getDeviceCode() + "\"");
        connection.setRequestProperty("Content-Type", contentType);
        byte[] bytes = body.getBytes("UTF-8");
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(bytes.length);
        OutputStream out = connection.getOutputStream();
        out.write(bytes);
        out.close();
        readResponse(connection);
    }

    private HttpRequest authorized(String endpoint) {
        HttpRequest req = new HttpRequest(getInstanceUrl(), endpoint);
        req.addHeader("X-GData-Device", "device-id=\"" + getDeviceCode() + "\"");
        return req;
    }
}
