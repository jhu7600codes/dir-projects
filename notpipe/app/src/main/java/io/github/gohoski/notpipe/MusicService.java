package io.github.gohoski.notpipe;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.github.gohoski.notpipe.api.Manager;
import io.github.gohoski.notpipe.data.VideoInfo;
import io.github.gohoski.notpipe.util.Compat;

/**
 * Created by Claude on 25.10.2026.
 * Music mode: plays the audio of videos (or whole playlists) in the background with a notification.
 * Downloaded files are used instead of streaming when available.
 */

public class MusicService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    public static final String ACTION_PLAY = "io.github.gohoski.notpipe.music.PLAY";
    public static final String ACTION_TOGGLE = "io.github.gohoski.notpipe.music.TOGGLE";
    public static final String ACTION_NEXT = "io.github.gohoski.notpipe.music.NEXT";
    public static final String ACTION_PREVIOUS = "io.github.gohoski.notpipe.music.PREVIOUS";
    public static final String ACTION_STOP = "io.github.gohoski.notpipe.music.STOP";
    public static final String EXTRA_IDS = "ids";
    public static final String EXTRA_TITLES = "titles";
    public static final String EXTRA_START = "start";

    private static final int NOTIFICATION_ID = 417;
    private static final int MAX_FAILS_IN_ROW = 3;

    private static MusicService running;

    private MediaPlayer player;
    private Handler handler = new Handler();
    private List<VideoInfo> queue = new ArrayList<VideoInfo>();
    private int index = 0;
    private boolean prepared = false;
    private boolean paused = false;
    private int failsInRow = 0;
    private int requestToken = 0;

    /**
     * Starts playing the given videos as audio, beginning at {@code start}.
     */
    public static void play(Context context, List<VideoInfo> videos, int start) {
        String[] ids = new String[videos.size()];
        String[] titles = new String[videos.size()];
        for (int i = 0; i < videos.size(); i++) {
            ids[i] = videos.get(i).id;
            titles[i] = videos.get(i).title;
        }
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(ACTION_PLAY);
        intent.putExtra(EXTRA_IDS, ids);
        intent.putExtra(EXTRA_TITLES, titles);
        intent.putExtra(EXTRA_START, start);
        context.startService(intent);
    }

    public static void sendAction(Context context, String action) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(action);
        context.startService(intent);
    }

    public static boolean isActive() {
        return running != null && !running.queue.isEmpty();
    }

    public static boolean isPaused() {
        return running != null && running.paused;
    }

    /** @return title of the current track or null if nothing is playing */
    public static String getCurrentTitle() {
        if (!isActive()) return null;
        return running.queue.get(running.index).title;
    }

    public static String getCurrentId() {
        if (!isActive()) return null;
        return running.queue.get(running.index).id;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        running = this;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onStart(Intent intent, int startId) {
        handleIntent(intent);
    }

    /** Android 2.0+; not an @Override so it still compiles against the Android 1.5 SDK */
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return 2; // START_NOT_STICKY
    }

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        if (ACTION_PLAY.equals(action)) {
            String[] ids = intent.getStringArrayExtra(EXTRA_IDS);
            String[] titles = intent.getStringArrayExtra(EXTRA_TITLES);
            if (ids == null || ids.length == 0) return;
            queue.clear();
            for (int i = 0; i < ids.length; i++) {
                String title = titles != null && i < titles.length ? titles[i] : ids[i];
                queue.add(new VideoInfo(ids[i], title, "", "", "", "", "", -1, null));
            }
            index = Math.max(0, Math.min(intent.getIntExtra(EXTRA_START, 0), queue.size() - 1));
            failsInRow = 0;
            playCurrent();
        } else if (ACTION_TOGGLE.equals(action)) {
            toggle();
        } else if (ACTION_NEXT.equals(action)) {
            skip(1);
        } else if (ACTION_PREVIOUS.equals(action)) {
            skip(-1);
        } else if (ACTION_STOP.equals(action)) {
            stopSelf();
        }
    }

    private void toggle() {
        if (player == null || !prepared) return;
        if (paused) {
            player.start();
            paused = false;
        } else {
            player.pause();
            paused = true;
        }
        updateNotification(getString(paused ? R.string.paused : R.string.playing));
    }

    private void skip(int delta) {
        if (queue.isEmpty()) return;
        int next = index + delta;
        if (next < 0 || next >= queue.size()) {
            if (delta > 0) stopSelf();
            return;
        }
        index = next;
        playCurrent();
    }

    private void playCurrent() {
        releasePlayer();
        paused = false;
        final VideoInfo video = queue.get(index);
        final int token = ++requestToken;
        Compat.startForeground(this, NOTIFICATION_ID, buildNotification(getString(R.string.loading_audio)));

        File local = DownloadService.findDownloaded(video.id);
        if (local != null) {
            startPlayer(local.getAbsolutePath(), token);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = null;
                try {
                    url = Manager.getInstance().getAudioUrl(video.id);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                final String resolved = url;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (token != requestToken || running == null) return;
                        if (resolved == null) {
                            onTrackFailed();
                        } else {
                            startPlayer(resolved, token);
                        }
                    }
                });
            }
        }).start();
    }

    private void startPlayer(String source, int token) {
        if (token != requestToken) return;
        try {
            player = new MediaPlayer();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            player.setOnPreparedListener(this);
            player.setOnCompletionListener(this);
            player.setOnErrorListener(this);
            player.setDataSource(source);
            prepared = false;
            player.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            onTrackFailed();
        }
    }

    private void onTrackFailed() {
        failsInRow++;
        Toast.makeText(this, R.string.audio_failed, Toast.LENGTH_SHORT).show();
        if (failsInRow >= MAX_FAILS_IN_ROW || index >= queue.size() - 1) {
            stopSelf();
        } else {
            skip(1);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        prepared = true;
        failsInRow = 0;
        mp.start();
        updateNotification(getString(R.string.playing));
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        skip(1);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        releasePlayer();
        onTrackFailed();
        return true;
    }

    private void releasePlayer() {
        prepared = false;
        if (player != null) {
            try {
                player.reset();
                player.release();
            } catch (Exception ignored) {}
            player = null;
        }
    }

    private void updateNotification(String status) {
        Compat.startForeground(this, NOTIFICATION_ID, buildNotification(status));
    }

    @SuppressWarnings("deprecation")
    private Notification buildNotification(String status) {
        String title = queue.isEmpty() ? getString(R.string.app_name) : queue.get(index).title;
        Notification notification = new Notification(android.R.drawable.ic_media_play, title, System.currentTimeMillis());
        notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

        RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification);
        views.setTextViewText(R.id.notification_title, title);
        String position = queue.size() > 1 ? " · " + (index + 1) + "/" + queue.size() : "";
        views.setTextViewText(R.id.notification_text, status + position);
        views.setViewVisibility(R.id.notification_buttons, android.view.View.VISIBLE);
        views.setImageViewResource(R.id.notification_toggle, paused ? android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause);
        views.setOnClickPendingIntent(R.id.notification_toggle, actionIntent(ACTION_TOGGLE, 1));
        views.setOnClickPendingIntent(R.id.notification_next, actionIntent(ACTION_NEXT, 2));
        views.setOnClickPendingIntent(R.id.notification_stop, actionIntent(ACTION_STOP, 3));
        notification.contentView = views;
        // Buttons inside notifications aren't clickable before Android 3.0, so a tap toggles playback there
        notification.contentIntent = NotPipe.SDK < 11 ? actionIntent(ACTION_TOGGLE, 4)
                : PendingIntent.getActivity(this, 0, new Intent(this, LibraryActivity.class), 0);
        return notification;
    }

    private PendingIntent actionIntent(String action, int requestCode) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, requestCode, intent, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requestToken++;
        releasePlayer();
        queue.clear();
        Compat.stopForeground(this);
        running = null;
    }
}
