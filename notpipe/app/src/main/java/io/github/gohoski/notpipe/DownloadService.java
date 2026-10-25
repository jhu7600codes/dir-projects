package io.github.gohoski.notpipe;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.StatFs;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;
import java.util.LinkedList;

import io.github.gohoski.notpipe.api.Manager;
import io.github.gohoski.notpipe.config.Config;
import io.github.gohoski.notpipe.config.ConfigManager;
import io.github.gohoski.notpipe.http.HttpClient;
import io.github.gohoski.notpipe.util.Compat;

/**
 * Created by Claude on 25.10.2026.
 * Downloads videos or their audio to the SD card (notPipe/downloads) one at a time, with a progress notification.
 * Files are named "Title [videoId].mp4" / ".m4a" so they can be found again by ID for offline playback.
 */

public class DownloadService extends Service {
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_TITLE = "title";
    /** "360", "480", "720", "1080" for video or {@link #AUDIO} */
    public static final String EXTRA_QUALITY = "quality";
    public static final String AUDIO = "audio";
    public static final String DIR_DOWNLOADS = "notPipe/downloads";

    private static final int NOTIFICATION_ID = 418;
    private static final int DONE_NOTIFICATION_ID = 419;
    private static final long MIN_FREE_SPACE = 50L * 1024L * 1024L;

    private final LinkedList<Job> jobs = new LinkedList<Job>();
    private Handler handler = new Handler();
    private Thread worker;
    private int finished = 0, failed = 0;

    private static class Job {
        String id, title, quality;
    }

