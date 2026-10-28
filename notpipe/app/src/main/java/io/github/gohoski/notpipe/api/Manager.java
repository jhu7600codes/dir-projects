package io.github.gohoski.notpipe.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.github.gohoski.notpipe.NotPipe;
import io.github.gohoski.notpipe.R;
import io.github.gohoski.notpipe.config.Config;
import io.github.gohoski.notpipe.config.ConfigManager;
import io.github.gohoski.notpipe.http.HttpClient;
import io.github.gohoski.notpipe.http.VideoTooLongException;

/**
 * Manager class for handling multiple API instances with fallback capability.
 */
public class Manager {
    private static Manager instance;
    private Random random;
    private ConfigManager configManager;

    private List<Metadata> metadataInstances;
    private List<VideoStream> videoInstances; //360p only
    private List<VideoStream> hqInstances; //high-quality instances (480p+)
    private List<Conversion> conversionInstances; // Tracks instances capable of conversion
    private List<ChannelApi> channelApiInstances; // Tracks instances capable of ChannelApi
    private List<PlaylistApi> playlistInstances;
    private List<AudioStream> audioInstances;
    private List<ShortsApi> shortsInstances;

    private static final String[] HQ_QUALITIES = {"1080", "720", "480"};
    private static final int RACE_WIDTH = 3;
    private static final int HQ_RACE_WIDTH = 2; // HQ makes yt2009 mux/transcode on the server, so don't overdo it
    private final ExecutorService racePool = Executors.newCachedThreadPool();
    private final Map<String, Integer> scores = java.util.Collections.synchronizedMap(new HashMap<String, Integer>());

    private Manager() {
        random = new Random();
    }

    public static void init() {
        if (instance == null) {
            instance = new Manager();
            instance.initializeInstances();
        }
    }

