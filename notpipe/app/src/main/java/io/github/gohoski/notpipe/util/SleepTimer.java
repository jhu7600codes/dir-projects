package io.github.gohoski.notpipe.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import io.github.gohoski.notpipe.MusicService;
import io.github.gohoski.notpipe.NotPipe;
import io.github.gohoski.notpipe.R;

/**
 * Created by Claude on 28.10.2026.
 * Stops the background player and pauses the video after the chosen time.
 */

public class SleepTimer {
    private static final int[] MINUTES = {0, 10, 15, 30, 45, 60, 90};
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static volatile boolean pauseVideo = false;
    private static long endsAt = 0;

    private static final Runnable FIRE = new Runnable() {
        @Override
        public void run() {
            endsAt = 0;
            pauseVideo = true;
            if (MusicService.isActive()) MusicService.sendAction(NotPipe.getAppContext(), MusicService.ACTION_STOP);
        }
    };

    private SleepTimer() {}

    /** Polled by the video player; true once after the timer went off */
    public static boolean consumeVideoPause() {
        if (!pauseVideo) return false;
        pauseVideo = false;
        return true;
    }

    public static void showDialog(final Activity activity) {
        String[] labels = new String[MINUTES.length];
        labels[0] = activity.getString(R.string.sleep_off);
        for (int i = 1; i < MINUTES.length; i++) labels[i] = activity.getString(R.string.sleep_minutes, MINUTES[i]);
        String title = activity.getString(R.string.sleep_timer);
        if (endsAt > 0) {
            title += " (" + activity.getString(R.string.sleep_left, (endsAt - System.currentTimeMillis()) / 60000 + 1) + ")";
        }
        new AlertDialog.Builder(activity).setTitle(title)
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        HANDLER.removeCallbacks(FIRE);
                        if (which == 0) {
                            endsAt = 0;
                            Toast.makeText(activity, R.string.sleep_cancelled, Toast.LENGTH_SHORT).show();
                        } else {
                            endsAt = System.currentTimeMillis() + MINUTES[which] * 60000L;
                            HANDLER.postDelayed(FIRE, MINUTES[which] * 60000L);
                            Toast.makeText(activity, activity.getString(R.string.sleep_set, MINUTES[which]), Toast.LENGTH_SHORT).show();
                        }
                    }
                }).show();
    }
}
