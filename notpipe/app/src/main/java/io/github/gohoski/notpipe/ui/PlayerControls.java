package io.github.gohoski.notpipe.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.Handler;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import io.github.gohoski.notpipe.R;

/**
 * Created by Claude on 28.10.2026.
 * YouTube-style controls drawn over the video instead of the stock MediaController:
 * previous / play-pause / next in the middle (±10 s on double-tap), a seek bar with times, quick buttons (speed, subtitles, quality, PiP, more)
 * and gestures: tap shows/hides, double-tap seeks, vertical swipes change brightness (left) and volume (right).
 */

public class PlayerControls {
    public interface Host {
        void onToggleFullscreen();
        void onSpeed();
        void onCaptions();
        void onQuality();
        void onPip();
        void onMore();
        void onNext();
        void onPrevious();
        boolean isFullscreen();
    }

    private static final int HIDE_DELAY = 3500;
    private static final int SEEK_STEP = 10000;

    private final Activity activity;
    private final VideoView video;
    private final Host host;
    private final FrameLayout overlay;
    private final ImageView playPause;
    private final SeekBar seekBar;
    private final TextView position, duration, speedButton, qualityButton, gestureLabel;
    private final Handler handler = new Handler();
    private final GestureDetector gestures;
    private final AudioManager audio;
    private boolean enabled = false;
    private boolean dragging = false;
    /** 0 = none yet, 1 = brightness, 2 = volume, 3 = horizontal (ignored) */
    private int swipeMode = 0;
    private float swipeStartValue;