    public static Manager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Manager.init() must be called first!");
        }
        return instance;
    }

    private void initializeInstances() {
        if (metadataInstances == null) {
            metadataInstances = new ArrayList<Metadata>();
            videoInstances = new ArrayList<VideoStream>();
            hqInstances = new ArrayList<VideoStream>();
            conversionInstances = new ArrayList<Conversion>();
            channelApiInstances = new ArrayList<ChannelApi>();
            playlistInstances = new ArrayList<PlaylistApi>();
            audioInstances = new ArrayList<AudioStream>();
            shortsInstances = new ArrayList<ShortsApi>();
        } else {
            metadataInstances.clear();
            videoInstances.clear();
            hqInstances.clear();
            conversionInstances.clear();
            channelApiInstances.clear();
            playlistInstances.clear();
            audioInstances.clear();
            shortsInstances.clear();
        }

        if (configManager == null) {
            configManager = ConfigManager.getInstance();
        }

        Config config = configManager.getConfig();

        List<String> invInstances = config.getInvidiousInstances();
        for (int i = 0; i < invInstances.size(); i++) {
            Invidious inv = new Invidious(invInstances.get(i));
            metadataInstances.add(inv);
            videoInstances.add(inv);
            playlistInstances.add(inv);
            audioInstances.add(inv);
            shortsInstances.add(inv);
        }

        List<String> yt2009List = config.getYt2009Instances();
        for (int i = 0; i < yt2009List.size(); i++) {
            Yt2009 yt2009 = new Yt2009(yt2009List.get(i));
            videoInstances.add(yt2009);
            hqInstances.add(yt2009);
            conversionInstances.add(yt2009);
            channelApiInstances.add(yt2009);
            playlistInstances.add(yt2009);
        }

        List<String> pipedList = config.getPipedInstances();
        for (int i = 0; i < pipedList.size(); i++) {
            String[] parts = pipedList.get(i).split(",", 2);
            String baseUrl = parts[0];
            String proxyUrl = parts.length > 1 ? parts[1] : parts[0];
            Piped piped = new Piped(baseUrl, proxyUrl);
            metadataInstances.add(piped);
            videoInstances.add(piped);
            playlistInstances.add(piped);
            audioInstances.add(piped);
        }

        List<String> ytApiInstances = config.getYtApiLegacyInstances();
        for (int i = 0; i < ytApiInstances.size(); i++) {
            YtApiLegacy ytApi = new YtApiLegacy(ytApiInstances.get(i));
            metadataInstances.add(ytApi);
            videoInstances.add(ytApi);
            hqInstances.add(ytApi);
            conversionInstances.add(ytApi);
            playlistInstances.add(ytApi);
        }
    }

    public interface MetadataTask<R> {
        R run(Metadata metadata) throws Exception;
    }

    /**
     * Runs the task on a few Metadata instances at once and returns the first result, like the video URL race.
     * A ContentUnavailableException from the first finished instance is passed on as before.
     * @param winner if not null, receives a fallback proxy that sticks to the winning instance
     */
    public <R> R raceMetadata(final MetadataTask<R> task, Metadata[] winner) throws IOException {
        List<Metadata> candidates = new ArrayList<Metadata>(metadataInstances);
        if (candidates.isEmpty()) throw new IOException("No Metadata instances");
        java.util.Collections.shuffle(candidates, random);
        java.util.Collections.sort(candidates, new java.util.Comparator<Metadata>() {
            @Override
            public int compare(Metadata a, Metadata b) {
                return score(b.getHost()) - score(a.getHost());
            }
        });
        CompletionService<Object[]> race = new ExecutorCompletionService<Object[]>(racePool);
        int next = 0, running = 0;
        while (next < candidates.size() && running < RACE_WIDTH) {
            submitMetadata(race, candidates.get(next++), task);
            running++;
        }
        Throwable last = null;
        while (running > 0) {
            Future<Object[]> done;
            try {
                done = race.poll(60000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new IOException("Interrupted");
            }
            if (done == null) break;
            running--;
            try {
                Object[] result = done.get();
                if (winner != null && winner.length > 0) {
                    winner[0] = createStatefulFallbackProxy(Metadata.class, metadataInstances, (Metadata) result[0]);
                }
                @SuppressWarnings("unchecked") R value = (R) result[1];
                return value;
            } catch (ExecutionException e) {
                last = e.getCause();
                if (last instanceof ContentUnavailableException) throw (ContentUnavailableException) last;
            } catch (InterruptedException e) {
                throw new IOException("Interrupted");
            }
            if (next < candidates.size()) {
                submitMetadata(race, candidates.get(next++), task);
                running++;
            }
        }
        if (metadataInstances.isEmpty()) {
            reloadInstances();
            showConnectionErrorToast();
        }
        if (last instanceof IOException) throw (IOException) last;
        throw new IOException(last != null ? last.getMessage() : "All instances failed");
    }

    private <R> void submitMetadata(CompletionService<Object[]> race, final Metadata instance, final MetadataTask<R> task) {
        race.submit(new Callable<Object[]>() {
            @Override
            public Object[] call() throws Exception {
                try {
                    Object value = task.run(instance);
                    addScore(instance.getHost(), 1);
                    return new Object[]{instance, value};
                } catch (ContentUnavailableException e) {
                    throw e;
                } catch (Exception e) {
                    addScore(instance.getHost(), -2);
                    if (isDeadInstanceError(e)) removeDeadInstance(instance);
                    throw e;
                }
            }
        });
    }

    public Metadata getMetadata() {
        if (metadataInstances.isEmpty()) throw new IllegalStateException("No Metadata instances");
        return createStatefulFallbackProxy(Metadata.class, metadataInstances);
    }

    /**
     * Retrieves a single raw Metadata instance without a dynamic fallback proxy wrapper.
     * Useful for optional single-attempt fallback operations (like channel icons).
     */
    public Metadata getRandomMetadata() {
        if (metadataInstances == null || metadataInstances.isEmpty()) {
            return null;
        }
        return metadataInstances.get(random.nextInt(metadataInstances.size()));
    }

    /**
     * Retrieves a Metadata proxy prioritizing YtApiLegacy instances.
     * Returns null if no YtApiLegacy instances are configured.
     */
    public Metadata getPopularMetadata() {
        if (metadataInstances == null || metadataInstances.isEmpty()) {
            return null;
        }

        List<Metadata> ytApiList = new ArrayList<Metadata>();
        for (int i = 0; i < metadataInstances.size(); i++) {
            Metadata m = metadataInstances.get(i);
            if (m instanceof YtApiLegacy) {
                ytApiList.add(m);
            }
        }

        if (ytApiList.isEmpty()) {
            return null;
        }

        return createStatefulFallbackProxy(Metadata.class, ytApiList);
    }

    public VideoStream getVideoStream() {
        if (videoInstances.isEmpty()) throw new IllegalStateException("No VideoStream instances");
        return createStatefulFallbackProxy(VideoStream.class, videoInstances);
    }

    public ChannelApi getChannelApi() {
        if (channelApiInstances.isEmpty()) throw new IllegalStateException("No ChannelApi instances");
        return createStatefulFallbackProxy(ChannelApi.class, channelApiInstances);
    }

    public PlaylistApi getPlaylistApi() {
        if (playlistInstances.isEmpty()) throw new IllegalStateException("No PlaylistApi instances");
        return createStatefulFallbackProxy(PlaylistApi.class, playlistInstances);
    }

    /** Replies to a comment (token from Comment.repliesToken), through Invidious */
    public List<io.github.gohoski.notpipe.data.Comment> getCommentReplies(String videoId, String token) throws IOException {
        return Invidious.getReplies(videoId, token);
    }

    /**
     * @return null if no instance supports channel Shorts (only Invidious does)
     */
    public ShortsApi getShortsApi() {
        if (shortsInstances.isEmpty()) return null;
        return createStatefulFallbackProxy(ShortsApi.class, shortsInstances);
    }

    /**
     * Returns an audio-only stream URL. If no instance can give one (e.g. only yt2009/YtAPILegacy are configured),
     * falls back to the 360p video stream—MediaPlayer plays its audio track fine without a surface.
     */
    public String getAudioUrl(String videoId) throws IOException {
        String url = getAudioOnlyUrl(videoId);
        if (url != null) return url;
        return getVideoUrl(videoId, "360", HttpClient.VIDEO_TIMEOUT);
    }

    /**
     * @return an audio-only stream URL, or null if no instance could provide one
     */
    public String getAudioOnlyUrl(String videoId) throws IOException {
        List<AudioStream> targetList = new ArrayList<AudioStream>(audioInstances);
        java.util.Collections.shuffle(targetList, random);
        for (int i = 0; i < targetList.size(); i++) {
            AudioStream currentInstance = targetList.get(i);
            try {
                return currentInstance.getAudioUrl(videoId);
            } catch (ContentUnavailableException e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
                if (isDeadInstanceError(e)) {
                    removeDeadInstance(currentInstance);
                }
            }
        }
        return null;
    }

    public List<Conversion> getConversion() {
        return conversionInstances;
    }

    public String getVideoUrl(String videoId, String quality, int timeout) throws IOException {
        return getVideoUrl(videoId, quality, timeout, null, null);
    }

    public String getVideoUrl(String videoId, String quality, int timeout, VideoStream preferredInstance, VideoStream[] successfulInstance) throws IOException {
        if (videoInstances.isEmpty() || ("360".equals(quality) ? videoInstances : hqInstances).isEmpty()) {
            reloadInstances();
            throw new IOException("No video instances left");
        }

        // Requested quality first, then step down instead of failing (1080 → 720 → 480).
        // A FileNotFoundException at the end makes the caller fall back to 360p like before.
        List<String> qualities = new ArrayList<String>();
        qualities.add(quality);
        if (!"360".equals(quality)) {
            for (int i = 0; i < HQ_QUALITIES.length; i++) {
                if (Integer.parseInt(HQ_QUALITIES[i]) < Integer.parseInt(quality)) qualities.add(HQ_QUALITIES[i]);
            }
        }

        Throwable lastError = null;
        boolean notFound = false;
        for (int q = 0; q < qualities.size(); q++) {
            String currentQuality = qualities.get(q);
            List<VideoStream> targetList = orderByScore("360".equals(currentQuality)
                    ? new ArrayList<VideoStream>(videoInstances) : new ArrayList<VideoStream>(hqInstances), preferredInstance);
            try {
                return raceVideoUrl(videoId, currentQuality, timeout, targetList, successfulInstance);
            } catch (ContentUnavailableException e) {
                throw e;
            } catch (FileNotFoundException e) {
                notFound = true;
            } catch (IOException e) {
                lastError = e;
            }
        }

        if (videoInstances.isEmpty() || hqInstances.isEmpty()) {
            reloadInstances();
            showConnectionErrorToast();
        }
        if (!"360".equals(quality)) throw new FileNotFoundException("Video quality not available");
        if (notFound && lastError == null) throw new FileNotFoundException("Video not available");
        if (lastError instanceof IOException) throw (IOException) lastError;
        throw new IOException("All instances failed");
    }

    /**
     * Instances that answered fast and reliably before go first; the rest are shuffled to spread the load.
     */
    private List<VideoStream> orderByScore(List<VideoStream> list, VideoStream preferred) {
        java.util.Collections.shuffle(list, random);
        java.util.Collections.sort(list, new java.util.Comparator<VideoStream>() {
            @Override
            public int compare(VideoStream a, VideoStream b) {
                return score(b.getHost()) - score(a.getHost());
            }
        });
        if (preferred != null && list.remove(preferred)) list.add(0, preferred);
        return list;
    }

    private int score(String host) {
        Integer s = scores.get(host);
        return s == null ? 0 : s;
    }

    private void addScore(String host, int delta) {
        scores.put(host, Math.max(-5, Math.min(10, score(host) + delta)));
    }

    /**
     * Asks a few instances at once and returns the first working URL, so one slow or dead instance
     * doesn't hold up playback. The losers finish in the background and only update the scores.
     */
    private String raceVideoUrl(final String videoId, final String quality, final int timeout,
                                List<VideoStream> candidates, VideoStream[] successfulInstance) throws IOException {
        CompletionService<Object[]> race = new ExecutorCompletionService<Object[]>(racePool);
        int width = "360".equals(quality) ? RACE_WIDTH : HQ_RACE_WIDTH;
        int next = 0, running = 0;
        while (next < candidates.size() && running < width) {
            submit(race, candidates.get(next++), videoId, quality, timeout);
            running++;
        }
        Throwable lastError = null;
        boolean notFound = false;
        while (running > 0) {
            Future<Object[]> done;
            try {
                done = race.poll(timeout + 5000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new IOException("Interrupted");
            }
            if (done == null) break;
            running--;
            try {
                Object[] result = done.get();
                if (successfulInstance != null && successfulInstance.length > 0) {
                    successfulInstance[0] = (VideoStream) result[0];
                }
                return (String) result[1];
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof ContentUnavailableException) throw (ContentUnavailableException) cause;
                if (cause instanceof FileNotFoundException) notFound = true; else lastError = cause;
            } catch (InterruptedException e) {
                throw new IOException("Interrupted");
            }
            if (next < candidates.size()) {
                submit(race, candidates.get(next++), videoId, quality, timeout);
                running++;
            }
        }
        if (notFound && lastError == null) throw new FileNotFoundException("Video quality not available");
        if (lastError instanceof IOException) throw (IOException) lastError;
        throw new IOException(lastError != null ? lastError.getMessage() : "All instances failed");
    }

    private void submit(CompletionService<Object[]> race, final VideoStream instance, final String videoId,
                        final String quality, final int timeout) {
        race.submit(new Callable<Object[]>() {
            @Override
            public Object[] call() throws Exception {
                long start = System.currentTimeMillis();
                try {
                    String url = instance.getVideoUrl(videoId, quality, timeout);
                    addScore(instance.getHost(), System.currentTimeMillis() - start < 8000 ? 2 : 1);
                    return new Object[]{instance, url};
                } catch (FileNotFoundException e) {
                    throw e;
                } catch (Exception e) {
                    addScore(instance.getHost(), -2);
                    if (isDeadInstanceError(e)) removeDeadInstance(instance);
                    throw e;
                }
            }
        });
    }

    public String getConvUrl(String videoId, String quality, int codec) throws IOException {
        return getConvUrl(videoId, quality, codec, null, null);
    }

    public String getConvUrl(String videoId, String quality, int codec, VideoStream preferredInstance, VideoStream[] successfulInstance) throws IOException {
        if (configManager.getConfig().isConvertVideos()) {
            if (conversionInstances.isEmpty()) {
                reloadInstances();
                throw new IOException("No conversion instances left");
            }

            List<Conversion> targetConvList = new ArrayList<Conversion>(conversionInstances);

            // Prioritize the preferred instance if it supports conversion
            if (preferredInstance instanceof Conversion && targetConvList.contains((Conversion) preferredInstance)) {
                targetConvList.remove((Conversion) preferredInstance);
                targetConvList.add(0, (Conversion) preferredInstance);
                if (targetConvList.size() > 1) {
                    List<Conversion> rest = targetConvList.subList(1, targetConvList.size());
                    java.util.Collections.shuffle(rest, random);
                }
            } else {
                java.util.Collections.shuffle(targetConvList, random);
            }

            for (int i = 0; i < targetConvList.size(); i++) {
                Conversion currentInstance = targetConvList.get(i);
                try {
                    String url = currentInstance.getConvUrl(videoId, codec);
                    if (successfulInstance != null && successfulInstance.length > 0 && currentInstance instanceof VideoStream) {
                        successfulInstance[0] = (VideoStream) currentInstance;
                    }
                    return url;
                } catch (Exception e) {
                    if (e instanceof ContentUnavailableException) {
                        throw (ContentUnavailableException) e;
                    } if (e instanceof VideoTooLongException) {
                        throw (VideoTooLongException) e;
                    } conversionInstances.remove(currentInstance);
                    if (isDeadInstanceError(e)) {
                        removeDeadInstance(currentInstance);
                    }
                }
            }
        }

        // Fall back to standard stream URLs if conversion is disabled or if all conversion attempts fail
        return getVideoUrl(videoId, quality, HttpClient.CONVERSION_TIMEOUT, preferredInstance, successfulInstance);
    }

    /**
     Creates a dynamic proxy that retains one instance for the lifespan of the Activity.
     */
    @SuppressWarnings("unchecked")
    private <T> T createStatefulFallbackProxy(final Class<T> interfaceClass, final List<T> pool) {
        return createStatefulFallbackProxy(interfaceClass, pool, null);
    }

    @SuppressWarnings("unchecked")
    private <T> T createStatefulFallbackProxy(final Class<T> interfaceClass, final List<T> pool, final T initial) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{ interfaceClass },
                new InvocationHandler() {
                    private T currentInstance = initial;
                    private void pickNewInstance() {
                        if (!pool.isEmpty()) {
                            currentInstance = pool.get(random.nextInt(pool.size()));
                        } else {
                            currentInstance = null;
                        }
                    }

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (currentInstance == null) pickNewInstance();

                        Throwable lastError = null;
                        List<T> localPool = new ArrayList<T>(pool);

                        while (!localPool.isEmpty()) {
                            if (currentInstance == null || !localPool.contains(currentInstance)) {
                                currentInstance = localPool.get(random.nextInt(localPool.size()));
                            }

                            try {
                                return method.invoke(currentInstance, args);
                            } catch (InvocationTargetException e) {
                                if (e.getCause() instanceof ContentUnavailableException) {
                                    throw e.getCause();
                                }
                                e.printStackTrace();
                                lastError = e.getCause();
                                localPool.remove(currentInstance);

                                if (isDeadInstanceError(lastError)) {
                                    removeDeadInstance(currentInstance);
                                }
                                currentInstance = null;
                            }
                        }

                        if (pool.isEmpty()) {
                            reloadInstances();
                            showConnectionErrorToast();
                        }

                        if (lastError != null) throw lastError;
                        throw new IOException("No available instances left for " + interfaceClass.getSimpleName());
                    }
                }
        );
    }

    public void removeDeadInstance(Object instance) {
        final String host;
        try {
            host = (String) instance.getClass().getMethod("getHost").invoke(instance);
            notifyDeadInstance(host);
        } catch (Exception ignored) { return; }
        removeByHost(metadataInstances, host);
        removeByHost(videoInstances, host);
        removeByHost(hqInstances, host);
        removeByHost(conversionInstances, host);
        removeByHost(channelApiInstances, host);
        removeByHost(playlistInstances, host);
        removeByHost(audioInstances, host);
        removeByHost(shortsInstances, host);
    }

    private void removeByHost(List list, String host) {
        java.util.Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
            try {
                Object item = iterator.next();
                String itemHost = (String) item.getClass().getMethod("getHost").invoke(item);
                if (host.equals(itemHost)) {
                    iterator.remove();
                }
            } catch (Exception ignored) {}
        }
    }

    private void notifyDeadInstance(final String name) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    Context context = NotPipe.getAppContext();
                    if (context != null) {
                        Toast.makeText(context,
                                context.getString(R.string.dead_instance, name),
                                Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void showConnectionErrorToast() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    Toast.makeText(NotPipe.getAppContext(),
                            "check your internet connection",
                            Toast.LENGTH_SHORT).show();
                } catch (Exception ignored) {}
            }
        });
    }

    private boolean isDeadInstanceError(Throwable t) {
        if (t == null) return false;

        if (!io.github.gohoski.notpipe.Utils.hasConnection(NotPipe.getAppContext())) {
            return false;
        }

        if (t instanceof SocketTimeoutException ||
                t instanceof ConnectException ||
                t instanceof UnknownHostException) {
            return true;
        }

        return false;
    }

    public void reloadInstances() {
        initializeInstances();
    }

    public List<VideoStream> getVideoInstances() {
        return videoInstances;
    }

    public List<VideoStream> getHqInstances() {
        return hqInstances;
    }

    public static class InstanceInfo {
        public VideoStream instance;
        public String host;
        public String name;
        public boolean supportsAllQualities;
    }

    public List<InstanceInfo> videoInstancesInfo() {
        initializeInstances();
        List<InstanceInfo> result = new ArrayList<InstanceInfo>();
        List<VideoStream> allKnown = new ArrayList<VideoStream>();
        List<String> processed = new ArrayList<String>();
        allKnown.addAll(videoInstances);
        allKnown.addAll(hqInstances);

        for (int i = 0; i < allKnown.size(); i++) {
            VideoStream instance = allKnown.get(i);
            InstanceInfo info = new InstanceInfo();
            info.instance = instance;
            info.host = instance.getHost();
            info.name = instance.getName();
            info.supportsAllQualities = !(instance instanceof Invidious) && !(instance instanceof Piped);
            if (!processed.contains(info.host)) {
                result.add(info);
                processed.add(info.host);
            }
        }

        return result;
    }
}