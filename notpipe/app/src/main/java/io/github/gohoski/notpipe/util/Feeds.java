package io.github.gohoski.notpipe.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.github.gohoski.notpipe.api.Manager;
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
     * @param channelId Shorts of one channel, or null for a feed made of the subscribed channels' Shorts
     *                  (falls back to searching #shorts when there are no subscriptions)
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
