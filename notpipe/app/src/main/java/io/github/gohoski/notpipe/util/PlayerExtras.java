package io.github.gohoski.notpipe.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import io.github.gohoski.notpipe.NotPipe;
import io.github.gohoski.notpipe.config.ConfigManager;
import io.github.gohoski.notpipe.http.HttpClient;
import io.github.gohoski.notpipe.http.HttpRequest;

/**
 * Created by Claude on 28.10.2026.
 * Player features around the video itself: player settings, resume positions, SponsorBlock segments and captions.
 * Network methods must run on a background thread.
 */

public class PlayerExtras {
    private static final String PREFS = "notPipe_player";
    private static final String RESUME_PREFS = "notPipe_resume";
    private static final String SPONSORBLOCK_API = "https://sponsor.ajay.app";
    private static final String SPONSORBLOCK_CATEGORIES =
            "[\"sponsor\",\"selfpromo\",\"interaction\",\"intro\",\"outro\",\"music_offtopic\"]";
    private static final int MAX_RESUME_ENTRIES = 300;

    public static final String KEY_AUTOPLAY = "autoplay";
    public static final String KEY_SPONSORBLOCK = "sponsorblock";
    public static final String KEY_RESUME = "resume";
    public static final String KEY_SPEED = "speed";
    public static final String KEY_AUTO_PIP = "auto_pip";
    public static final String KEY_BASIC_MODE = "basic_mode";

    private PlayerExtras() {}

    private static SharedPreferences prefs() {
        return NotPipe.getAppContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Autoplay, SponsorBlock and resume are on by default */
    public static boolean isEnabled(String key) {
        return prefs().getBoolean(key, true);
    }

    public static void setEnabled(String key, boolean enabled) {
        prefs().edit().putBoolean(key, enabled).commit();
    }

    /** Basic mode (on by default) hides technical details like instance names */
    public static boolean isBasicMode() {
        return isEnabled(KEY_BASIC_MODE);
    }

    public static String getShortsQuality() {
        return prefs().getString("shorts_quality", "360");
    }

    public static void setShortsQuality(String quality) {
        prefs().edit().putString("shorts_quality", quality).commit();
    }

    public static float getSpeed() {
        return prefs().getFloat(KEY_SPEED, 1f);
    }

    public static void setSpeed(float speed) {
        prefs().edit().putFloat(KEY_SPEED, speed).commit();
    }

    /* ---------- Resume where you left off ---------- */

    /** @return saved position in ms, or 0 */
    public static int getResumePosition(String videoId) {
        if (!isEnabled(KEY_RESUME)) return 0;
        return NotPipe.getAppContext().getSharedPreferences(RESUME_PREFS, Context.MODE_PRIVATE).getInt(videoId, 0);
    }

    /**
     * Remembers the position unless the video was barely started or practically finished.
     */
    public static void saveResumePosition(String videoId, int positionMs, int durationMs) {
        SharedPreferences resume = NotPipe.getAppContext().getSharedPreferences(RESUME_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = resume.edit();
        if (positionMs < 10000 || (durationMs > 0 && durationMs - positionMs < 15000)) {
            editor.remove(videoId);
        } else {
            if (resume.getAll().size() > MAX_RESUME_ENTRIES) editor.clear();
            editor.putInt(videoId, positionMs);
        }
        editor.commit();
    }

    /* ---------- SponsorBlock ---------- */

    /**
     * @return segments as {startMs, endMs}, empty if none or SponsorBlock is off
     */
    public static List<int[]> loadSponsorSegments(String videoId) {
        List<int[]> segments = new ArrayList<int[]>();
        if (!isEnabled(KEY_SPONSORBLOCK)) return segments;
        try {
            HttpRequest req = new HttpRequest.Builder(SPONSORBLOCK_API, "/api/skipSegments")
                    .addParam("videoID", videoId).addParam("categories", SPONSORBLOCK_CATEGORIES).build();
            String body = HttpClient.executeToString(req);
            if (!body.trim().startsWith("[")) return segments; // 404 "Not Found" = no segments
            JSONArray arr = JSON.getArray(body);
            for (int i = 0; i < arr.size(); i++) {
                JSONObject j = arr.getObject(i);
                if (!"skip".equals(j.getString("actionType", "skip"))) continue;
                JSONArray seg = j.getArray("segment");
                segments.add(new int[]{(int) (seg.getDouble(0) * 1000), (int) (seg.getDouble(1) * 1000)});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return segments;
    }

    /* ---------- Captions (Invidious) ---------- */

    public static class Caption {
        public final int start, end;
        public final String text;

        Caption(int start, int end, String text) {
            this.start = start;
            this.end = end;
            this.text = text;
        }
    }

    /** One available caption track */
    public static class Track {
        public final String label, url;

        Track(String label, String url) {
            this.label = label;
            this.url = url;
        }
    }

    /**
     * Lists caption tracks from the first Invidious instance that answers.
     */
    public static List<Track> loadTracks(String videoId) {
        List<Track> tracks = new ArrayList<Track>();
        try {
            String[] result = io.github.gohoski.notpipe.api.Discovery.fetchFromInvidious("/api/v1/captions/" + videoId);
            JSONArray arr = JSON.getObject(result[1]).getArray("captions");
            for (int j = 0; j < arr.size(); j++) {
                JSONObject c = arr.getObject(j);
                String url = c.getString("url", "");
                tracks.add(new Track(c.getString("label", "?"), url.startsWith("/") ? result[0] + url : url));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tracks;
    }

    private static final Pattern CUE = Pattern.compile(
            "(\\d+:)?(\\d{2}):(\\d{2})\\.(\\d{3})\\s+-->\\s+(\\d+:)?(\\d{2}):(\\d{2})\\.(\\d{3})[^\\n]*\\n([\\s\\S]*?)(?:\\n\\n|$)");

    /** Downloads and parses a WebVTT track */
    public static List<Caption> loadCaptions(String url) throws java.io.IOException {
        String vtt = HttpClient.executeToString(new HttpRequest(url)).replace("\r", "");
        List<Caption> captions = new ArrayList<Caption>();
        Matcher m = CUE.matcher(vtt);
        while (m.find()) {
            String text = m.group(9).replaceAll("<[^>]+>", "").trim();
            if (text.length() == 0) continue;
            captions.add(new Caption(toMs(m.group(1), m.group(2), m.group(3), m.group(4)),
                    toMs(m.group(5), m.group(6), m.group(7), m.group(8)), text));
        }
        return captions;
    }

    private static int toMs(String h, String m, String s, String ms) {
        int hours = h == null ? 0 : Integer.parseInt(h.substring(0, h.length() - 1));
        return ((hours * 60 + Integer.parseInt(m)) * 60 + Integer.parseInt(s)) * 1000 + Integer.parseInt(ms);
    }

    /** @return caption text shown at the position, or null */
    public static String captionAt(List<Caption> captions, int positionMs) {
        if (captions == null) return null;
        for (int i = 0; i < captions.size(); i++) {
            Caption c = captions.get(i);
            if (positionMs >= c.start && positionMs < c.end) return c.text;
        }
        return null;
    }
}