    private final Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            if (overlay.getVisibility() == View.VISIBLE) handler.postDelayed(this, 500);
        }
    };

    private final Runnable hideLabel = new Runnable() {
        @Override
        public void run() {
            gestureLabel.setVisibility(View.GONE);
        }
    };

    public PlayerControls(Activity activity, FrameLayout frame, VideoView video, Host host) {
        this.activity = activity;
        this.video = video;
        this.host = host;
        this.audio = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);

        overlay = new FrameLayout(activity);
        overlay.setBackgroundColor(0x66000000);
        overlay.setVisibility(View.GONE);

        // Center: previous 10 s, play/pause, next 10 s
        LinearLayout center = new LinearLayout(activity);
        center.setGravity(Gravity.CENTER);
        center.addView(icon(R.drawable.pl_prev, 40, new View.OnClickListener() {
            public void onClick(View v) { host().onPrevious(); }
        }));
        playPause = icon(R.drawable.pl_pause, 64, new View.OnClickListener() {
            public void onClick(View v) { togglePlay(); }
        });
        LinearLayout.LayoutParams bigParams = new LinearLayout.LayoutParams(dp(64), dp(64));
        bigParams.leftMargin = bigParams.rightMargin = dp(36);
        center.addView(playPause, bigParams);
        center.addView(icon(R.drawable.pl_next, 40, new View.OnClickListener() {
            public void onClick(View v) { host().onNext(); }
        }));
        overlay.addView(center, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));

        // Top right: quick settings
        LinearLayout top = new LinearLayout(activity);
        top.setGravity(Gravity.CENTER_VERTICAL);
        speedButton = textButton("1x", new View.OnClickListener() {
            public void onClick(View v) { host().onSpeed(); scheduleHide(); }
        });
        top.addView(speedButton);
        top.addView(textButton("CC", new View.OnClickListener() {
            public void onClick(View v) { host().onCaptions(); scheduleHide(); }
        }));
        qualityButton = textButton("360p", new View.OnClickListener() {
            public void onClick(View v) { host().onQuality(); scheduleHide(); }
        });
        top.addView(qualityButton);
        top.addView(icon(R.drawable.pl_pip, 30, new View.OnClickListener() {
            public void onClick(View v) { host().onPip(); }
        }));
        top.addView(icon(R.drawable.pl_more, 30, new View.OnClickListener() {
            public void onClick(View v) { host().onMore(); scheduleHide(); }
        }));
        overlay.addView(top, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT));

        // Bottom: time, seek bar, duration, next, fullscreen
        LinearLayout bottom = new LinearLayout(activity);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(dp(8), 0, dp(4), 0);
        position = timeText();
        bottom.addView(position);
        seekBar = new SeekBar(activity);
        seekBar.setMax(1000);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) position.setText(time(target(progress)));
            }

            public void onStartTrackingTouch(SeekBar bar) {
                dragging = true;
                handler.removeCallbacks(hideRunnable);
            }

            public void onStopTrackingTouch(SeekBar bar) {
                dragging = false;
                try {
                    PlayerControls.this.video.seekTo(target(bar.getProgress()));
                } catch (Exception ignored) {}
                scheduleHide();
            }
        });
        bottom.addView(seekBar, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        duration = timeText();
        bottom.addView(duration);
        bottom.addView(icon(R.drawable.pl_fullscreen, 30, new View.OnClickListener() {
            public void onClick(View v) { host().onToggleFullscreen(); scheduleHide(); }
        }));
        overlay.addView(bottom, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

        frame.addView(overlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));

        // Feedback for gestures ("+10s", "Brightness 60%", …), shown even when the controls are hidden
        gestureLabel = new TextView(activity);
        gestureLabel.setTextColor(Color.WHITE);
        gestureLabel.setTextSize(16);
        gestureLabel.setTypeface(null, Typeface.BOLD);
        gestureLabel.setBackgroundColor(0xAA000000);
        gestureLabel.setPadding(dp(12), dp(6), dp(12), dp(6));
        gestureLabel.setVisibility(View.GONE);
        frame.addView(gestureLabel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL));

        gestures = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                swipeMode = 0;
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (overlay.getVisibility() == View.VISIBLE) hide(); else show();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                boolean forward = e.getX() > overlay.getWidth() / 2;
                seekBy(forward ? SEEK_STEP : -SEEK_STEP);
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                // Brightness/volume swipes only in full screen, so scrolling the video page still works
                if (e1 == null || !host().isFullscreen()) return false;
                if (swipeMode == 0) {
                    if (Math.abs(e2.getY() - e1.getY()) < dp(16)) return false;
                    if (Math.abs(e2.getX() - e1.getX()) > Math.abs(e2.getY() - e1.getY())) {
                        swipeMode = 3;
                        return false;
                    }
                    swipeMode = e1.getX() < overlay.getWidth() / 2 ? 1 : 2;
                    swipeStartValue = swipeMode == 1 ? currentBrightness()
                            : audio.getStreamVolume(AudioManager.STREAM_MUSIC) / (float) audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                }
                if (swipeMode == 3) return false;
                float change = (e1.getY() - e2.getY()) / (overlay.getHeight() * 0.8f);
                float value = Math.max(0f, Math.min(1f, swipeStartValue + change));
                if (swipeMode == 1) {
                    WindowManager.LayoutParams params = PlayerControls.this.activity.getWindow().getAttributes();
                    params.screenBrightness = Math.max(0.02f, value);
                    PlayerControls.this.activity.getWindow().setAttributes(params);
                    label(PlayerControls.this.activity.getString(R.string.brightness) + " " + Math.round(value * 100) + "%");
                } else {
                    int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(value * max), 0);
                    label(PlayerControls.this.activity.getString(R.string.volume) + " " + Math.round(value * 100) + "%");
                }
                return true;
            }
        });
    }

    private Host host() {
        return host;
    }

    /** Call when the video is prepared; until then taps do nothing */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) hide();
    }

    public void setSpeed(float speed) {
        speedButton.setText((speed == (int) speed ? String.valueOf((int) speed) : String.valueOf(speed)) + "x");
    }

    public void setQuality(String quality) {
        qualityButton.setText(quality + "p");
    }

    /** @return true if the touch was used */
    public boolean onTouch(MotionEvent event) {
        if (!enabled) return false;
        return gestures.onTouchEvent(event);
    }

    public void show() {
        if (!enabled) return;
        updatePlayIcon();
        overlay.setVisibility(View.VISIBLE);
        handler.removeCallbacks(progressRunnable);
        handler.post(progressRunnable);
        scheduleHide();
    }

    public void hide() {
        overlay.setVisibility(View.GONE);
        handler.removeCallbacks(hideRunnable);
    }

    public void release() {
        handler.removeCallbacks(hideRunnable);
        handler.removeCallbacks(progressRunnable);
        handler.removeCallbacks(hideLabel);
    }

    private void scheduleHide() {
        handler.removeCallbacks(hideRunnable);
        handler.postDelayed(hideRunnable, HIDE_DELAY);
    }

    private void togglePlay() {
        try {
            if (video.isPlaying()) {
                video.pause();
                handler.removeCallbacks(hideRunnable);
            } else {
                video.start();
                scheduleHide();
            }
        } catch (Exception ignored) {}
        updatePlayIcon();
    }

    private void updatePlayIcon() {
        boolean playing;
        try {
            playing = video.isPlaying();
        } catch (Exception e) {
            playing = false;
        }
        playPause.setImageResource(playing ? R.drawable.pl_pause : R.drawable.pl_play);
    }

    private void seekBy(int delta) {
        if (!enabled) return;
        try {
            int target = Math.max(0, Math.min(video.getCurrentPosition() + delta, video.getDuration() - 1000));
            video.seekTo(target);
        } catch (Exception ignored) {}
        label(delta > 0 ? "+10s" : "-10s");
        updateProgress();
    }

    private void updateProgress() {
        if (dragging) return;
        try {
            int pos = video.getCurrentPosition(), dur = video.getDuration();
            position.setText(time(pos));
            duration.setText(dur > 0 ? time(dur) : "--:--");
            if (dur > 0) seekBar.setProgress((int) (1000L * pos / dur));
            seekBar.setSecondaryProgress(video.getBufferPercentage() * 10);
            updatePlayIcon();
        } catch (Exception ignored) {}
    }

    private int target(int progress) {
        try {
            return (int) ((long) video.getDuration() * progress / 1000);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String time(int ms) {
        int total = Math.max(0, ms / 1000);
        int h = total / 3600, m = (total / 60) % 60, sec = total % 60;
        String ss = (sec < 10 ? "0" : "") + sec;
        return h > 0 ? h + ":" + (m < 10 ? "0" : "") + m + ":" + ss : m + ":" + ss;
    }

    private float currentBrightness() {
        float b = activity.getWindow().getAttributes().screenBrightness;
        return b < 0 ? 0.5f : b;
    }

    private void label(String text) {
        gestureLabel.setText(text);
        gestureLabel.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideLabel);
        handler.postDelayed(hideLabel, 900);
    }

    private ImageView icon(int res, int sizeDp, View.OnClickListener listener) {
        ImageView view = new ImageView(activity);
        view.setImageResource(res);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int pad = dp(sizeDp >= 40 ? 4 : 8);
        view.setPadding(pad, pad, pad, pad);
        view.setClickable(true);
        view.setBackgroundResource(R.drawable.tv_focus);
        view.setOnClickListener(listener);
        view.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp + 8), dp(sizeDp + 8)));
        return view;
    }

    private TextView textButton(String text, View.OnClickListener listener) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTypeface(null, Typeface.BOLD);
        view.setTextSize(13);
        view.setPadding(dp(10), dp(10), dp(10), dp(10));
        view.setClickable(true);
        view.setBackgroundResource(R.drawable.tv_focus);
        view.setOnClickListener(listener);
        return view;
    }

    private TextView timeText() {
        TextView view = new TextView(activity);
        view.setTextColor(Color.WHITE);
        view.setTextSize(12);
        view.setText("0:00");
        view.setPadding(dp(4), 0, dp(4), 0);
        return view;
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, activity.getResources().getDisplayMetrics());
    }
}
