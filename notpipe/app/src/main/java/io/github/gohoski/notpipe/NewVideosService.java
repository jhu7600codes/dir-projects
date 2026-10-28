package io.github.gohoski.notpipe;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.widget.RemoteViews;

import java.util.List;

import io.github.gohoski.notpipe.data.VideoInfo;
import io.github.gohoski.notpipe.util.Feeds;
import io.github.gohoski.notpipe.util.LocalLibrary;

/**
 * Created by Claude on 28.10.2026.
 * Checks the local subscriptions every few hours and shows a notification about new uploads.
 * The check only notifies about videos newer than the previous check, so the first run is silent.
 */

public class NewVideosService extends IntentService {
    private static final String PREFS = "notPipe_new_videos";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_LAST_SEEN = "last_seen";
    private static final long INTERVAL = 3 * AlarmManager.INTERVAL_HOUR;
    private static final int NOTIFICATION_ID = 420;

    public NewVideosService() {
        super("NewVideosService");
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).commit();
        schedule(context);
    }

    /** (Re)schedules or cancels the periodic check; safe to call often */
    public static void schedule(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pending = PendingIntent.getService(context, 0, new Intent(context, NewVideosService.class), 0);
        alarms.cancel(pending);
        if (isEnabled(context)) {
            alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + INTERVAL,
                    INTERVAL, pending);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onHandleIntent(Intent intent) {
        if (!isEnabled(this) || LocalLibrary.getInstance().getSubscriptions().isEmpty()) return;
        List<VideoInfo> feed;
        try {
            feed = Feeds.loadSubscriptions();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        long lastSeen = prefs(this).getLong(KEY_LAST_SEEN, 0);
        long newest = lastSeen;
        int count = 0;
        VideoInfo first = null;
        for (int i = 0; i < feed.size(); i++) {
            VideoInfo v = feed.get(i);
            if (v.publishedAt == null) continue;
            long time = v.publishedAt.getTime();
            if (time > newest) newest = time;
            if (lastSeen > 0 && time > lastSeen) {
                count++;
                if (first == null) first = v;
            }
        }
        prefs(this).edit().putLong(KEY_LAST_SEEN, newest).commit();
        if (count == 0 || first == null) return;

        String title = getResources().getQuantityString(R.plurals.new_videos, count, count);
        String text = first.channel + ": " + first.title;
        Notification notification = new Notification(android.R.drawable.stat_notify_more, title, System.currentTimeMillis());
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification);
        views.setTextViewText(R.id.notification_title, title);
        views.setTextViewText(R.id.notification_text, text);
        notification.contentView = views;
        Intent open = new Intent(this, VideoListActivity.class);
        open.putExtra(VideoListActivity.EXTRA_SOURCE, VideoListActivity.SOURCE_FEED);
        open.putExtra(VideoListActivity.EXTRA_TITLE, getString(R.string.subscriptions));
        notification.contentIntent = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notification);
    }

    /** Alarms don't survive a reboot */
    public static class BootReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            schedule(context);
        }
    }
}
