package io.github.gohoski.notpipe;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.github.gohoski.notpipe.api.Manager;
import io.github.gohoski.notpipe.config.Config;
import io.github.gohoski.notpipe.config.ConfigManager;
import io.github.gohoski.notpipe.data.VideoInfo;
import io.github.gohoski.notpipe.http.HttpClient;
import io.github.gohoski.notpipe.ui.NavBar;
import io.github.gohoski.notpipe.ui.VideoActions;
import io.github.gohoski.notpipe.util.ChannelIconResolver;
import io.github.gohoski.notpipe.util.Feeds;
import io.github.gohoski.notpipe.util.ImageLoader;
import io.github.gohoski.notpipe.util.LocalLibrary;
import io.github.gohoski.notpipe.util.PlayerExtras;

/**
 * Created by Claude on 25.10.2026.
 * Full-screen Shorts player. Swipe up/down, the arrows or the D-pad switch videos; a tap pauses.
 * Without an extra it plays Shorts of the subscribed channels (or #shorts search results), with EXTRA_CHANNEL a channel's Shorts.
 */

public class ShortsActivity extends Activity {
    public static final String EXTRA_CHANNEL = "channel";
    /** Start at this Short (opened from the home feed's Shorts shelf) */
    public static final String EXTRA_START_ID = "start_id";
    public static final String EXTRA_START_TITLE = "start_title";
    public static final String EXTRA_START_THUMBNAIL = "start_thumbnail";
    public static final String EXTRA_START_CHANNEL = "start_channel";
    public static final String EXTRA_START_CHANNEL_ID = "start_channel_id";
    private static final int MIN_FLING_DISTANCE = 80;
    private static final int MAX_RETRIES = 2;

