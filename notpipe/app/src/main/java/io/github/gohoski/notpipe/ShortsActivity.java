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
import io.github.gohoski.notpipe.ui.VideoActions;
import io.github.gohoski.notpipe.util.Feeds;
import io.github.gohoski.notpipe.util.ImageLoader;
import io.github.gohoski.notpipe.util.LocalLibrary;

/**
 * Created by Claude on 25.10.2026.
 * Full-screen Shorts player. Swipe up/down, the arrows or the D-pad switch videos; a tap pauses.
 * Without an extra it plays Shorts of the subscribed channels (or #shorts search results), with EXTRA_CHANNEL a channel's Shorts.
 */

public class ShortsActivity extends Activity {
    public static final String EXTRA_CHANNEL = "channel";
    private static final int MIN_FLING_DISTANCE = 80;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_shorts);
        context = this;
        config = ConfigManager.getInstance().getConfig();

        videoView = (VideoView) findViewById(R.id.short_video);
        thumbnail = (ImageView) findViewById(R.id.short_thumbnail);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                prepared = true;
                mp.setLooping(true);
                findViewById(R.id.short_loading).setVisibility(View.GONE);
                thumbnail.setVisibility(View.GONE);
                videoView.start();
            }
        });
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Toast.makeText(context, R.string.unsupported_format, Toast.LENGTH_SHORT).show();
                findViewById(R.id.short_loading).setVisibility(View.GONE);
                return true;
            }
        });

        gestures = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                togglePause();
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
        findViewById(R.id.short_previous).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                move(-1);
            }
        });
        findViewById(R.id.short_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                move(1);
            }
        });
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

    private void togglePause() {
        if (!prepared) return;
        if (videoView.isPlaying()) videoView.pause(); else videoView.start();
    }

    private void move(int delta) {
        int next = index + delta;
        if (shorts.isEmpty() || next < 0 || next >= shorts.size()) return;
        index = next;
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
        ((TextView) findViewById(R.id.short_channel)).setText(video.channel);
        ((TextView) findViewById(R.id.short_position)).setText((index + 1) + "/" + shorts.size());
        findViewById(R.id.short_loading).setVisibility(View.VISIBLE);
        thumbnail.setVisibility(View.VISIBLE);
        ImageLoader.loadImage(video.thumbnail, thumbnail, false);
        LocalLibrary.getInstance().addToHistory(video);

        new Thread(new Runnable() {
            @Override
            public void run() {
                String source = null;
                try {
                    source = resolveSource(video.id);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
            playCurrent();
        }
    }
}
