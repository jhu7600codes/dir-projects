package io.github.gohoski.notpipe.api;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.gohoski.notpipe.Utils;
import io.github.gohoski.notpipe.data.Channel;
import io.github.gohoski.notpipe.data.Playlist;
import io.github.gohoski.notpipe.data.VideoInfo;

/**
 * Created by Claude on 25.10.2026.
 * Minimal parser for the GData (YouTube API v2) Atom feeds that yt2009 emulates.
 * Regex-based like the rest of the yt2009 code, since the feeds are generated from fixed templates.
 */

final class GdataParser {
    private static final Pattern ENTRY = Pattern.compile("<entry>(.*?)</entry>", Pattern.DOTALL);
    private static final Pattern VIDEO_ID = Pattern.compile("<yt:videoid[^>]*>([a-zA-Z0-9_-]{11})</yt:videoid>");
    private static final Pattern TITLE = Pattern.compile("<title[^>]*>([^<]*)</title>");
    private static final Pattern AUTHOR = Pattern.compile("<author>\\s*<name>([^<]*)</name>");
    private static final Pattern AUTHOR_ID = Pattern.compile("<(?:yt:uploaderId|yt:userId|yt9aid)>(UC[a-zA-Z0-9_-]+)<");
    private static final Pattern DURATION = Pattern.compile("<yt:duration seconds=['\"](\\d+)['\"]");
    private static final Pattern VIEWS = Pattern.compile("viewCount=['\"](\\d+)['\"]");
    private static final Pattern PUBLISHED = Pattern.compile("<published>([^<]+)</published>");
    private static final Pattern PLAYLIST_ID = Pattern.compile("<yt:playlistId>([^<]+)</yt:playlistId>");
    private static final Pattern COUNT_HINT = Pattern.compile("<yt:countHint>(\\d+)</yt:countHint>");
    private static final Pattern CHANNEL_ID = Pattern.compile("<y9id>([^<]+)</y9id>");
    private static final Pattern USERNAME = Pattern.compile("<yt:username>([^<]*)</yt:username>");
    private static final Pattern AVATAR = Pattern.compile("<y9av>([^<]*)</y9av>");
    private static final Pattern FEED_TITLE = Pattern.compile("<feed[^>]*>.*?<title[^>]*>([^<]*)</title>", Pattern.DOTALL);

    private GdataParser() {}

    static List<VideoInfo> parseVideos(String baseUrl, String xml) {
        List<VideoInfo> videos = new ArrayList<VideoInfo>();
        Matcher entries = ENTRY.matcher(xml);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        while (entries.find()) {
            String entry = entries.group(1);
            String id = find(VIDEO_ID, entry);
            if (id == null) continue;
            String duration = find(DURATION, entry);
            String views = find(VIEWS, entry);
            String published = find(PUBLISHED, entry);
            Date date = null;
            if (published != null && published.length() >= 19) {
                try {
                    date = sdf.parse(published.substring(0, 19));
                } catch (Exception ignored) {}
            }
            String author = find(AUTHOR, entry);
            String authorId = find(AUTHOR_ID, entry);
            videos.add(new VideoInfo(id, unescape(find(TITLE, entry)), baseUrl + "/thumb_proxy?v=" + id,
                    author == null ? "" : unescape(author), "", authorId == null ? "" : authorId,
                    duration == null || "0".equals(duration) || "1".equals(duration) ? "" : Utils.formatDuration(Integer.parseInt(duration)),
                    views == null ? -1 : Long.parseLong(views), date));
        }
        return videos;
    }

    static List<Playlist> parsePlaylists(String baseUrl, String xml) {
        List<Playlist> playlists = new ArrayList<Playlist>();
        Matcher entries = ENTRY.matcher(xml);
        while (entries.find()) {
            String entry = entries.group(1);
            String id = find(PLAYLIST_ID, entry);
            if (id == null) continue;
            String count = find(COUNT_HINT, entry);
            String thumb = find(MEDIA_THUMB, entry);
            if (thumb != null && thumb.startsWith("/")) thumb = baseUrl + thumb;
            playlists.add(new Playlist(id, unescape(find(TITLE, entry)), "", thumb == null ? "" : thumb.replace("&amp;", "&"),
                    count == null ? -1 : Integer.parseInt(count), null));
        }
        return playlists;
    }

    private static final Pattern MEDIA_THUMB = Pattern.compile("<media:thumbnail[^>]*url=['\"]([^'\"]+)['\"]");
    private static final Pattern AUTHOR_NAME = Pattern.compile("<author>\\s*<name>([^<]*)</name>");

    /** @return {name, avatar url} of a user entry */
    static String[] parseProfile(String baseUrl, String xml) {
        String name = find(AUTHOR_NAME, xml);
        String avatar = find(MEDIA_THUMB, xml);
        if (avatar != null && avatar.startsWith("/")) avatar = baseUrl + avatar;
        return new String[]{name == null ? "" : unescape(name).trim(), avatar == null ? "" : avatar.replace("&amp;", "&")};
    }

    static List<Channel> parseSubscriptions(String baseUrl, String xml) {
        List<Channel> channels = new ArrayList<Channel>();
        Matcher entries = ENTRY.matcher(xml);
        while (entries.find()) {
            String entry = entries.group(1);
            String id = find(CHANNEL_ID, entry);
            if (id == null) continue;
            String name = find(USERNAME, entry);
            String avatar = find(AVATAR, entry);
            if (avatar != null && avatar.startsWith("/")) avatar = baseUrl + avatar;
            channels.add(new Channel(id, name == null ? id : unescape(name), avatar == null ? "" : avatar, "", "", -1, null));
        }
        return channels;
    }

    static String parseFeedTitle(String xml) {
        String title = find(FEED_TITLE, xml);
        return title == null ? "" : unescape(title).trim();
    }

    private static String find(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private static String unescape(String s) {
        if (s == null) return "";
        return s.replace("&quot;", "\"").replace("&apos;", "'").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&amp;", "&");
    }
}
