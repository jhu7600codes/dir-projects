package io.github.gohoski.notpipe.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.github.gohoski.notpipe.Utils;
import io.github.gohoski.notpipe.api.Manager;
import io.github.gohoski.notpipe.api.Metadata;
import io.github.gohoski.notpipe.api.ShortsApi;
import io.github.gohoski.notpipe.api.Yt2009Account;
import io.github.gohoski.notpipe.data.Channel;
import io.github.gohoski.notpipe.data.VideoInfo;

/**
 * Created by Claude on 25.10.2026.
 * Builds the "Recommended" and "Shorts" feeds. Must be called on a background thread.
 */

public class Feeds {
    private static final int HISTORY_SEEDS = 6;
    private static final int MAX_RECOMMENDATIONS = 60;
    private static final int SHORTS_CHANNELS = 15;
    private static final int MAX_SHORT_SECONDS = 180;
    private static final int FEED_VIDEOS_PER_CHANNEL = 10;
    private static final int FEED_MAX_VIDEOS = 150;

    private Feeds() {}

    /**
     * Recommendations of the yt2009 account if signed in, otherwise videos related to the local watch history.
     * @return empty list if there's nothing to recommend from (no account, no history)
     */
    public static List<VideoInfo> loadRecommendations() throws InterruptedException {
        Yt2009Account account = Yt2009Account.getInstance();
        if (account.isSignedIn()) {
            try {
                List<VideoInfo> recs = account.getRecommendations();
                if (!recs.isEmpty()) return recs;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return loadHistoryRecommendations();
    }

    /**
     * Mixes related videos of the most recently watched videos, round-robin so every seed is represented,
     * skipping anything already watched.
     */
    public static List<VideoInfo> loadHistoryRecommendations() throws InterruptedException {
        List<VideoInfo> history = LocalLibrary.getInstance().getHistory();
        final Set<String> watched = new HashSet<String>();
        for (int i = 0; i < history.size(); i++) watched.add(history.get(i).id);
        int seeds = Math.min(HISTORY_SEEDS, history.size());
        final List<List<VideoInfo>> related = new ArrayList<List<VideoInfo>>();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        for (int i = 0; i < seeds; i++) {
            final String id = history.get(i).id;
            final List<VideoInfo> bucket = Collections.synchronizedList(new ArrayList<VideoInfo>());
            related.add(bucket);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        bucket.addAll(Manager.getInstance().getMetadata().getRelated(id));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.MINUTES);
        return interleave(related, watched, MAX_RECOMMENDATIONS);
    }

    /**
     * Latest videos of all local subscriptions, newest first.
     */
    public static List<VideoInfo> loadSubscriptions() throws InterruptedException {
        List<Channel> channels = LocalLibrary.getInstance().getSubscriptions();
        final List<VideoInfo> result = Collections.synchronizedList(new ArrayList<VideoInfo>());
        ExecutorService executor = Executors.newFixedThreadPool(4);
        for (int i = 0; i < channels.size(); i++) {
            final Channel channel = channels.get(i);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    List<VideoInfo> channelVideos = null;
                    try {
                        Metadata metadata = Manager.getInstance().getMetadata();
                        channelVideos = metadata.getChannelVideos(channel.id, 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (channelVideos == null || channelVideos.isEmpty()) {
                        try {
                            channelVideos = Manager.getInstance().getChannelApi().getChannelVideos(channel.id, 0);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (channelVideos == null) return;
                    for (int j = 0; j < channelVideos.size() && j < FEED_VIDEOS_PER_CHANNEL; j++) {
                        VideoInfo v = channelVideos.get(j);
                        result.add(new VideoInfo(v.id, v.title, v.thumbnail, channel.title, channel.thumbnail,
                                channel.id, v.duration, v.views, v.publishedAt));
                    }
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        List<VideoInfo> sorted = new ArrayList<VideoInfo>(result);
        Collections.sort(sorted, new Comparator<VideoInfo>() {
            @Override
            public int compare(VideoInfo a, VideoInfo b) {
                if (a.publishedAt == null && b.publishedAt == null) return 0;
                if (a.publishedAt == null) return 1;
                if (b.publishedAt == null) return -1;
                return b.publishedAt.compareTo(a.publishedAt);
            }
        });
        return sorted.size() > FEED_MAX_VIDEOS ? new ArrayList<VideoInfo>(sorted.subList(0, FEED_MAX_VIDEOS)) : sorted;
    }

    /**
     * @param channelId Shorts of one channel, or null for a feed made of the subscribed channels' Shorts
     *                  (falls back to the channels of popular videos, then to searching #shorts)
     */
    public static List<VideoInfo> loadShorts(String channelId) throws InterruptedException {
        final ShortsApi api = Manager.getInstance().getShortsApi();
        if (channelId != null) {
            if (api == null) return new ArrayList<VideoInfo>();
            try {
                return api.getChannelShorts(channelId);
            } catch (Exception e) {
                e.printStackTrace();
                return new ArrayList<VideoInfo>();
            }
        }

        List<VideoInfo> result = new ArrayList<VideoInfo>();
        List<Channel> channels = LocalLibrary.getInstance().getSubscriptions();
        if (api != null && channels.isEmpty()) {
            channels = popularChannels();
        }
        if (api != null && !channels.isEmpty()) {
            Collections.shuffle(channels);
            final List<List<VideoInfo>> buckets = new ArrayList<List<VideoInfo>>();
            ExecutorService executor = Executors.newFixedThreadPool(3);
            for (int i = 0; i < channels.size() && i < SHORTS_CHANNELS; i++) {
                final Channel channel = channels.get(i);
                final List<VideoInfo> bucket = Collections.synchronizedList(new ArrayList<VideoInfo>());
                buckets.add(bucket);
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            List<VideoInfo> shorts = api.getChannelShorts(channel.id);
                            for (int j = 0; j < shorts.size(); j++) {
                                VideoInfo v = shorts.get(j);
                                bucket.add(new VideoInfo(v.id, v.title, v.thumbnail, channel.title, channel.thumbnail,
                                        channel.id, v.duration, v.views, v.publishedAt));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            executor.shutdown();
            executor.awaitTermination(2, TimeUnit.MINUTES);
            result = interleave(buckets, new HashSet<String>(), 200);
        }

        if (result.isEmpty()) {
            try {
                List<VideoInfo> found = Manager.getInstance().getMetadata().search("#shorts");
                for (int i = 0; i < found.size(); i++) {
                    int seconds = parseDuration(found.get(i).duration);
                    if (seconds > 0 && seconds <= MAX_SHORT_SECONDS) result.add(found.get(i));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /** Channels of the current popular videos, used for Shorts when there are no subscriptions */
    private static List<Channel> popularChannels() {
        List<Channel> channels = new ArrayList<Channel>();
        try {
            // Through PlaylistApi rather than Metadata.getPopularVideos(), since empty Piped playlists fall through to the next instance there
            List<VideoInfo> popular = Manager.getInstance().getPlaylistApi().getPlaylist(Utils.getHypePlaylist()).videos;
            Set<String> seen = new HashSet<String>();
            for (int i = 0; i < popular.size(); i++) {
                VideoInfo v = popular.get(i);
                if (v.channelId != null && v.channelId.startsWith("UC") && seen.add(v.channelId)) {
                    channels.add(new Channel(v.channelId, v.channel, v.channelThumbnail, "", "", -1, null));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return channels;
    }

    private static List<VideoInfo> interleave(List<List<VideoInfo>> buckets, Set<String> skip, int max) {
        List<VideoInfo> result = new ArrayList<VideoInfo>();
        Set<String> seen = new HashSet<String>(skip);
        boolean added = true;
        for (int round = 0; added && result.size() < max; round++) {
            added = false;
            for (int b = 0; b < buckets.size() && result.size() < max; b++) {
                List<VideoInfo> bucket = buckets.get(b);
                if (round >= bucket.size()) continue;
                added = true;
                VideoInfo v = bucket.get(round);
                if (seen.add(v.id)) result.add(v);
            }
        }
        return result;
    }

    /** "1:02:03" / "4:05" → seconds, -1 if unknown */
    public static int parseDuration(String duration) {
        if (duration == null || duration.length() == 0) return -1;
        try {
            String[] parts = duration.split(":");
            int seconds = 0;
            for (int i = 0; i < parts.length; i++) {
                seconds = seconds * 60 + Integer.parseInt(parts[i].trim());
            }
            return seconds;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