    private final List<VideoInfo> shorts = new ArrayList<VideoInfo>();
    private int index = 0;
    private VideoView videoView;
    private ImageView thumbnail;
    private Context context;
    private Config config;
    private Handler handler = new Handler();
    private GestureDetector gestures;
    private boolean prepared = false;
    private boolean isDestroyedFlag = false;
    private int requestToken = 0;
    private int retries = 0;
    private final Runnable progressTicker = new Runnable() {
        @Override
        public void run() {
            if (isDestroyedFlag || !prepared) return;
            try {
                int duration = videoView.getDuration();
                if (duration > 0) {
                    ((android.widget.ProgressBar) findViewById(R.id.short_progress))
                            .setProgress((int) (1000L * videoView.getCurrentPosition() / duration));
                }
            } catch (Exception ignored) {}
            handler.postDelayed(this, 250);
        }
    };
    private final java.util.Map<String, String> prefetched =
            java.util.Collections.synchronizedMap(new java.util.HashMap<String, String>());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_shorts);
        NavBar.attach(this, NavBar.SHORTS);
        context = this;
        config = ConfigManager.getInstance().getConfig();

        videoView = (VideoView) findViewById(R.id.short_video);
        thumbnail = (ImageView) findViewById(R.id.short_thumbnail);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                prepared = true;
                fitVideo(mp.getVideoWidth(), mp.getVideoHeight());
                handler.removeCallbacks(progressTicker);
                handler.post(progressTicker);
                mp.setLooping(true);
                findViewById(R.id.short_loading).setVisibility(View.GONE);
                thumbnail.setVisibility(View.GONE);
                videoView.start();
            }
        });
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                // Stream errors (often a dying instance): resolve again, Manager picks instances at random
                if (retries < MAX_RETRIES && !shorts.isEmpty()) {
                    retries++;
                    playCurrent();
                    return true;
                }
                Toast.makeText(context, R.string.unsupported_format, Toast.LENGTH_LONG).show();
                findViewById(R.id.short_loading).setVisibility(View.GONE);
                thumbnail.setVisibility(View.VISIBLE);
                return true;
            }
        });

        gestures = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                togglePause();
                return true;
            }

            /** Double-tap likes, like on YouTube */
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (!shorts.isEmpty()) VideoActions.like(ShortsActivity.this, shorts.get(index), true);
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float dy = e2.getY() - e1.getY();
                if (Math.abs(dy) < MIN_FLING_DISTANCE || Math.abs(dy) < Math.abs(e2.getX() - e1.getX())) return false;
                move(dy < 0 ? 1 : -1);
                return true;
            }
        });
        findViewById(R.id.shorts_root).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestures.onTouchEvent(event);
            }
        });
        View.OnClickListener actions = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shorts.isEmpty()) return;
                VideoInfo video = shorts.get(index);
                int id = v.getId();
                if (id == R.id.short_like) {
                    VideoActions.like(ShortsActivity.this, video, true);
                } else if (id == R.id.short_dislike) {
                    VideoActions.like(ShortsActivity.this, video, false);
                } else if (id == R.id.short_comments) {
                    io.github.gohoski.notpipe.ui.CommentsDialog.show(ShortsActivity.this, video.id);
                } else if (id == R.id.short_share) {
                    VideoActions.share(ShortsActivity.this, video);
                } else if (id == R.id.short_download) {
                    VideoActions.showDownloadDialog(ShortsActivity.this, video);
                }
            }
        };
        findViewById(R.id.short_subscribe).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSubscribe();
            }
        });
        View.OnClickListener openChannel = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shorts.isEmpty()) return;
                VideoInfo video = shorts.get(index);
                if (video.channelId == null || video.channelId.length() == 0) return;
                Intent intent = new Intent(context, ChannelActivity.class);
                intent.putExtra("ID", video.channelId);
                startActivity(intent);
            }
        };
        findViewById(R.id.short_avatar).setOnClickListener(openChannel);
        findViewById(R.id.short_channel).setOnClickListener(openChannel);
        findViewById(R.id.short_like).setOnClickListener(actions);
        findViewById(R.id.short_dislike).setOnClickListener(actions);
        findViewById(R.id.short_share).setOnClickListener(actions);
        findViewById(R.id.short_comments).setOnClickListener(actions);
        findViewById(R.id.short_download).setOnClickListener(actions);
        findViewById(R.id.short_more).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOptionsMenu();
            }
        });

        // Music would play over the Shorts otherwise
        if (MusicService.isActive() && !MusicService.isPaused()) {
            MusicService.sendAction(this, MusicService.ACTION_TOGGLE);
        }
        new LoadShortsTask().execute(getIntent().getStringExtra(EXTRA_CHANNEL));
    }

    /** Scales the video up to fill the screen (keeping its shape) instead of showing it at its pixel size */
    private void fitVideo(int videoWidth, int videoHeight) {
        View root = findViewById(R.id.shorts_root);
        int w = root.getWidth(), h = root.getHeight();
        if (videoWidth <= 0 || videoHeight <= 0 || w <= 0 || h <= 0) return;
        float scale = Math.min(w / (float) videoWidth, h / (float) videoHeight);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                Math.round(videoWidth * scale), Math.round(videoHeight * scale), android.view.Gravity.CENTER);
        videoView.setLayoutParams(params);
        videoView.getHolder().setFixedSize(params.width, params.height);
    }

    private void showChannel(final VideoInfo video) {
        ((TextView) findViewById(R.id.short_channel)).setText(video.channel);
        final ImageView avatar = (ImageView) findViewById(R.id.short_avatar);
        avatar.setImageBitmap(null);
        String icon = video.channelThumbnail;
        if ((icon == null || icon.length() == 0) && video.channelId != null && video.channelId.length() > 0) {
            icon = ChannelIconResolver.getResolved(video.channelId);
            if (icon == null) {
                ChannelIconResolver.requestFallback(video.channelId, new ChannelIconResolver.OnIconResolvedListener() {
                    @Override
                    public void onIconResolved(String channelId, String url) {
                        if (!isDestroyedFlag && url != null && url.length() > 0 && !shorts.isEmpty()
                                && channelId.equals(shorts.get(index).channelId)) {
                            ImageLoader.loadImage(url, avatar, false);
                        }
                    }
                });
            }
        }
        if (icon != null && icon.length() > 0 && !ChannelIconResolver.FAILED.equals(icon)) {
            ImageLoader.loadImage(icon, avatar, false);
        }
        updateSubscribeButton(video);
    }

    private void updateSubscribeButton(VideoInfo video) {
        TextView button = (TextView) findViewById(R.id.short_subscribe);
        boolean hasChannel = video.channelId != null && video.channelId.startsWith("UC");
        button.setVisibility(hasChannel ? View.VISIBLE : View.GONE);
        boolean subscribed = hasChannel && LocalLibrary.getInstance().isSubscribed(video.channelId);
        button.setSelected(subscribed);
        button.setText(subscribed ? R.string.subscribed_short : R.string.subscribe);
    }

    private void toggleSubscribe() {
        if (shorts.isEmpty()) return;
        VideoInfo video = shorts.get(index);
        if (video.channelId == null || !video.channelId.startsWith("UC")) return;
        LocalLibrary library = LocalLibrary.getInstance();
        if (library.isSubscribed(video.channelId)) {
            library.unsubscribe(video.channelId);
        } else {
            library.subscribe(video.channelId, video.channel, video.channelThumbnail);
            Toast.makeText(context, getString(R.string.subscribed, video.channel), Toast.LENGTH_SHORT).show();
        }
        updateSubscribeButton(video);
    }

    private void togglePause() {
        if (!prepared) return;
        if (videoView.isPlaying()) videoView.pause(); else videoView.start();
    }

    private void move(int delta) {
        int next = index + delta;
        if (shorts.isEmpty() || next < 0 || next >= shorts.size()) return;
        index = next;
        retries = 0;
        playCurrent();
    }

    private void playCurrent() {
        final VideoInfo video = shorts.get(index);
        final int token = ++requestToken;
        prepared = false;
        try {
            videoView.stopPlayback();
        } catch (Exception ignored) {}
        ((TextView) findViewById(R.id.short_title)).setText(video.title);
        showChannel(video);
        findViewById(R.id.short_loading).setVisibility(View.VISIBLE);
        thumbnail.setVisibility(View.VISIBLE);
        ImageLoader.loadImage(video.thumbnail, thumbnail, false);
        LocalLibrary.getInstance().addToHistory(video);

        new Thread(new Runnable() {
            @Override
            public void run() {
                String source = retries > 0 ? null : prefetched.remove(video.id);
                if (source == null) {
                    try {
                        source = resolveSource(video.id);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                prefetchNext(token);
                final String resolved = source;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isDestroyedFlag || token != requestToken) return;
                        if (resolved == null) {
                            findViewById(R.id.short_loading).setVisibility(View.GONE);
                            Toast.makeText(context, R.string.audio_failed, Toast.LENGTH_SHORT).show();
                            move(1);
                            return;
                        }
                        videoView.setVideoURI(Uri.parse(resolved));
                    }
                });
            }
        }).start();
    }

    /**
     * Resolves the next Short's URL in the background so swiping to it starts right away.
     */
    private void prefetchNext(int token) {
        int next = index + 1;
        if (token != requestToken || next >= shorts.size()) return;
        String id = shorts.get(next).id;
        if (prefetched.containsKey(id)) return;
        try {
            String url = resolveSource(id);
            if (url != null) prefetched.put(id, url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Downloaded file if there is one; with conversion on, the converted file is downloaded first
     * like in VideoActivity since converted streams can't be played while they're generated.
     */
    private String resolveSource(String id) throws Exception {
        File local = DownloadService.findDownloaded(id, false);
        if (local != null) return Uri.fromFile(local).toString();
        Manager manager = Manager.getInstance();
        if (config.isConvertVideos() && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File dir = new File(Environment.getExternalStorageDirectory(), "notPipe/videos");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, id + ".mp4");
            if (!file.exists()) {
                String url = manager.getConvUrl(id, "360", config.getConvertCodec());
                HttpClient.downloadToFile(url, file.getAbsolutePath(), HttpClient.CONVERSION_TIMEOUT, null);
            }
            return Uri.fromFile(file).toString();
        }
        String quality = PlayerExtras.getShortsQuality();
        if (!"360".equals(quality)) {
            // HD needs the server to prepare it; the next Short is resolved while this one plays, which hides most of it
            try {
                return manager.getVideoUrl(id, quality, HttpClient.CONVERSION_TIMEOUT);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return manager.getVideoUrl(id, "360", HttpClient.VIDEO_TIMEOUT);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            move(1);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            move(-1);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            togglePause();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_shorts, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (shorts.isEmpty()) return true;
        VideoInfo video = shorts.get(index);
        int id = item.getItemId();
        if (id == R.id.action_open_video) {
            Intent intent = new Intent(this, VideoActivity.class);
            intent.putExtra("ID", video.id);
            startActivity(intent);
        } else if (id == R.id.action_channel) {
            if (video.channelId == null || video.channelId.length() == 0) return true;
            Intent intent = new Intent(this, ChannelActivity.class);
            intent.putExtra("ID", video.channelId);
            startActivity(intent);
        } else if (id == R.id.action_listen_background) {
            if (prepared) videoView.pause();
            MusicService.play(this, shorts.subList(index, shorts.size()), 0);
            Toast.makeText(this, R.string.playing_in_background, Toast.LENGTH_SHORT).show();
        } else if (id == R.id.action_download) {
            VideoActions.showDownloadDialog(this, video);
        } else if (id == R.id.action_shorts_quality) {
            final String[] values = {"360", "480", "720", "1080"};
            String current = PlayerExtras.getShortsQuality();
            int checked = 0;
            for (int i = 0; i < values.length; i++) if (values[i].equals(current)) checked = i;
            new android.app.AlertDialog.Builder(this).setTitle(R.string.shorts_quality)
                    .setSingleChoiceItems(getResources().getStringArray(R.array.qualities), checked,
                            new android.content.DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(android.content.DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    PlayerExtras.setShortsQuality(values[which]);
                                    prefetched.clear();
                                    retries = 0;
                                    playCurrent();
                                }
                            }).show();
        } else if (id == R.id.action_add_playlist) {
            VideoActions.showAddToPlaylistDialog(this, video);
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (prepared && videoView.isPlaying()) videoView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prepared) videoView.start();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ImageLoader.clearCache();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyedFlag = true;
        requestToken++;
        try {
            videoView.stopPlayback();
        } catch (Exception ignored) {}
        if (isFinishing()) ImageLoader.clearCache();
    }

    private class LoadShortsTask extends AsyncTask<String, Void, List<VideoInfo>> {
        @Override
        protected List<VideoInfo> doInBackground(String... params) {
            try {
                return Feeds.loadShorts(params[0]);
            } catch (Exception e) {
                e.printStackTrace();
                return new ArrayList<VideoInfo>();
            }
        }

        @Override
        protected void onPostExecute(List<VideoInfo> result) {
            if (isDestroyedFlag) return;
            findViewById(R.id.short_loading).setVisibility(View.GONE);
            if (result.isEmpty()) {
                TextView message = (TextView) findViewById(R.id.short_message);
                message.setText(Manager.getInstance().getShortsApi() == null ? R.string.no_shorts_instances : R.string.no_shorts);
                message.setVisibility(View.VISIBLE);
                findViewById(R.id.short_info).setVisibility(View.GONE);
                return;
            }
            shorts.addAll(result);
            index = 0;
            String startId = getIntent().getStringExtra(EXTRA_START_ID);
            if (startId != null) {
                VideoInfo start = null;
                for (int i = 0; i < shorts.size(); i++) {
                    if (startId.equals(shorts.get(i).id)) start = shorts.remove(i);
                }
                if (start == null) {
                    Intent in = getIntent();
                    start = new VideoInfo(startId, in.getStringExtra(EXTRA_START_TITLE), in.getStringExtra(EXTRA_START_THUMBNAIL),
                            in.getStringExtra(EXTRA_START_CHANNEL), "", in.getStringExtra(EXTRA_START_CHANNEL_ID), "", -1, null);
                }
                shorts.add(0, start);
            }
            playCurrent();
        }
    }
}