    public static void enqueue(Context context, String id, String title, String quality) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra(EXTRA_ID, id);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_QUALITY, quality);
        context.startService(intent);
    }

    public static File getDownloadsDir() {
        return new File(Environment.getExternalStorageDirectory(), DIR_DOWNLOADS);
    }

    /**
     * @return the downloaded file (video or audio) for the video, or null if it wasn't downloaded
     */
    public static File findDownloaded(String videoId) {
        return findDownloaded(videoId, null);
    }

    /**
     * @param audio null for any file, true for audio (.m4a) only, false for video only
     */
    public static File findDownloaded(String videoId, Boolean audio) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) return null;
        File[] files = getDownloadsDir().listFiles();
        if (files == null) return null;
        String tag = "[" + videoId + "]";
        for (int i = 0; i < files.length; i++) {
            String name = files[i].getName();
            if (!name.contains(tag) || name.endsWith(".part")) continue;
            if (audio == null || audio.booleanValue() == name.endsWith(".m4a")) return files[i];
        }
        return null;
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
        if (intent == null || intent.getStringExtra(EXTRA_ID) == null) return;
        Job job = new Job();
        job.id = intent.getStringExtra(EXTRA_ID);
        job.title = intent.getStringExtra(EXTRA_TITLE);
        job.quality = intent.getStringExtra(EXTRA_QUALITY);
        if (job.title == null) job.title = job.id;
        if (job.quality == null) job.quality = "360";
        synchronized (jobs) {
            jobs.add(job);
        }
        Toast.makeText(this, getString(R.string.download_queued, job.title), Toast.LENGTH_SHORT).show();
        if (worker == null) {
            worker = new Thread(new Runnable() {
                @Override
                public void run() {
                    processQueue();
                }
            });
            worker.start();
        }
    }

    private void processQueue() {
        while (true) {
            final Job job;
            final int remaining;
            synchronized (jobs) {
                if (jobs.isEmpty()) {
                    worker = null;
                    break;
                }
                job = jobs.removeFirst();
                remaining = jobs.size();
            }
            boolean ok = download(job, remaining);
            if (ok) finished++; else failed++;
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (jobs) {
                    if (worker != null || !jobs.isEmpty()) return;
                }
                showDoneNotification();
                stopSelf();
            }
        });
    }

    private boolean download(final Job job, final int remaining) {
        showProgress(job, remaining, getString(R.string.preparing_download), -1);
        try {
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                toast(getString(R.string.sd_card));
                return false;
            }
            File dir = getDownloadsDir();
            if (!dir.exists()) dir.mkdirs();
            StatFs stat = new StatFs(dir.getPath());
            if ((long) stat.getBlockSize() * (long) stat.getAvailableBlocks() < MIN_FREE_SPACE) {
                toast(getString(R.string.sd_card_space));
                return false;
            }
            if (findDownloaded(job.id, Boolean.valueOf(AUDIO.equals(job.quality))) != null) {
                return true;
            }

            Manager manager = Manager.getInstance();
            Config config = ConfigManager.getInstance().getConfig();
            String url;
            String extension;
            int timeout = HttpClient.VIDEO_TIMEOUT;
            if (AUDIO.equals(job.quality)) {
                url = manager.getAudioOnlyUrl(job.id);
                extension = ".m4a";
                if (url == null) {
                    url = manager.getVideoUrl(job.id, "360", timeout);
                    extension = ".mp4";
                }
            } else if (config.isConvertVideos()) {
                // The device can't play H.264, so save the converted file to be able to watch it later
                url = manager.getConvUrl(job.id, job.quality, config.getConvertCodec());
                extension = config.getConvertCodec() == 1 ? ".3gp" : ".mp4";
                timeout = HttpClient.CONVERSION_TIMEOUT;
            } else {
                url = manager.getVideoUrl(job.id, job.quality, timeout);
                extension = ".mp4";
            }

            String name = sanitize(job.title) + " [" + job.id + "]" + extension;
            File target = new File(dir, name);
            File part = new File(dir, name + ".part");
            HttpClient.downloadToFile(url, part.getAbsolutePath(), timeout, new HttpClient.DownloadProgressListener() {
                private long lastUpdate = 0;

                @Override
                public boolean onProgress(long bytesDownloaded, long totalBytes) {
                    long now = System.currentTimeMillis();
                    if (now - lastUpdate > 1000) {
                        lastUpdate = now;
                        int percent = totalBytes > 0 ? (int) (bytesDownloaded * 100 / totalBytes) : -1;
                        String text = percent >= 0 ? getString(R.string.percent, percent)
                                : String.format("%.1f MB", bytesDownloaded / 1048576.0);
                        showProgress(job, remaining, text, percent);
                    }
                    return true;
                }
            });
            if (!part.renameTo(target)) {
                part.delete();
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            toast(getString(R.string.download_failed, job.title));
            return false;
        }
    }

    private static String sanitize(String title) {
        String clean = title.replaceAll("[\\\\/:*?\"<>|\\[\\]]", "_").trim();
        if (clean.length() > 80) clean = clean.substring(0, 80).trim();
        return clean.length() == 0 ? "video" : clean;
    }

    private void toast(final String text) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DownloadService.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void showProgress(final Job job, final int remaining, final String text, final int percent) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Notification notification = new Notification(android.R.drawable.stat_sys_download,
                        getString(R.string.downloading), System.currentTimeMillis());
                notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification);
                views.setTextViewText(R.id.notification_title, job.title);
                views.setTextViewText(R.id.notification_text, remaining > 0
                        ? text + " · " + getString(R.string.downloads_queued, remaining) : text);
                views.setViewVisibility(R.id.notification_progress, android.view.View.VISIBLE);
                views.setProgressBar(R.id.notification_progress, 100, Math.max(percent, 0), percent < 0);
                notification.contentView = views;
                notification.contentIntent = PendingIntent.getActivity(DownloadService.this, 0,
                        new Intent(DownloadService.this, DownloadsActivity.class), 0);
                Compat.startForeground(DownloadService.this, NOTIFICATION_ID, notification);
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void showDoneNotification() {
        Compat.stopForeground(this);
        if (finished == 0 && failed == 0) return;
        String text = failed > 0 ? getString(R.string.downloads_done_failed, finished, failed)
                : getString(R.string.downloads_done, finished);
        Notification notification = new Notification(android.R.drawable.stat_sys_download_done, text, System.currentTimeMillis());
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification);
        views.setTextViewText(R.id.notification_title, getString(R.string.downloads));
        views.setTextViewText(R.id.notification_text, text);
        notification.contentView = views;
        notification.contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, DownloadsActivity.class), 0);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(DONE_NOTIFICATION_ID, notification);
    }
}
