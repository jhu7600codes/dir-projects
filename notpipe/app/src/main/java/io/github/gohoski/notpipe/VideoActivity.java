package io.github.gohoski.notpipe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import io.github.gohoski.notpipe.api.ContentUnavailableException;
import io.github.gohoski.notpipe.api.Conversion;
import io.github.gohoski.notpipe.api.Manager;
import io.github.gohoski.notpipe.api.Metadata;
import io.github.gohoski.notpipe.api.VideoStream;
import io.github.gohoski.notpipe.api.Yt2009Account;
import io.github.gohoski.notpipe.config.Config;
import io.github.gohoski.notpipe.config.ConfigManager;
import io.github.gohoski.notpipe.data.Comment;
import io.github.gohoski.notpipe.data.Video;
import io.github.gohoski.notpipe.data.VideoInfo;
import io.github.gohoski.notpipe.http.HttpClient;
import io.github.gohoski.notpipe.ui.AdapterLinearLayout;
import io.github.gohoski.notpipe.ui.AspectRatioVideoView;
import io.github.gohoski.notpipe.ui.CommentAdapter;
import io.github.gohoski.notpipe.ui.PlayerControls;
import io.github.gohoski.notpipe.ui.VideoActions;
import io.github.gohoski.notpipe.ui.VideoAdapter;
import io.github.gohoski.notpipe.util.ChannelIconResolver;
import io.github.gohoski.notpipe.util.ImageLoader;
import io.github.gohoski.notpipe.util.LocalLibrary;
import io.github.gohoski.notpipe.util.PlayerExtras;
import io.github.gohoski.notpipe.util.SleepTimer;

public class VideoActivity extends Activity {
    String videoId;
    LinearLayout videoLayout;
    FrameLayout videoFrame;
    Context context;
    ImageView thumbnail, channelThumbnail, play;
    VideoView videoView;
    View relatedList, commentsList;
    ProgressBar relatedLoading, commentsLoading;
    ScrollView scrollView;
    View tabsScrollView;

    Video video;
    List<VideoInfo> relatedVideos = new ArrayList<VideoInfo>();
    List<Comment> comments = new ArrayList<Comment>();
    VideoAdapter relatedAdapter;
    CommentAdapter commentsAdapter;

    Metadata api;
    VideoStream videoStream;
    Config config;

    boolean relatedLoaded = false;
    boolean commentsLoaded = false;
    protected boolean isOpencore = NotPipe.SDK < 8; // OpenCORE—multimedia framework used on Android <2.2—has some bugs that need to be catched, hence this boolean

    private LoadVideoTask loadVideoTask;
    private ResolveStreamTask resolveStreamTask;
    private DownloadVideoTask downloadVideoTask;

    private static final int MAX_STREAM_RETRIES = 3;
    private int streamRetryCount = 0;
    private LoadCommentsTask loadCommentsTask;
    private LoadRelatedTask loadRelatedTask;

    // Retry Handler and Runnable to manage automated recovery on I/O errors (1, -1004)
    private Handler retryHandler = new Handler();
    private Runnable retryRunnable = null;

    private int videoPosition = 0;
    private boolean videoPlaying = false;
    private boolean videoPrepared = false;
    private String videoUrl = null;
    private boolean isUsingMetadataUrl = false;
    private boolean isTabletFullscreen = false;
    private boolean isFullscreenMode = false; // Unified field tracking active full screen state
    private boolean isVideoViewNeedsReload = true; // Tracks if VideoView lost its surface bounds
    private String resolvedQuality = null;
    private boolean isActivityStopped = false;

    private int videoBufferTimeout = 60000;
    private boolean watchStarted = false;
    private volatile String hqUrl;
    private VideoStream hqInstance;
    private boolean hqResolving = false;
    private boolean waitingForHq = false;
    private MediaPlayer mediaPlayer;
    private List<int[]> sponsorSegments = new ArrayList<int[]>();
    private List<PlayerExtras.Caption> captions;
    private TextView captionView;
    private PlayerControls controls;
    private boolean looping = false;
    private boolean enteredFullscreenForPip = false;
    private final Handler handler = new Handler();
    private final Runnable extrasTicker = new Runnable() {
        @Override
        public void run() {
            tickExtras();
            handler.postDelayed(this, 400);
        }
    };
    private int watchtimeTicks = 0;
    /** 400 ms ticks without progress before a stalled stream is reopened (6 s) */
    private static final int STALL_TICKS = 15;
    private int stallTicks = 0;
    private int stallPosition = -1;
    private int stallRecoveries = 0;
    private Yt2009Account.WatchSession watchSession;
    private static final String DIR_VIDEOS = "notPipe/videos";

    private Handler systemUiHandler = new Handler();
    private Runnable hideSystemUiRunnable = new Runnable() {
        @Override
        public void run() {
            if (isTabletFullscreen || isFullscreenMode) {
                hideSystemUI();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        api = Manager.getInstance().getMetadata();
        config = ConfigManager.getInstance().getConfig();
        context = this;

        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri == null) {
            videoId = intent.getStringExtra("ID");
        } else {
            videoId = uri.getHost().contains("youtube.com") ? uri.getQueryParameter("v") : uri.getLastPathSegment();
        }

        relatedAdapter = new VideoAdapter(this, R.layout.item_video, relatedVideos);
        relatedAdapter.setChannelIconListener(new VideoAdapter.ChannelIconListener() {
            @Override
            public String getResolvedIcon(String channelId) {
                return ChannelIconResolver.getResolved(channelId);
            }

            @Override
            public void onRequestFallbackIcon(String channelId) {
                ChannelIconResolver.requestFallback(channelId, new ChannelIconResolver.OnIconResolvedListener() {
                    @Override
                    public void onIconResolved(String channelId, String url) {
                        if (url != null && url.length() > 0 && relatedLoaded && relatedList instanceof ViewGroup) {
                            ViewGroup vg = (ViewGroup) relatedList;
                            for (int i = 0; i < vg.getChildCount(); i++) {
                                View child = vg.getChildAt(i);
                                if (child != null) {
                                    relatedAdapter.updateChannelIconForView(child, channelId);
                                }
                            }
                        }
                    }
                });
            }
        });

        commentsAdapter = new CommentAdapter(this, R.layout.item_comment, comments);

        setupViewReferences();
        setupAdapters();
        setupTabHost();
        // Hide the tabs panel initially so we don't start loading tabs before the main video loads
        View tabHost = findViewById(android.R.id.tabhost);
        if (tabHost != null) {
            tabHost.setVisibility(View.GONE);
        }

        // Apply orientation configuration
        if (isTablet()) {
            applyTabletLayout(getResources().getConfiguration().orientation);
        } else {
            View fsButton = findViewById(R.id.full_screen);
            if (fsButton != null) fsButton.setVisibility(View.GONE);
        }

        videoView.setVisibility(View.GONE);
        applyOpenCoreLayoutFix();
        setupClickListeners();
        setupScrollHandler();
        setupPlayerExtras();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && !isTablet()) {
            enterFullscreenMode();
        }

        if (VideoCache.hasValidMetadata(videoId)) {
            api = VideoCache.getMetadataInstance();
            onVideoDataLoaded(VideoCache.getVideo());
        } else {
            loadVideoTask = new LoadVideoTask();
            loadVideoTask.execute(videoId);
        }
    }

    private void handleVideoClick(int position) {
        ImageLoader.clearCache();
        System.gc();
        Intent intent = new Intent(context, VideoActivity.class);
        intent.putExtra("ID", relatedVideos.get(position).id);
        context.startActivity(intent);
    }

    private int lastScrollY = -1;
    private int lastTabsScrollY = -1;
    private int lastListHeight = -1;
    private Handler scrollHandler = new Handler();
    private Runnable scrollCheckRunnable = new Runnable() {
        @Override
        public void run() {
            boolean shouldCheck = false;
            if (scrollView != null) {
                int currentScrollY = scrollView.getScrollY();
                if (currentScrollY != lastScrollY || lastScrollY == -1) {
                    lastScrollY = currentScrollY;
                    shouldCheck = true;
                }
            }
            if (tabsScrollView != null) {
                int currentTabsScrollY = tabsScrollView.getScrollY();
                if (currentTabsScrollY != lastTabsScrollY || lastTabsScrollY == -1) {
                    lastTabsScrollY = currentTabsScrollY;
                    shouldCheck = true;
                }
            }
            int currentListHeight = 0;
            TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
            if (tabHost != null) {
                String currentTab = tabHost.getCurrentTabTag();
                View activeView = null;
                if ("related".equals(currentTab) && relatedLoaded) {
                    activeView = relatedList;
                } else if ("comments".equals(currentTab) && commentsLoaded) {
                    activeView = commentsList;
                }
                if (activeView != null) currentListHeight = activeView.getHeight();
            }
            if (currentListHeight != lastListHeight || lastListHeight == -1) {
                lastListHeight = currentListHeight;
                shouldCheck = true;
            }
            if (shouldCheck) checkVisibleItems();
            scrollHandler.postDelayed(this, 100);
        }
    };

    /**
     * Identifies exactly which views are on the screen and loads their images.
     */
    private void checkVisibleItems() {
        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        if (tabHost == null) return;

        String currentTab = tabHost.getCurrentTabTag();
        View activeView = null;

        if ("related".equals(currentTab) && relatedLoaded) {
            activeView = relatedList;
        } else if ("comments".equals(currentTab) && commentsLoaded) {
            activeView = commentsList;
        }

        if (activeView instanceof AdapterLinearLayout) {
            AdapterLinearLayout activeList = (AdapterLinearLayout) activeView;
            if (activeList.getChildCount() == 0) return;

            // Find the outermost ScrollView ancestor of the active list.
            // In portrait, this will traverse past the inner tabs_scroll_view and select scroll_view.
            // In landscape, scroll_view is in a sibling branch, so it selects tabs_scroll_view.
            ScrollView activeScrollView = null;
            ViewParent parent = activeList.getParent();
            while (parent != null) {
                if (parent instanceof ScrollView) {
                    activeScrollView = (ScrollView) parent;
                }
                parent = parent.getParent();
            }

            if (activeScrollView == null || activeScrollView.getHeight() == 0) return;

            int visibleTop = activeScrollView.getScrollY();
            int visibleBottom = visibleTop + activeScrollView.getHeight();

            for (int i = 0; i < activeList.getChildCount(); i++) {
                View child = activeList.getChildAt(i);
                // Prevent zero-height unlaid-out children from prematurely evaluating as visible
                if (child == null || child.getHeight() == 0) continue;

                // Calculate the child's top position relative to the selected activeScrollView
                int childTop = 0;
                View curr = child;
                while (curr != null && curr != activeScrollView) {
                    childTop += curr.getTop();
                    ViewParent currParent = curr.getParent();
                    if (currParent instanceof View) {
                        curr = (View) currParent;
                    } else {
                        curr = null;
                    }
                }

                int childBottom = childTop + child.getHeight();

                if (childBottom > visibleTop && childTop < visibleBottom) {
                    if ("related".equals(currentTab)) {
                        relatedAdapter.loadImagesForView(child);
                    } else {
                        commentsAdapter.loadImagesForView(child);
                    }
                }
            }
        }
    }

    private static final int POSITION_TRACK_INTERVAL = 5000;
    private Handler positionTrackHandler = new Handler();
    private Runnable positionTrackRunnable = new Runnable() {
        @Override
        public void run() {
            if (videoView != null && videoPrepared) {
                try {
                    if (videoView.isPlaying()) {
                        int pos = videoView.getCurrentPosition();
                        if (pos > 0) {
                            videoPosition = pos;
                        }
                        // YouTube's own players send watchtime pings regularly, ~every 30 s here
                        if (++watchtimeTicks % 6 == 0) reportYoutubeWatchtime(false);
                    }
                } catch (Exception ignored) {}
            }
            positionTrackHandler.postDelayed(this, POSITION_TRACK_INTERVAL);
        }
    };

    private void startPositionTracking() {
        positionTrackHandler.removeCallbacks(positionTrackRunnable);
        positionTrackHandler.postDelayed(positionTrackRunnable, POSITION_TRACK_INTERVAL);
    }

    private void stopPositionTracking() {
        positionTrackHandler.removeCallbacks(positionTrackRunnable);
    }

    /**
     * Dynamically determines the video quality to request and sets the corresponding timeout.
     */
    private int getTimeoutForQuality(String quality) {
        if (video == null || "360".equals(quality)) {
            return 60000;
        } else {
            if (video.length < 1800) { // Shorter than 30 minutes
                return 180000;
            } else { // 30 minutes to 60 minutes
                return 360000;
            }
        }
    }
    private String determineQuality() {
        String quality = config.getPreferredQuality();
        if (quality == null) quality = "360";

        if (video == null) {
            videoBufferTimeout = 60000;
            return quality;
        }

        if (!"360".equals(quality)) {
            if (video.length > 3600) {
                quality = "360";
                Toast.makeText(context, R.string.long_360, Toast.LENGTH_LONG).show();
            } else if ("1080".equals(quality)) {
                Toast.makeText(context, R.string.experimental_1080, Toast.LENGTH_LONG).show();
            }
        }

        videoBufferTimeout = getTimeoutForQuality(quality);
        return quality;
    }

    /**
     * Updates and displays the progress/loading label right as playback is initiated.
     */
    private void updateProgressMessage(String quality) {
        TextView progressView = (TextView) findViewById(R.id.video_progress);
        if (progressView != null) {
            if (quality == null) quality = "360";
            if (config.isConvertVideos()) {
                progressView.setText(R.string.conv_long);
                progressView.setVisibility(View.VISIBLE);
            } else if (!"360".equals(quality)) {
                progressView.setText(getString(R.string.hq_long, quality));
                progressView.setVisibility(View.VISIBLE);
            } else {
                progressView.setVisibility(View.GONE);
            }
        }
    }

    private void resetVideo() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        cancelVideoTimeout();
        stopPositionTracking();
        if (videoView != null) {
            videoView.stopPlayback();
            videoView.setVideoURI(null);
            videoView.setMediaController(null);
        }
        if (controls != null) controls.setEnabled(false);
        videoPlaying = false;
        videoPrepared = false;
        videoUrl = null;
    }

    private void restoreVideoUI() {
        LinearLayout loading = (LinearLayout) findViewById(R.id.video_loading);
        if (loading != null) loading.setVisibility(View.GONE);
        if (play != null) play.setVisibility(View.VISIBLE);
        if (thumbnail != null) thumbnail.setVisibility(View.VISIBLE);
        TextView progressView = (TextView) findViewById(R.id.video_progress);
        if (progressView != null) progressView.setVisibility(View.GONE);
    }

    private void showVideoLoadingUI() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        View loading = findViewById(R.id.video_loading);
        if (loading != null) loading.setVisibility(View.VISIBLE);
        if (play != null) play.setVisibility(View.GONE);
        if (thumbnail != null) thumbnail.setVisibility(View.INVISIBLE); // Keep it INVISIBLE to maintain bounding box size
    }

    private VideoStream selectVideoStream(String quality) {
        List<VideoStream> targetList = "360".equals(quality)
                ? Manager.getInstance().getVideoInstances()
                : Manager.getInstance().getHqInstances();

        if (config.isConvertVideos()) {
            List<Conversion> conversions = Manager.getInstance().getConversion();
            List<VideoStream> convInstances = new ArrayList<VideoStream>();
            for (int i = 0; i < targetList.size(); i++) {
                VideoStream vs = targetList.get(i);
                if (vs instanceof Conversion && conversions.contains(vs)) {
                    convInstances.add(vs);
                }
            }
            if (!convInstances.isEmpty()) {
                targetList = convInstances;
            }
        }

        if (!targetList.isEmpty()) {
            return targetList.get(new Random().nextInt(targetList.size()));
        }
        return null;
    }

    private void playVideo() {
        if (video == null) return;
        streamRetryCount = 0;
        showVideoLoadingUI();
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                final String runQuality = resolvedQuality != null ? resolvedQuality : "360";
                updateProgressMessage(runQuality);
                if (canPrefetchHq() && !VideoCache.hasValidStream(videoId, runQuality)) {
                    if (hqUrl != null) {
                        videoStream = hqInstance;
                        isUsingMetadataUrl = false;
                        videoUrl = hqUrl;
                        proceedPlay(hqUrl);
                        return;
                    }
                    if (hqResolving) {
                        // The HD request started when the page opened; wait for it instead of starting a second one
                        waitingForHq = true;
                        return;
                    }
                }
                if (VideoCache.hasValidStream(videoId, runQuality)) {
                    videoUrl = VideoCache.getStreamUrl();
                    videoStream = VideoCache.getVideoStreamInstance();
                    proceedPlay(videoUrl);
                    return;
                }
                if (config.isConvertVideos() || !"360".equals(runQuality) || video.videoUrl == null || video.videoUrl.length() == 0) {
                    isUsingMetadataUrl = false;
                    if (videoStream == null) videoStream = selectVideoStream(runQuality);
                    resolveStreamTask = new ResolveStreamTask(videoStream, runQuality);
                    resolveStreamTask.execute(videoId, runQuality);
                } else {
                    isUsingMetadataUrl = true;
                    videoUrl = video.videoUrl;
                    VideoCache.putStream(videoId, videoUrl, "360", null);
                    proceedPlay(videoUrl);
                }
            }
        });
    }

    private boolean hasSoftwareKeys() {
        if (NotPipe.SDK < 14) return false;
        try {
            // If the device has physical Menu and Back buttons, it does not use an on-screen nav bar.
            // This prevents the black bar glitch on devices like Samsung tablets.
            android.view.ViewConfiguration viewConfig = android.view.ViewConfiguration.get(this);
            boolean hasMenuKey = ((Boolean) viewConfig.getClass().getMethod("hasPermanentMenuKey").invoke(viewConfig)).booleanValue();
            boolean hasBackKey = android.view.KeyCharacterMap.deviceHasKey(android.view.KeyEvent.KEYCODE_BACK);
            return !hasMenuKey && !hasBackKey;
        } catch (Exception e) {
            return true;
        }
    }

    private void hideSystemUI() {
        try {
            View decorView = getWindow().getDecorView();
            if (NotPipe.SDK >= 14) {
                int flags = 1 | 4; // SYSTEM_UI_FLAG_LOW_PROFILE | SYSTEM_UI_FLAG_FULLSCREEN
                if (hasSoftwareKeys()) {
                    flags |= 2; // SYSTEM_UI_FLAG_HIDE_NAVIGATION
                }
                if (NotPipe.SDK >= 19) {
                    flags |= 4096; // SYSTEM_UI_FLAG_IMMERSIVE_STICKY (Better than standard IMMERSIVE)
                }
                decorView.getClass().getMethod("setSystemUiVisibility", int.class).invoke(decorView, flags);
            } else if (NotPipe.SDK >= 11) {
                decorView.getClass().getMethod("setSystemUiVisibility", int.class).invoke(decorView, 1);
            }
        } catch (Exception ignored) {}
    }

    private void showSystemUI() {
        try {
            View decorView = getWindow().getDecorView();
            if (NotPipe.SDK >= 11) {
                decorView.getClass().getMethod("setSystemUiVisibility", int.class).invoke(decorView, 0);
            }
        } catch (Exception ignored) {}
    }

    private void hideDummyTab() {
        final TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        if (tabHost != null && tabHost.getTabWidget() != null && tabHost.getTabWidget().getChildCount() > 0) {
            tabHost.getTabWidget().post(new Runnable() {
                @Override
                public void run() {
                    View dummyTab = tabHost.getTabWidget().getChildAt(0);
                    dummyTab.setVisibility(View.GONE);
                    ViewGroup.LayoutParams params = dummyTab.getLayoutParams();
                    if (params != null) {
                        params.width = 0;
                        params.height = 0;
                        dummyTab.setLayoutParams(params);
                    }
                }
            });
        }
    }

    private void applyOpenCoreLayoutFix() {
        if (isOpencore && videoView != null) {
            videoView.setVisibility(View.VISIBLE);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.FILL_PARENT
            );
            params.gravity = android.view.Gravity.CENTER;
            videoView.setLayoutParams(params);
            videoView.requestLayout();
        }
    }

    private File getCachedVideoFile(String vId) {
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard, DIR_VIDEOS);
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, vId + ".mp4");
    }

    private void updatePlaybackViaText(String host) {
        TextView playbackInfo = (TextView) findViewById(R.id.playback_info);
        if (playbackInfo != null) playbackInfo.setText(getString(R.string.playback_via, host));
    }

    private void restoreVideoMetadata() {
        if (video == null) return;

        ((TextView) findViewById(R.id.title)).setText(video.title);
        ((TextView) findViewById(R.id.channel_title)).setText(video.channel);
        ((TextView) findViewById(R.id.subscribers)).setText(Utils.formatNumber(context, video.subscribers));
        if (video.likes > 0)
            ((Button) findViewById(R.id.like)).setText(Utils.formatNumber(context, video.likes));
        ((TextView) findViewById(R.id.views)).setText(getString(R.string.views, Utils.formatNumber(context, video.views)) +
                "   " + Utils.formatTimeAgo(context, video.publishedAt));

        ImageLoader.loadImage(video.thumbnail, thumbnail, false);
        ImageLoader.loadImage(video.channelThumbnail, channelThumbnail, false);

        if (videoStream != null) {
            updatePlaybackViaText(videoStream.getHost());
        } else if (isUsingMetadataUrl) {
            updatePlaybackViaText(api.getHost());
        }
    }

    private void attachVideoListeners() {
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(final MediaPlayer mp) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cancelVideoTimeout();
                        streamRetryCount = 0;
                        restoreVideoUI();
                        thumbnail.setVisibility(View.INVISIBLE);
                        play.setVisibility(View.GONE);

                        videoPrepared = true;
                        mediaPlayer = mp;
                        if (videoPosition <= 0) {
                            videoPosition = PlayerExtras.getResumePosition(videoId);
                            if (videoPosition > 0) Toast.makeText(context, R.string.resumed, Toast.LENGTH_SHORT).show();
                        }
                        if (videoPosition > 0) {
                            videoView.seekTo(videoPosition);
                        }
                        if (PlayerExtras.getSpeed() != 1f) applySpeed(PlayerExtras.getSpeed());
                        startExtrasTicker();

                        AspectRatioVideoView arvv = (AspectRatioVideoView) videoView;
                        arvv.setVideoDimensions(mp.getVideoWidth(), mp.getVideoHeight());

                        if (controls != null) {
                            controls.setEnabled(true);
                            controls.setSpeed(PlayerExtras.getSpeed());
                            if (resolvedQuality != null) controls.setQuality(resolvedQuality);
                        }
                        if (looping) mp.setLooping(true);

                        if (isOpencore) {
                            videoView.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    ((AspectRatioVideoView) videoView).forceLayoutUpdate();
                                    videoView.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (videoPlaying) {
                                                mp.start(); startPositionTracking();
                                            }
                                        }
                                    }, 200);
                                }
                            }, 100);
                        } else {
                            if (videoPlaying) {
                                mp.start(); startPositionTracking();
                            }
                        }
                        startYoutubeWatch();
                    }
                });
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                cancelVideoTimeout();
                videoPlaying = false;
                videoPosition = 0;
                PlayerExtras.saveResumePosition(videoId, 0, 0);
                reportYoutubeWatchtime(true);
                playNextIfAutoplay();
            }
        });

        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, final int what, final int extra) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cancelVideoTimeout();
                        if (what == 43 || what == -11) return; // Ignore certain framework bugs

                        // Error code indicates unsupported format
                        if (what == 1 && extra == -2147483648) {
                            resetVideo(); restoreVideoUI();
                            Toast.makeText(context, R.string.unsupported_format, Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Determine if we were attempting to play a local cached file
                        boolean isLocalFile = false;
                        if (videoUrl != null && (videoUrl.startsWith(Environment.getExternalStorageDirectory().getPath()) || videoUrl.startsWith("file://"))) {
                            isLocalFile = true;
                        }

                        if (isLocalFile) {
                            // Local playback failed (often due to corrupted downloads or aborted conversions)
                            try {
                                File cachedFile = getCachedVideoFile(videoId);
                                if (cachedFile.exists()) {
                                    cachedFile.delete();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            VideoCache.clearAll(); // Invalidate stream cache so the corrupted path is forgotten
                            resetVideo();

                            // Force automated recovery by requesting a fresh stream and triggering download/conversion
                            showVideoLoadingUI();
                            resolveStreamTask = new ResolveStreamTask(null);
                            resolveStreamTask.execute(videoId);
                            return;
                        }

                        // Intercept network stream buffer drops (MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_IO)
                        if (what == 1 && extra == -1004) {
                            final String savedUrl = videoUrl;
                            if (videoView != null) {
                                try {
                                    int pos = videoView.getCurrentPosition();
                                    if (pos > 0) {
                                        videoPosition = pos;
                                    }
                                } catch (Exception e) { e.printStackTrace(); }
                            }
                            Log.d("VideoActivity", "pos:"+videoPosition);
                            resetVideo();
                            videoUrl = savedUrl; // Retain the url to avoid losing it during resetVideo()

                            showVideoLoadingUI();

                            if (retryRunnable != null) {
                                retryHandler.removeCallbacks(retryRunnable);
                            }

                            retryRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    if (isActivityStopped) {
                                        isVideoViewNeedsReload = true;
                                        return;
                                    }
                                    if (videoUrl != null) {
                                        applyOpenCoreLayoutFix();
                                        videoView.setVisibility(View.VISIBLE);
                                        attachVideoListeners();
                                        videoPlaying = true;
                                        loadVideoUri(videoUrl);
                                    } else {
                                        resolveStreamTask = new ResolveStreamTask(null);
                                        resolveStreamTask.execute(videoId);
                                    }
                                }
                            };
                            retryHandler.postDelayed(retryRunnable, 2999);
                            return;
                        }

                        resetVideo();
                        boolean isOffline = !Utils.hasConnection(context);

                        // If it's a local file error, we do not declare the server instance dead
                        if (!isLocalFile) {
                            Object instanceToRemove = isUsingMetadataUrl ? api : videoStream;
                            if (instanceToRemove != null && !isOffline) {
                                Manager.getInstance().removeDeadInstance(instanceToRemove);
                            }
                        }
                        isUsingMetadataUrl = false;

                        if (isOffline || streamRetryCount < MAX_STREAM_RETRIES) {
                            if (!isOffline) streamRetryCount++;
                            showVideoLoadingUI();
                            resolveStreamTask = new ResolveStreamTask(null);
                            resolveStreamTask.execute(videoId);
                        } else {
                            restoreVideoUI();
                            Toast.makeText(context, "Stream failed after multiple attempts.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                return true;
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("videoId", videoId);

        // Save the exact position and actual playing state before destroying layout bounds
        if (videoView != null && videoPrepared) {
            try {
                int pos = videoView.getCurrentPosition();
                if (pos > 0) {
                    videoPosition = pos;
                }
                if (videoView.isPlaying()) {
                    videoPlaying = true;
                }
            } catch (Exception ignored) {}
        }

        outState.putInt("videoPosition", videoPosition);
        outState.putBoolean("videoPlaying", videoPlaying);
        outState.putBoolean("videoPrepared", videoPrepared);
        outState.putBoolean("isUsingMetadataUrl", isUsingMetadataUrl);
        outState.putBoolean("isTabletFullscreen", isTabletFullscreen);
        outState.putBoolean("isFullscreenMode", isFullscreenMode);
        if (video != null) {
            outState.putString("videoUrl", videoUrl);
            outState.putString("title", video.title);
            outState.putString("channel", video.channel);
            outState.putString("channelThumbnail", video.channelThumbnail);
            outState.putString("thumbnail", video.thumbnail);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        videoId = savedInstanceState.getString("videoId");
        videoPosition = savedInstanceState.getInt("videoPosition");
        videoPlaying = savedInstanceState.getBoolean("videoPlaying");
        videoPrepared = savedInstanceState.getBoolean("videoPrepared");
        isUsingMetadataUrl = savedInstanceState.getBoolean("isUsingMetadataUrl", false);
        videoUrl = savedInstanceState.getString("videoUrl");
        isTabletFullscreen = savedInstanceState.getBoolean("isTabletFullscreen", false);
        isFullscreenMode = savedInstanceState.getBoolean("isFullscreenMode", false);

        if (isFullscreenMode) {
            enterFullscreenMode();
        } else if (isTabletFullscreen && isTablet()) {
            enterFullscreenMode();
        }
    }

    private void leaveFullscreen() {
        if (isTablet()) {
            exitFullscreenMode();
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            exitFullscreenMode();
        }
    }

    /* ---------- Picture-in-picture (Android 8.0+; reflection since the SDK level is older) ---------- */

    private boolean isInPip() {
        if (NotPipe.SDK < 24) return false;
        try {
            return (Boolean) Activity.class.getMethod("isInPictureInPictureMode").invoke(this);
        } catch (Exception e) {
            return false;
        }
    }

    private void enterPip() {
        if (NotPipe.SDK < 26) {
            Toast.makeText(context, R.string.pip_unsupported, Toast.LENGTH_LONG).show();
            return;
        }
        if (!videoPrepared) return;
        if (controls != null) controls.hide();
        if (!isFullscreenMode) {
            enteredFullscreenForPip = true;
            enterFullscreenMode();
        }
        try {
            Activity.class.getMethod("enterPictureInPictureMode").invoke(this);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.pip_unsupported, Toast.LENGTH_LONG).show();
        }
    }

    /** Leaving the app while a video plays drops it into a PiP window, like YouTube */
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        try {
            if (NotPipe.SDK >= 26 && videoPrepared && videoView.isPlaying() && PlayerExtras.isEnabled(PlayerExtras.KEY_AUTO_PIP)) {
                enterPip();
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        if (!isInPictureInPictureMode && enteredFullscreenForPip) {
            enteredFullscreenForPip = false;
            exitFullscreenMode();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (isFullscreenMode) {
                leaveFullscreen();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        systemUiHandler.removeCallbacks(hideSystemUiRunnable);
        scrollHandler.removeCallbacks(scrollCheckRunnable);
        if (retryRunnable != null) {
            retryHandler.removeCallbacks(retryRunnable);
        }
        cancelVideoTimeout();
        stopPositionTracking();
        handler.removeCallbacks(extrasTicker);
        if (controls != null) controls.release();
        ImageLoader.clearCache();

        // Cancel all running AsyncTasks to stop background operations
        if (loadVideoTask != null) loadVideoTask.cancel(true);
        if (resolveStreamTask != null) resolveStreamTask.cancel(true);
        if (downloadVideoTask != null) downloadVideoTask.cancel(true);
        if (loadCommentsTask != null) loadCommentsTask.cancel(true);
        if (loadRelatedTask != null) loadRelatedTask.cancel(true);
    }

    /**
     * When signed in through yt2009, reports the playback to YouTube so it goes into the account's
     * watch history and YouTube's recommendations learn from it. Runs once per video.
     */
    private void startYoutubeWatch() {
        if (watchStarted || !Yt2009Account.getInstance().isSignedIn()) return;
        watchStarted = true;
        final String id = videoId;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    watchSession = Yt2009Account.getInstance().startWatch(id);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void reportYoutubeWatchtime(final boolean finalReport) {
        final Yt2009Account.WatchSession session = watchSession;
        if (session == null || videoView == null || !videoPrepared) return;
        final float seconds;
        final boolean playing;
        try {
            seconds = videoView.getCurrentPosition() / 1000f;
            playing = videoView.isPlaying();
        } catch (Exception e) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Yt2009Account.getInstance().reportWatchtime(session, seconds, playing, finalReport);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected void onPause() {
        if (isInPip()) {
            // Keep playing in the PiP window
            super.onPause();
            return;
        }
        reportYoutubeWatchtime(isFinishing());
        handler.removeCallbacks(extrasTicker);
        if (videoView != null && videoPrepared) {
            try {
                PlayerExtras.saveResumePosition(videoId, videoView.getCurrentPosition(), videoView.getDuration());
            } catch (Exception ignored) {}
        }
        super.onPause();
        stopPositionTracking();
        if (videoView != null) {
            try {
                if (videoPrepared) {
                    int pos = videoView.getCurrentPosition();
                    if (pos > 0) {
                        videoPosition = pos;
                    }
                    if (videoView.isPlaying()) {
                        videoPlaying = true;
                        videoView.pause();
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        isActivityStopped = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActivityStopped = true;
        cancelVideoTimeout(); // Cancel active buffer timeouts so they don't fire in the background

        if (videoView != null && videoUrl != null) {
            videoView.stopPlayback();
            videoView.setVisibility(View.GONE);
            isVideoViewNeedsReload = true;
            videoPrepared = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoPrepared) startExtrasTicker();
        if (videoUrl != null && !config.isUseExternalPlayer()) {
            if (config.isStreamPlayback() || videoUrl.startsWith(Environment.getExternalStorageDirectory().getPath()) || videoUrl.startsWith("file://")) {
                if (isVideoViewNeedsReload) {
                    showVideoLoadingUI();

                    updateProgressMessage(resolvedQuality);
                    applyOpenCoreLayoutFix();
                    videoView.setVisibility(View.VISIBLE);
                    attachVideoListeners();
                    loadVideoUri(videoUrl);
                    isVideoViewNeedsReload = false;
                }
            }
        }
        if (relatedLoaded) {
            android.view.ViewGroup relatedViewGroup = (android.view.ViewGroup) relatedList;
            for (int i = 0; i < relatedViewGroup.getChildCount(); i++) {
                relatedAdapter.resetImageView(relatedViewGroup.getChildAt(i));
            }
            checkVisibleItems();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ImageLoader.clearCache();
        VideoCache.clearAll();
    }

    private boolean isTablet() {
        return getResources().getBoolean(R.bool.is_tablet);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Only release sensor locks once back in normal portrait orientation
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && !isFullscreenMode) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }

        if (isTablet()) {
            applyTabletLayout(newConfig.orientation);

            // Tablets stay in fullscreen on rotation if manually toggled
            if (isFullscreenMode) {
                enterFullscreenMode();
            } else {
                if (videoView != null) {
                    videoView.requestLayout();
                    videoView.invalidate();
                    ((AspectRatioVideoView) videoView).forceLayoutUpdate();
                }
            }
        } else {
            // For phones:
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                enterFullscreenMode();
            } else {
                // If exiting landscape fullscreen, or portrait fullscreen is active
                if (isFullscreenMode && !config.isFullscreenRotateLandscape()) {
                    enterFullscreenMode();
                } else {
                    exitFullscreenMode();
                }
            }
        }
    }

    private void applyTabletLayout(int orientation) {
        LinearLayout mainContentLayout = (LinearLayout) findViewById(R.id.main_content_layout);
        View videoLayout = findViewById(R.id.video_layout);
        ViewGroup portraitTabContainer = (ViewGroup) findViewById(R.id.portrait_tab_container);
        ViewGroup landscapeTabContainer = (ViewGroup) findViewById(R.id.landscape_tab_container);
        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        View tabsScrollView = findViewById(R.id.tabs_scroll_view);
        View fsButton = findViewById(R.id.full_screen);

        if (mainContentLayout == null || videoLayout == null || portraitTabContainer == null
                || landscapeTabContainer == null || tabHost == null) {
            return;
        }

        if (fsButton != null) {
            // Only visible in tablet landscape layout, and only if NOT currently in fullscreen
            if (orientation == Configuration.ORIENTATION_LANDSCAPE && !isFullscreenMode) {
                fsButton.setVisibility(View.VISIBLE);
            } else {
                fsButton.setVisibility(View.GONE);
            }
        }

        // Detach TabHost from its current parent
        ViewGroup currentParent = (ViewGroup) tabHost.getParent();
        if (currentParent != null) {
            currentParent.removeView(tabHost);
        }

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mainContentLayout.setOrientation(LinearLayout.HORIZONTAL);

            LinearLayout.LayoutParams videoParams = new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.FILL_PARENT,
                    6.0f
            );
            videoLayout.setLayoutParams(videoParams);

            LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.FILL_PARENT,
                    3.5f
            );
            landscapeTabContainer.setLayoutParams(tabParams);

            landscapeTabContainer.setVisibility(isFullscreenMode ? View.GONE : View.VISIBLE);
            portraitTabContainer.setVisibility(View.GONE);

            landscapeTabContainer.addView(tabHost, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.FILL_PARENT
            ));

            if (tabsScrollView != null) {
                tabsScrollView.getLayoutParams().height = ViewGroup.LayoutParams.FILL_PARENT;
                tabsScrollView.requestLayout();
            }
        } else {
            mainContentLayout.setOrientation(LinearLayout.VERTICAL);

            LinearLayout.LayoutParams videoParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.FILL_PARENT,
                    1.0f
            );
            videoLayout.setLayoutParams(videoParams);

            landscapeTabContainer.setVisibility(View.GONE);
            portraitTabContainer.setVisibility(isFullscreenMode ? View.GONE : View.VISIBLE);

            portraitTabContainer.addView(tabHost, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            if (tabsScrollView != null) {
                tabsScrollView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                tabsScrollView.requestLayout();
            }
        }

        hideDummyTab();
        triggerLayoutFix(tabHost);
    }

    private void toggleActionBar(boolean show) {
        if (NotPipe.SDK >= 11) {
            try {
                Object actionBar = Activity.class.getMethod("getActionBar").invoke(this);
                if (actionBar != null) {
                    actionBar.getClass().getMethod(show ? "show" : "hide").invoke(actionBar);
                }
            } catch (Exception ignored) {}
        }
        try {
            View titleView = getWindow().findViewById(android.R.id.title);
            if (titleView != null) {
                titleView.setVisibility(show ? View.VISIBLE : View.GONE);
                if (titleView.getParent() instanceof View) {
                    ((View) titleView.getParent()).setVisibility(show ? View.VISIBLE : View.GONE);
                }
            }
        } catch (Exception ignored) {}
    }

    private void enterFullscreenMode() {
        isFullscreenMode = true;
        isTabletFullscreen = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        toggleActionBar(false);

        hideSystemUI();
        systemUiHandler.removeCallbacks(hideSystemUiRunnable);
        systemUiHandler.postDelayed(hideSystemUiRunnable, 5000);

        if (scrollView != null) scrollView.setVisibility(View.GONE);
        if (videoFrame != null) videoFrame.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT, 1.0f));

        View tabHost = findViewById(android.R.id.tabhost);
        if (tabHost != null && tabHost.getParent() instanceof View) {
            ((View) tabHost.getParent()).setVisibility(View.GONE);
        }

        FrameLayout.LayoutParams videoParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
        videoParams.gravity = android.view.Gravity.CENTER;
        videoView.setLayoutParams(videoParams);
        ((AspectRatioVideoView) videoView).setFullscreen(true);

        // Fix for the Action Bar Gap & SurfaceView bug.
        // Wait ~300ms for the Action Bar hide animation to finish, then force the layout to snap to the new bounds.
        if (NotPipe.SDK >= 11) {
            videoView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (videoLayout != null) {
                        videoLayout.requestLayout();
                        videoLayout.invalidate();
                    }
                    if (videoFrame != null) {
                        videoFrame.requestLayout();
                        videoFrame.invalidate();
                    }
                    videoView.requestLayout();
                    videoView.invalidate();
                    ((AspectRatioVideoView) videoView).forceLayoutUpdate();
                }
            }, 300);
        }
    }

    private void exitFullscreenMode() {
        isFullscreenMode = false;
        isTabletFullscreen = false;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        toggleActionBar(true);
        showSystemUI();
        systemUiHandler.removeCallbacks(hideSystemUiRunnable);
        if (scrollView != null) scrollView.setVisibility(View.VISIBLE);
        if (videoFrame != null) videoFrame.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        View tabHost = findViewById(android.R.id.tabhost);
        if (tabHost != null && tabHost.getParent() instanceof View) {
            ((View) tabHost.getParent()).setVisibility(View.VISIBLE);
        }
        if (isTablet()) {
            applyTabletLayout(getResources().getConfiguration().orientation);
        }
        FrameLayout.LayoutParams videoParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
        videoParams.gravity = android.view.Gravity.CENTER;
        videoView.setLayoutParams(videoParams);
        Runnable updateLayout = new Runnable() {
            @Override
            public void run() {
                if (videoLayout != null) {
                    videoLayout.requestLayout();
                    videoLayout.invalidate();
                }
                videoView.requestLayout();
                videoView.invalidate();
                ((AspectRatioVideoView) videoView).forceLayoutUpdate();
            }
        };
        if (isOpencore) {
            videoView.post(updateLayout);
        } else {
            // Force re-layout after Action Bar expands so bounds snap properly
            videoView.postDelayed(updateLayout, 300);
        }
        ((AspectRatioVideoView) videoView).setFullscreen(false);
        videoView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isActivityStopped) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        }, 350);
    }

    private void setupViewReferences() {
        videoView = (VideoView) findViewById(R.id.video);
        videoLayout = (LinearLayout) findViewById(R.id.video_layout);
        videoFrame = (FrameLayout) findViewById(R.id.video_frame);
        thumbnail = (ImageView) findViewById(R.id.thumbnail);
        channelThumbnail = (ImageView) findViewById(R.id.channel_thumbnail);
        play = (ImageView) findViewById(R.id.play);
        relatedList = findViewById(R.id.related_list);
        commentsList = findViewById(R.id.comments_list);
        addCommentComposer();
        relatedLoading = (ProgressBar) findViewById(R.id.related_loading);
        commentsLoading = (ProgressBar) findViewById(R.id.comments_loading);
        scrollView = (ScrollView) findViewById(R.id.scroll_view);
        tabsScrollView = findViewById(R.id.tabs_scroll_view);
    }

    /** "Add a comment" box at the top of the Comments tab, like on Shorts */
    private void addCommentComposer() {
        if (commentsList == null || !(commentsList.getParent() instanceof ViewGroup)) return;
        ViewGroup parent = (ViewGroup) commentsList.getParent();
        View composer = io.github.gohoski.notpipe.ui.CommentsDialog.composer(this, videoId,
                new io.github.gohoski.notpipe.ui.CommentsDialog.OnPosted() {
                    public void onPosted(Comment comment) {
                        comments.add(0, comment);
                        if (commentsAdapter != null) commentsAdapter.notifyDataSetChanged();
                        View noComments = findViewById(R.id.no_comments);
                        if (noComments != null) noComments.setVisibility(View.GONE);
                        triggerLayoutFix(commentsList);
                    }
                });
        float density = getResources().getDisplayMetrics().density;
        composer.setPadding((int) (15 * density), (int) (8 * density), (int) (8 * density), (int) (4 * density));
        parent.addView(composer, parent.indexOfChild(commentsList));
    }

    private void setupAdapters() {
        if (relatedList instanceof android.widget.ListView) {
            android.widget.ListView lv = (android.widget.ListView) relatedList;
            lv.setAdapter(relatedAdapter);
            lv.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    handleVideoClick(position);
                }
            });
        } else if (relatedList instanceof AdapterLinearLayout) {
            AdapterLinearLayout all = (AdapterLinearLayout) relatedList;
            all.setAdapter(relatedAdapter);
            all.setOnItemClickListener(new AdapterLinearLayout.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterLinearLayout parent, View view, int position, long id) {
                    handleVideoClick(position);
                }
            });
        }

        if (commentsList instanceof android.widget.ListView) {
            ((android.widget.ListView) commentsList).setAdapter(commentsAdapter);
            ((android.widget.ListView) commentsList).setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    showReplies(comments.get(position));
                }
            });
        } else if (commentsList instanceof AdapterLinearLayout) {
            ((AdapterLinearLayout) commentsList).setAdapter(commentsAdapter);
            ((AdapterLinearLayout) commentsList).setOnItemClickListener(new AdapterLinearLayout.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterLinearLayout parent, View view, int position, long id) {
                    showReplies(comments.get(position));
                }
            });
        }
    }

    /** Opens a comment's replies (Invidious only, other APIs don't give a reply token) */
    private void showReplies(final Comment comment) {
        io.github.gohoski.notpipe.ui.CommentsDialog.showReplies(this, videoId, comment);
    }

    private void setupClickListeners() {
        findViewById(R.id.share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    startActivity(Intent.createChooser(
                            new Intent(android.content.Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(android.content.Intent.EXTRA_TEXT, "https://youtu.be/" + videoId)
                                    .putExtra(android.content.Intent.EXTRA_SUBJECT, video != null ? video.title : ""),
                            getString(R.string.share)));
                } catch (android.content.ActivityNotFoundException ignored) {}
            }
        });

        TextView playbackInfo = (TextView) findViewById(R.id.playback_info);
        playbackInfo.setText(getString(R.string.playback_via, getString(R.string.loading_)));
        if (PlayerExtras.isBasicMode()) playbackInfo.setVisibility(View.GONE);
        playbackInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Manager.InstanceInfo> instances = Manager.getInstance().videoInstancesInfo();
                if (instances.isEmpty()) return;
                final List<Object> dialogItems = new ArrayList<Object>();
                String preferredQuality = config.getPreferredQuality();
                String currentApiType = "";
                for (int i = 0; i < instances.size(); i++) {
                    Manager.InstanceInfo info = instances.get(i);
                    if (!info.name.equals(currentApiType)) {
                        currentApiType = info.name;
                        if (!info.supportsAllQualities && !"360".equals(preferredQuality)) {
                            dialogItems.add(currentApiType + " (360p)");
                        } else {
                            dialogItems.add(currentApiType);
                        }
                    }
                    dialogItems.add(info);
                }

                BaseAdapter adapter = new BaseAdapter() {
                    @Override
                    public int getCount() {
                        return dialogItems.size();
                    }

                    @Override
                    public Object getItem(int position) {
                        return dialogItems.get(position);
                    }

                    @Override
                    public long getItemId(int position) {
                        return position;
                    }

                    @Override
                    public boolean isEnabled(int position) {
                        return !(dialogItems.get(position) instanceof String);
                    }

                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        Object item = dialogItems.get(position);
                        TextView textView = new TextView(context);
                        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
                        if (item instanceof String) {
                            textView.setText((String) item);
                            textView.setPadding(padding, padding, padding, padding / 2);
                            textView.setTypeface(null, Typeface.BOLD);
                            textView.setTextColor(Color.WHITE);
                            textView.setBackgroundColor(Color.DKGRAY);
                            textView.setTextSize(12);
                        } else {
                            Manager.InstanceInfo info = (Manager.InstanceInfo) item;
                            textView.setText(info.host);
                            textView.setPadding(padding * 2, padding, padding, padding);
                            textView.setTextSize(16);
                            if (NotPipe.SDK < 11) textView.setTextColor(Color.BLACK);
                        }
                        return textView;
                    }
                };

                new AlertDialog.Builder(context)
                        .setTitle(R.string.select_ins)
                        .setAdapter(adapter, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Object selected = dialogItems.get(which);
                                if (selected instanceof Manager.InstanceInfo) {
                                    Manager.InstanceInfo info = (Manager.InstanceInfo) selected;
                                    videoStream = info.instance;
                                    updatePlaybackViaText(info.host);

                                    if (!config.isUseExternalPlayer()) {
                                        thumbnail.setVisibility(View.INVISIBLE);
                                        play.setVisibility(View.GONE);
                                    }
                                    findViewById(R.id.video_loading).setVisibility(View.VISIBLE);

                                    if (videoView != null && videoPlaying && !config.isUseExternalPlayer()) {
                                        resetVideo();
                                    }
                                    findViewById(R.id.video_loading).setVisibility(View.VISIBLE);
                                    resolveStreamTask = new ResolveStreamTask(info.instance);
                                    resolveStreamTask.execute(videoId);
                                }
                            }
                        }).show();
            }
        });

        View.OnClickListener rateListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (video == null) return;
                VideoActions.like(VideoActivity.this, video, view.getId() == R.id.like);
            }
        };
        findViewById(R.id.save_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (video != null) VideoActions.showAddToPlaylistDialog(VideoActivity.this, video);
            }
        });
        findViewById(R.id.download_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (video != null) VideoActions.showDownloadDialog(VideoActivity.this, video);
            }
        });
        findViewById(R.id.like).setOnClickListener(rateListener);
        findViewById(R.id.dislike).setOnClickListener(rateListener);

        findViewById(R.id.video_meta).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (video == null) return;
                LinearLayout layout = new LinearLayout(context);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(15, 15, 15, 15);

                TextView viewsAndDate = new TextView(context);
                viewsAndDate.setText(getString(R.string.views, NumberFormat.getNumberInstance().format(video.views))
                        + "   " + DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(video.publishedAt));
                viewsAndDate.setTypeface(null, Typeface.BOLD);
                viewsAndDate.setPadding(0, 0, 0, 10);
                layout.addView(viewsAndDate);

                TextView desc = new TextView(context);
                desc.setText(video.description);
                desc.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                layout.addView(desc);

                ScrollView scroll = new ScrollView(context);
                scroll.addView(layout);

                new AlertDialog.Builder(context).setTitle(R.string.desc).setView(scroll).show();
            }
        });

        findViewById(R.id.channel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (video == null || video.channelId == null || video.channelId.length() == 0) return;
                Intent intent = new Intent(VideoActivity.this, ChannelActivity.class);
                intent.putExtra("ID", video.channelId);
                startActivity(intent);
                ImageLoader.clearCache();
                System.gc();
            }
        });

        View.OnClickListener playVideoListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playVideo();
            }
        };
        if (thumbnail != null) thumbnail.setOnClickListener(playVideoListener);
        if (play != null) play.setOnClickListener(playVideoListener);

        View fullScreenBtn = findViewById(R.id.full_screen);
        if (fullScreenBtn != null) {
            fullScreenBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    enterFullscreenByConfig();
                }
            });
        }

        if (videoFrame != null) {
            videoFrame.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, android.view.MotionEvent event) {
                    boolean used = controls != null && videoPrepared && controls.onTouch(event);
                    if (isFullscreenMode && event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                        systemUiHandler.removeCallbacks(hideSystemUiRunnable);
                        hideSystemUI();
                    }
                    return used;
                }
            });
        }
    }

    private void setupTabHost() {
        final TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        if (tabHost == null) return;
        if (tabHost.getTabWidget() != null && tabHost.getTabWidget().getChildCount() > 0) {
            tabHost.clearAllTabs();
        }
        tabHost.setup();
        // Android Holo has a bug where the first tab gets randomly removed in landscape orientation. It gets back again when the user enters and
        // exits full screen mode, and it isn't present in normal non-tablet UI, so we use a dummy tab as a tablet workaround
        boolean hasDummy = false;
        if (isTablet() && NotPipe.SDK >= 11) {
            tabHost.addTab(tabHost.newTabSpec("dummy").setIndicator("").setContent(new TabHost.TabContentFactory() {
                public View createTabContent(String tag) {
                    return new View(VideoActivity.this);
                }
            }));
            hasDummy = true;
        }
        tabHost.addTab(tabHost.newTabSpec("related").setIndicator(getString(R.string.related)).setContent(R.id.related));
        tabHost.addTab(tabHost.newTabSpec("comments").setIndicator(getString(R.string.comments)).setContent(R.id.comments));

        TabWidget widget = tabHost.getTabWidget();
        if (widget != null) {
            if (isTablet() && NotPipe.SDK >= 11) {
                hideDummyTab();
            } else if (NotPipe.SDK < 11) {
                int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 33f, getResources().getDisplayMetrics());
                for (int i = 0; i < widget.getChildCount(); i++) {
                    View child = widget.getChildAt(i);
                    if (child != null && child.getLayoutParams() != null) {
                        child.getLayoutParams().height = height;
                    }
                }
            }
        }
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            public void onTabChanged(final String tabId) {
                tabHost.post(new Runnable() {
                    @Override
                    public void run() {
                        if ("related".equals(tabId)) loadRelatedVideos();
                        else loadComments();
                    }
                });

                Runnable layoutFix = new Runnable() {
                    @Override
                    public void run() {
                        if (scrollView != null) {
                            scrollView.requestLayout();
                            scrollView.invalidate();
                        }
                        if (tabsScrollView != null) {
                            tabsScrollView.requestLayout();
                            tabsScrollView.invalidate();
                        }
                        checkVisibleItems();
                    }
                };
                if (tabsScrollView != null) {
                    tabsScrollView.post(layoutFix);
                } else if (scrollView != null) {
                    scrollView.post(layoutFix);
                }
            }
        });

        // If a dummy tab was inserted at index 0, select the "related" tab (index 1) explicitly
        if (hasDummy) tabHost.setCurrentTab(1);
    }

    private void setupScrollHandler() {
        scrollHandler.post(scrollCheckRunnable);
    }

    private android.os.Handler videoTimeoutHandler = new android.os.Handler();
    private Runnable videoTimeoutRunnable = null;

    private void setupVideoTimeout() {
        cancelVideoTimeout();
        videoTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                final boolean isOffline = !Utils.hasConnection(context);
                Object instanceToRemove = isUsingMetadataUrl ? api : videoStream;
                if (instanceToRemove != null && !isOffline)
                    Manager.getInstance().removeDeadInstance(instanceToRemove);
                isUsingMetadataUrl = false;
                resetVideo();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isOffline || streamRetryCount < MAX_STREAM_RETRIES) {
                            if (!isOffline) streamRetryCount++;
                            showVideoLoadingUI();
                            resolveStreamTask = new ResolveStreamTask(null);
                            resolveStreamTask.execute(videoId);
                        } else {
                            restoreVideoUI();
                            Toast.makeText(context, "Video stream timed out after multiple attempts.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        };
        videoTimeoutHandler.postDelayed(videoTimeoutRunnable, videoBufferTimeout);
    }

    private void cancelVideoTimeout() {
        if (videoTimeoutRunnable != null) {
            videoTimeoutHandler.removeCallbacks(videoTimeoutRunnable);
            videoTimeoutRunnable = null;
        }
    }

    private void loadRelatedVideos() {
        if (relatedLoaded || video == null) return;
        TextView noRelatedView = (TextView) findViewById(R.id.no_related);
        if (video.related != null && !video.related.isEmpty()) {
            relatedVideos.clear();
            relatedVideos.addAll(video.related);
            relatedAdapter.notifyDataSetChanged();
            relatedLoading.setVisibility(View.GONE);
            relatedLoaded = true;
            if (noRelatedView != null) noRelatedView.setVisibility(View.GONE);
            triggerLayoutFix(relatedList);
        } else {
            if (loadRelatedTask != null && loadRelatedTask.getStatus() != AsyncTask.Status.FINISHED) return;
            relatedLoading.setVisibility(View.VISIBLE);
            if (noRelatedView != null) noRelatedView.setVisibility(View.GONE);
            triggerLayoutFix(relatedLoading);
            loadRelatedTask = new LoadRelatedTask();
            loadRelatedTask.execute(videoId);
        }
    }

    private void loadComments() {
        if (commentsLoaded || video == null) return;
        if (video.comments != null && !video.comments.isEmpty()) {
            comments.clear();
            comments.addAll(video.comments);
            commentsAdapter.notifyDataSetChanged();
            commentsLoading.setVisibility(View.GONE);
            commentsLoaded = true;
            triggerLayoutFix(commentsList);
        } else {
            if (loadCommentsTask != null && loadCommentsTask.getStatus() != AsyncTask.Status.FINISHED) return;
            commentsLoading.setVisibility(View.VISIBLE);
            triggerLayoutFix(commentsLoading);
            loadCommentsTask = new LoadCommentsTask();
            loadCommentsTask.execute(videoId);
        }
    }

    /**
     * Fixes hangup found on Android 4.0-4.3 when the MediaPlayer hangs the whole UI before the server responds with a video stream
     */
    private void executeAsyncSetVideoUri(final String targetUrl) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean needsFallback = false;
                try {
                    videoView.setVideoURI(Uri.parse(targetUrl));
                } catch (RuntimeException e) {
                    if (!e.getClass().getSimpleName().contains("CalledFromWrongThreadException")) {
                        needsFallback = true;
                    }
                } catch (Exception e) {
                    needsFallback = true;
                }

                if (needsFallback) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                videoView.setVideoURI(Uri.parse(targetUrl));
                            } catch (Exception e) { e.printStackTrace(); }
                            videoView.requestFocus(0);
                            setupVideoTimeout();
                            videoView.requestLayout();
                            videoView.invalidate();
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            videoView.requestFocus(0);
                            setupVideoTimeout();
                            videoView.requestLayout();
                            videoView.invalidate();
                        }
                    });
                }
            }
        }).start();
    }

    private void loadVideoUri(final String targetUrl) {
        Log.d("VideoActivity", targetUrl);
        if (config.isAsyncSetVideoUri()) {
            boolean surfaceReady = false;
            try {
                android.view.Surface surface = videoView.getHolder().getSurface();
                if (surface != null) {
                    if (NotPipe.SDK >= 9) {
                        surfaceReady = (Boolean) surface.getClass().getMethod("isValid").invoke(surface);
                    } else {
                        surfaceReady = true; // For archaic devices, assuming non-null surface is ready
                    }
                }
            } catch (Exception ignored) {}

            if (surfaceReady) {
                // Surface is fully ready right now, safe to run async
                executeAsyncSetVideoUri(targetUrl);
            } else {
                // If the surface isn't ready, wait for surfaceCreated first
                videoView.getHolder().addCallback(new android.view.SurfaceHolder.Callback() {
                    @Override
                    public void surfaceCreated(android.view.SurfaceHolder holder) {
                        videoView.getHolder().removeCallback(this);
                        // Post inside UI queue to guarantee VideoView's own surfaceCreated finishes first
                        videoView.post(new Runnable() {
                            @Override
                            public void run() {
                                executeAsyncSetVideoUri(targetUrl);
                            }
                        });
                    }

                    @Override
                    public void surfaceChanged(android.view.SurfaceHolder holder, int format, int width, int height) {}

                    @Override
                    public void surfaceDestroyed(android.view.SurfaceHolder holder) {}
                });
            }
        } else {
            videoView.setVideoURI(Uri.parse(targetUrl));
            videoView.requestFocus(0);
            setupVideoTimeout();
            videoView.requestLayout();
            videoView.invalidate();
        }
    }

    /**
     * Android 4.1 has a weird bug with tabs, where if the video is currently playing, the tab contents can disappear.
     * This is especially visible on TouchWiz. This method fixes this issue.
     * @param targetView view to fix
     */
    private void triggerLayoutFix(final View targetView) {
        if (targetView == null) return;
        targetView.post(new Runnable() {
            @Override
            public void run() {
                // 1. Invalidate and lay out the target view
                targetView.requestLayout();
                targetView.invalidate();

                // On tablets, the layout is side-by-side and more stable.
                // We bypass the scroll-nudge and full decor-view invalidations to prevent scrolling stutter.
                if (!isTablet()) {
                    // 2. Invalidate parent scroll views & execute a 1-pixel scroll nudge to force rendering
                    if (scrollView != null) {
                        scrollView.requestLayout();
                        scrollView.invalidate();
                        scrollView.scrollBy(0, 1);
                        scrollView.scrollBy(0, -1);
                    }
                    if (tabsScrollView != null) {
                        tabsScrollView.requestLayout();
                        tabsScrollView.invalidate();
                        tabsScrollView.scrollBy(0, 1);
                        tabsScrollView.scrollBy(0, -1);
                    }

                    // 3. Force the absolute window root to re-evaluate all child views
                    if (getWindow() != null && getWindow().getDecorView() != null) {
                        getWindow().getDecorView().requestLayout();
                        getWindow().getDecorView().invalidate();
                    }
                }

                checkVisibleItems();
            }
        });
    }

    private void onVideoDataLoaded(final Video fetchedVideo) {
        video = fetchedVideo;
        LocalLibrary.getInstance().addToHistory(video);

        if (video.channelId != null && video.channelThumbnail != null && video.channelThumbnail.length() > 0)
            ChannelIconResolver.setResolved(video.channelId, video.channelThumbnail);
        findViewById(R.id.loading).setVisibility(View.GONE);
        videoLayout.setVisibility(View.VISIBLE);

        ((TextView) findViewById(R.id.title)).setText(video.title);
        ((TextView) findViewById(R.id.channel_title)).setText(video.channel);
        ((TextView) findViewById(R.id.subscribers)).setText(Utils.formatNumber(context, video.subscribers));
        if (video.likes > 0)
            ((Button) findViewById(R.id.like)).setText(Utils.formatNumber(context, video.likes));
        ((TextView) findViewById(R.id.views)).setText(getString(R.string.views, Utils.formatNumber(context, video.views)) +
                "   " + Utils.formatTimeAgo(context, video.publishedAt));

        resolvedQuality = determineQuality();
        final String quality = resolvedQuality;
        if (!VideoCache.hasValidStream(videoId, quality)) {
            if (config.isConvertVideos() || !"360".equals(quality) || video.videoUrl == null || video.videoUrl.length() == 0) {
                isUsingMetadataUrl = false;
                if (videoStream == null) {
                    videoStream = selectVideoStream(quality);
                } if (videoStream != null) {
                    updatePlaybackViaText(videoStream.getHost());
                }
            } else {
                isUsingMetadataUrl = true;
                updatePlaybackViaText(api.getHost());
            }
        } else {
            videoStream = VideoCache.getVideoStreamInstance();
            isUsingMetadataUrl = (videoStream == null);
            if (videoStream != null) {
                updatePlaybackViaText(videoStream.getHost());
            } else {
                updatePlaybackViaText(api.getHost());
            }
        }

        ImageLoader.loadImage(video.thumbnail, thumbnail, false);
        ImageLoader.loadImage(video.channelThumbnail, channelThumbnail, false);

        View tabHostView = findViewById(android.R.id.tabhost);
        if (tabHostView != null) {
            tabHostView.setVisibility(View.VISIBLE);
        }

        loadRelatedVideos();
        startHqPrefetch();
    }

    /* ---------- Fast HD: yt2009/YtAPILegacy prepare 480p+ on the server, so that starts as soon as the page opens ---------- */

    private boolean canPrefetchHq() {
        return resolvedQuality != null && !"360".equals(resolvedQuality) && !config.isConvertVideos()
                && config.isStreamPlayback() && !config.isUseExternalPlayer();
    }

    private void startHqPrefetch() {
        if (!canPrefetchHq() || hqResolving || hqUrl != null || VideoCache.hasValidStream(videoId, resolvedQuality)) return;
        hqResolving = true;
        final String id = videoId, quality = resolvedQuality;
        final int timeout = videoBufferTimeout;
        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = null;
                final VideoStream[] winner = new VideoStream[1];
                try {
                    url = Manager.getInstance().getVideoUrl(id, quality, timeout, null, winner);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                final String resolved = url;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        hqResolving = false;
                        if (isFinishing() || !id.equals(videoId)) return;
                        boolean play = waitingForHq;
                        waitingForHq = false;
                        if (resolved == null) {
                            // Let the normal path handle it (it falls back to 360p with its usual message)
                            if (play) {
                                isUsingMetadataUrl = false;
                                resolveStreamTask = new ResolveStreamTask(null, quality);
                                resolveStreamTask.execute(id, quality);
                            }
                            return;
                        }
                        hqUrl = resolved;
                        hqInstance = winner[0];
                        VideoCache.putStream(id, resolved, quality, winner[0]);
                        if (play) {
                            videoStream = hqInstance;
                            isUsingMetadataUrl = false;
                            videoUrl = resolved;
                            proceedPlay(resolved);
                        }
                    }
                });
            }
        }).start();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_video, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem qualityItem = menu.findItem(R.id.action_video_quality);
        if (qualityItem != null) {
            qualityItem.setVisible(!config.isConvertVideos());
        }

        MenuItem fullscreenItem = menu.findItem(R.id.action_full_screen);
        if (fullscreenItem != null) {
            boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            fullscreenItem.setVisible(isPortrait && !isFullscreenMode);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_video_quality) {
            showQualityDialog();
            return true;
        } else if (itemId == R.id.action_full_screen) {
            enterFullscreenByConfig();
            return true;
        } else if (itemId == R.id.action_speed) {
            showSpeedDialog();
            return true;
        } else if (itemId == R.id.action_captions) {
            showCaptionsDialog();
            return true;
        } else if (itemId == R.id.action_player_settings) {
            showPlayerSettings();
            return true;
        } else if (itemId == R.id.action_add_comment) {
            final View composer = io.github.gohoski.notpipe.ui.CommentsDialog.composer(this, videoId,
                    new io.github.gohoski.notpipe.ui.CommentsDialog.OnPosted() {
                        public void onPosted(Comment comment) {
                            comments.add(0, comment);
                            commentsAdapter.notifyDataSetChanged();
                        }
                    });
            new AlertDialog.Builder(this).setTitle(R.string.add_comment).setView(composer)
                    .setPositiveButton(android.R.string.cancel, null).show();
            return true;
        } else if (itemId == R.id.action_chapters) {
            showChapters();
            return true;
        } else if (itemId == R.id.action_loop) {
            looping = !looping;
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.setLooping(looping);
                } catch (Exception ignored) {}
            }
            Toast.makeText(context, looping ? R.string.loop_on : R.string.loop_off, Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_sleep_timer) {
            SleepTimer.showDialog(this);
            return true;
        } else if (itemId == R.id.action_pip) {
            enterPip();
            return true;
        } else if (itemId == R.id.action_listen_background || itemId == R.id.action_download
                || itemId == R.id.action_add_playlist) {
            if (video == null) {
                Toast.makeText(context, "Please wait for video to load", Toast.LENGTH_SHORT).show();
                return true;
            }
            if (itemId == R.id.action_listen_background) {
                listenInBackground();
            } else if (itemId == R.id.action_download) {
                VideoActions.showDownloadDialog(this, video);
            } else {
                VideoActions.showAddToPlaylistDialog(this, video);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Music mode: stops the video player and continues with audio only in MusicService.
     */
    /* ---------- Player extras: SponsorBlock, captions, speed, autoplay, double-tap seek ---------- */

    private void setupPlayerExtras() {
        if (videoFrame != null) {
            controls = new PlayerControls(this, videoFrame, videoView, new PlayerControls.Host() {
                public void onToggleFullscreen() {
                    if (isFullscreenMode) leaveFullscreen(); else enterFullscreenByConfig();
                }
                public void onSpeed() { showSpeedDialog(); }
                public void onCaptions() { showCaptionsDialog(); }
                public void onQuality() {
                    if (config.isConvertVideos()) return;
                    showQualityDialog();
                }
                public void onPip() { enterPip(); }
                public void onMore() { openOptionsMenu(); }
                public void onNext() { playNext(true); }
                public void onPrevious() { playPrevious(); }
                public boolean isFullscreen() { return isFullscreenMode; }
                public List<VideoInfo> getRelatedVideos() { return relatedVideos; }
                public void onOpenRelated(VideoInfo item) { openFromPlayer(item); }
            });
        }
        if (videoFrame != null) {
            captionView = new TextView(this);
            captionView.setTextColor(Color.WHITE);
            captionView.setBackgroundColor(0xAA000000);
            captionView.setTextSize(15);
            captionView.setGravity(android.view.Gravity.CENTER);
            captionView.setPadding(8, 4, 8, 4);
            captionView.setVisibility(View.GONE);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);
            params.bottomMargin = 24;
            videoFrame.addView(captionView, params);
        }
        final String id = videoId;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<int[]> segments = PlayerExtras.loadSponsorSegments(id);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        sponsorSegments = segments;
                    }
                });
            }
        }).start();
    }

    private void startExtrasTicker() {
        handler.removeCallbacks(extrasTicker);
        handler.postDelayed(extrasTicker, 400);
    }

    /**
     * The stream stopped delivering data while "playing": reopen it at the same spot,
     * and after a few tries ask for a fresh stream (often another instance).
     */
    private void recoverFromStall(int position) {
        if (videoUrl == null || videoUrl.startsWith("file://") || videoUrl.startsWith("/")) return;
        // HD is a finished file on the yt2009 instance: it catches up on its own, and switching
        // instances would make the new one download and process it all over again, so just wait
        String playingQuality = resolvedQuality != null ? resolvedQuality : config.getPreferredQuality();
        if (playingQuality != null && !"360".equals(playingQuality)) return;
        String url = videoUrl;
        if (controls != null) controls.setBuffering(false);
        videoPosition = position;
        stallRecoveries++;
        resetVideo();
        showVideoLoadingUI();
        if (stallRecoveries <= 1) {
            videoUrl = url;
            videoPlaying = true;
            loadVideoUri(url);
        } else {
            // Reopening didn't help: the instance itself is slow, so switch to another one
            stallRecoveries = 0;
            String slowHost = videoStream != null ? videoStream.getHost() : null;
            Manager.getInstance().reportSlowInstance(slowHost);
            String quality = resolvedQuality != null ? resolvedQuality : config.getPreferredQuality();
            videoStream = null;
            for (int i = 0; i < 5 && slowHost != null; i++) {
                VideoStream candidate = selectVideoStream(quality == null ? "360" : quality);
                if (candidate == null || !slowHost.equals(candidate.getHost())) {
                    videoStream = candidate;
                    break;
                }
            }
            VideoCache.clearAll();
            resolveStreamTask = new ResolveStreamTask(null);
            resolveStreamTask.execute(videoId);
        }
    }

    private void tickExtras() {
        if (videoView == null || !videoPrepared) return;
        if (SleepTimer.consumeVideoPause()) {
            try {
                videoView.pause();
            } catch (Exception ignored) {}
            return;
        }
        int position;
        try {
            if (!videoView.isPlaying()) {
                stallTicks = 0;
                if (controls != null) controls.setBuffering(false);
                return;
            }
            position = videoView.getCurrentPosition();
        } catch (Exception e) {
            return;
        }
        if (position != stallPosition) {
            stallPosition = position;
            stallTicks = 0;
            if (controls != null) controls.setBuffering(false);
        } else if (++stallTicks == 2 && controls != null) {
            controls.setBuffering(true);
        } else if (stallTicks >= STALL_TICKS) {
            stallTicks = 0;
            recoverFromStall(position);
            return;
        }
        for (int i = 0; i < sponsorSegments.size(); i++) {
            int[] seg = sponsorSegments.get(i);
            if (position >= seg[0] && position < seg[1] - 500) {
                videoView.seekTo(seg[1]);
                Toast.makeText(context, R.string.sponsor_skipped, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (captionView != null) {
            String text = PlayerExtras.captionAt(captions, position);
            if (text == null) {
                captionView.setVisibility(View.GONE);
            } else {
                captionView.setText(text);
                captionView.setVisibility(View.VISIBLE);
            }
        }
    }

    /** PlaybackParams exist on Android 6.0+ only, so it's done through reflection */
    private void applySpeed(float speed) {
        if (mediaPlayer == null || NotPipe.SDK < 23) return;
        try {
            Class<?> paramsClass = Class.forName("android.media.PlaybackParams");
            Object params = paramsClass.newInstance();
            params = paramsClass.getMethod("setSpeed", float.class).invoke(params, speed);
            MediaPlayer.class.getMethod("setPlaybackParams", paramsClass).invoke(mediaPlayer, params);
            if (!videoView.isPlaying() && speed != 1f) videoView.pause();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSpeedDialog() {
        if (NotPipe.SDK < 23) {
            Toast.makeText(context, R.string.speed_unsupported, Toast.LENGTH_LONG).show();
            return;
        }
        final float[] speeds = {0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f};
        String[] labels = new String[speeds.length];
        int checked = 2;
        for (int i = 0; i < speeds.length; i++) {
            labels[i] = speeds[i] + "x";
            if (speeds[i] == PlayerExtras.getSpeed()) checked = i;
        }
        new AlertDialog.Builder(context).setTitle(R.string.playback_speed)
                .setSingleChoiceItems(labels, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        PlayerExtras.setSpeed(speeds[which]);
                        applySpeed(speeds[which]);
                        if (controls != null) controls.setSpeed(speeds[which]);
                    }
                }).show();
    }

    private void showCaptionsDialog() {
        Toast.makeText(context, R.string.loading_captions, Toast.LENGTH_SHORT).show();
        final String id = videoId;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<PlayerExtras.Track> tracks = PlayerExtras.loadTracks(id);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) return;
                        if (tracks.isEmpty()) {
                            Toast.makeText(context, R.string.no_captions, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String[] labels = new String[tracks.size() + 1];
                        labels[0] = getString(R.string.captions_off);
                        for (int i = 0; i < tracks.size(); i++) labels[i + 1] = tracks.get(i).label;
                        new AlertDialog.Builder(context).setTitle(R.string.captions)
                                .setItems(labels, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (which == 0) {
                                            captions = null;
                                            if (captionView != null) captionView.setVisibility(View.GONE);
                                        } else {
                                            loadCaptionTrack(tracks.get(which - 1).url);
                                        }
                                    }
                                }).show();
                    }
                });
            }
        }).start();
    }

    private void loadCaptionTrack(final String url) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<PlayerExtras.Caption> loaded = null;
                try {
                    loaded = PlayerExtras.loadCaptions(url);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                final List<PlayerExtras.Caption> result = loaded;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (result == null || result.isEmpty()) {
                            Toast.makeText(context, R.string.no_captions, Toast.LENGTH_SHORT).show();
                        } else {
                            captions = result;
                        }
                    }
                });
            }
        }).start();
    }

    private void showPlayerSettings() {
        final String[] keys = {PlayerExtras.KEY_AUTOPLAY, PlayerExtras.KEY_SPONSORBLOCK, PlayerExtras.KEY_RESUME,
                PlayerExtras.KEY_AUTO_PIP};
        String[] labels = {getString(R.string.autoplay), getString(R.string.sponsorblock), getString(R.string.resume_playback),
                getString(R.string.auto_pip)};
        boolean[] checked = new boolean[keys.length];
        for (int i = 0; i < keys.length; i++) checked[i] = PlayerExtras.isEnabled(keys[i]);
        new AlertDialog.Builder(context).setTitle(R.string.player_settings)
                .setMultiChoiceItems(labels, checked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        PlayerExtras.setEnabled(keys[which], isChecked);
                    }
                })
                .setPositiveButton(android.R.string.ok, null).show();
    }

    /** Chapters are the timestamps ("1:23 Title") in the description, like YouTube does it */
    private void showChapters() {
        if (video == null || video.description == null) return;
        final List<Integer> times = new ArrayList<Integer>();
        final List<String> titles = new ArrayList<String>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?m)^\\s*[\\[(]?((?:\\d{1,2}:)?\\d{1,2}:\\d{2})[\\])]?\\s*[-–—:|]?\\s*(.*)$")
                .matcher(video.description);
        while (m.find()) {
            int seconds = io.github.gohoski.notpipe.util.Feeds.parseDuration(m.group(1));
            if (seconds < 0) continue;
            times.add(seconds * 1000);
            titles.add(m.group(1) + "  " + m.group(2).trim());
        }
        if (times.isEmpty()) {
            Toast.makeText(context, R.string.no_chapters, Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(context).setTitle(R.string.chapters)
                .setItems(titles.toArray(new String[titles.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (videoPrepared) {
                            videoView.seekTo(times.get(which));
                        } else {
                            videoPosition = times.get(which);
                            playVideo();
                        }
                    }
                }).show();
    }

    /** Autoplay: opens the first related video that isn't in the watch history yet */
    /** Videos left by next/autoplay in this session, so "previous" can go back to them */
    private static final List<String> SESSION_HISTORY = new ArrayList<String>();

    /** Back to the video before next/autoplay; restarts the current one if there's none (like YouTube) */
    private void playPrevious() {
        if (SESSION_HISTORY.isEmpty()) {
            if (videoPrepared) videoView.seekTo(0);
            return;
        }
        String previous = SESSION_HISTORY.remove(SESSION_HISTORY.size() - 1);
        Intent intent = new Intent(this, VideoActivity.class);
        intent.putExtra("ID", previous);
        startActivity(intent);
        finish();
    }

    private void playNextIfAutoplay() {
        playNext(false);
    }

    /** Opens a video picked in the player's "More videos" row, keeping it in the session history */
    private void openFromPlayer(VideoInfo item) {
        SESSION_HISTORY.add(videoId);
        if (SESSION_HISTORY.size() > 50) SESSION_HISTORY.remove(0);
        Intent intent = new Intent(this, VideoActivity.class);
        intent.putExtra("ID", item.id);
        startActivity(intent);
        finish();
    }

    /** @param force true when the user pressed "next", false for autoplay */
    private void playNext(boolean force) {
        if ((!force && !PlayerExtras.isEnabled(PlayerExtras.KEY_AUTOPLAY)) || relatedVideos.isEmpty() || isFinishing()) return;
        List<VideoInfo> history = LocalLibrary.getInstance().getHistory();
        VideoInfo next = relatedVideos.get(0);
        for (int i = 0; i < relatedVideos.size(); i++) {
            boolean watched = false;
            for (int j = 0; j < history.size() && j < 50; j++) {
                if (history.get(j).id.equals(relatedVideos.get(i).id)) { watched = true; break; }
            }
            if (!watched) { next = relatedVideos.get(i); break; }
        }
        Toast.makeText(context, getString(R.string.up_next, next.title), Toast.LENGTH_SHORT).show();
        SESSION_HISTORY.add(videoId);
        if (SESSION_HISTORY.size() > 50) SESSION_HISTORY.remove(0);
        Intent intent = new Intent(this, VideoActivity.class);
        intent.putExtra("ID", next.id);
        startActivity(intent);
        finish();
    }

    private void listenInBackground() {
        if (videoView != null && videoPlaying) {
            try {
                if (videoPrepared) videoView.pause();
            } catch (Exception ignored) {}
        }
        List<VideoInfo> queue = new ArrayList<VideoInfo>();
        queue.add(video);
        for (int i = 0; i < relatedVideos.size() && i < 20; i++) {
            queue.add(relatedVideos.get(i));
        }
        MusicService.play(this, queue, 0);
        Toast.makeText(context, R.string.playing_in_background, Toast.LENGTH_SHORT).show();
    }

    private void enterFullscreenByConfig() {
        isFullscreenMode = true;
        isTabletFullscreen = true;
        if (config.isFullscreenRotateLandscape()) {
            // Lock the orientation to landscape first to block sensor rotation
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // Already in landscape, enter fullscreen directly
                enterFullscreenMode();
            }
            // If in portrait, setRequestedOrientation will trigger onConfigurationChanged,
            // which will subsequently handle entering fullscreen.
        } else {
            enterFullscreenMode();
        }
    }

    /**
     * Renders quality options selection dialog. Includes warning prompts for high quality choices
     * and duration restriction validations based on standard player behavior.
     */
    private void showQualityDialog() {
        if (video == null) {
            Toast.makeText(context, "Please wait for video to load", Toast.LENGTH_SHORT).show();
            return;
        }

        if (video.length > 3600) {
            Toast.makeText(context, R.string.long_360, Toast.LENGTH_LONG).show();
            return;
        }

        String currentQuality = config.getPreferredQuality();
        if (currentQuality == null) {
            currentQuality = "360";
        }

        final String[] displayQualities = getResources().getStringArray(R.array.qualities);
        final String[] qualities = new String[displayQualities.length];
        for (int i = 0; i < displayQualities.length; i++) {
            qualities[i] = displayQualities[i].replace("p", "");
        }

        int checkedItem = 0;
        for (int i = 0; i < qualities.length; i++) {
            if (qualities[i].equals(currentQuality)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.select_quality)
                .setSingleChoiceItems(displayQualities, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        final String selectedQuality = qualities[which];
                        if (selectedQuality.equals(config.getPreferredQuality())) return; // Avoid unnecessary reloads if selecting the same quality

                        if ("1080".equals(selectedQuality)) {
                            new AlertDialog.Builder(context)
                                    .setTitle(android.R.string.dialog_alert_title)
                                    .setMessage(R.string.experimental_1080)
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface d, int w) {
                                            applyQuality(selectedQuality);
                                        }
                                    })
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .show();
                        } else applyQuality(selectedQuality);
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    /**
     * Formally saves the preferred quality selection, stops existing play states,
     * and performs stream re-resolution.
     */
    private void applyQuality(String newQuality) {
        config.setPreferredQuality(newQuality);
        ConfigManager.getInstance().saveConfig(config);
        config = ConfigManager.getInstance().getConfig();

        resolvedQuality = newQuality;
        videoBufferTimeout = getTimeoutForQuality(newQuality);

        if (videoView != null) {
            try {
                if (videoPrepared) {
                    int pos = videoView.getCurrentPosition();
                    if (pos > 0) {
                        videoPosition = pos;
                    }
                }
            } catch (Exception ignored) {}

            resetVideo();
            restoreVideoUI();
        }

        if (video != null) {
            showVideoLoadingUI();
            updateProgressMessage(newQuality);

            if (resolveStreamTask != null) {
                resolveStreamTask.cancel(true);
            }
            if (downloadVideoTask != null) {
                downloadVideoTask.cancel(true);
            }

            if (VideoCache.hasValidStream(videoId, newQuality)) {
                videoUrl = VideoCache.getStreamUrl();
                videoStream = VideoCache.getVideoStreamInstance();
                isUsingMetadataUrl = (videoStream == null);
                proceedPlay(videoUrl);
                return;
            }

            if (config.isConvertVideos() || !"360".equals(newQuality) || video.videoUrl == null || video.videoUrl.length() == 0) {
                isUsingMetadataUrl = false;

                if (videoStream == null) {
                    videoStream = selectVideoStream(newQuality);
                }

                resolveStreamTask = new ResolveStreamTask(videoStream, newQuality);
                resolveStreamTask.execute(videoId, newQuality);
            } else {
                isUsingMetadataUrl = true;
                videoUrl = video.videoUrl;
                VideoCache.putStream(videoId, videoUrl, "360", videoStream);
                proceedPlay(videoUrl);
            }
        }
    }

    private class LoadVideoTask extends AsyncTask<String, Void, Video> {
        private boolean contentUnavailable;

        @Override
        protected Video doInBackground(String... params) {
            try {
                if (isCancelled()) return null;
                final String id = params[0];
                Metadata[] winner = new Metadata[1];
                Video result = Manager.getInstance().raceMetadata(new Manager.MetadataTask<Video>() {
                    public Video run(Metadata metadata) throws Exception {
                        return metadata.getVideo(id);
                    }
                }, winner);
                // Related videos and comments then use the instance that answered first
                if (winner[0] != null) api = winner[0];
                return result;
            } catch (ContentUnavailableException e) {
                contentUnavailable = true;
                e.printStackTrace();
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(final Video fetchedVideo) {
            if (isCancelled()) return;
            if (fetchedVideo == null) {
                findViewById(R.id.loading).setVisibility(View.GONE);
                if (contentUnavailable) {
                    Toast.makeText(context, R.string.content_unavailable, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, R.string.metadata_fail, Toast.LENGTH_SHORT).show();
                }
                return;
            }

            VideoCache.put(videoId, fetchedVideo, api);
            onVideoDataLoaded(fetchedVideo);
        }
    }

    private class ResolveStreamTask extends AsyncTask<String, Void, String> {
        private VideoStream targetInstance;
        private VideoStream[] successInstance = new VideoStream[1];
        private String errorMessage;
        private boolean isFileNotFound;
        private String requestedQuality;
        private String taskQuality;

        ResolveStreamTask(VideoStream targetInstance) {
            this.targetInstance = targetInstance;
        }

        ResolveStreamTask(VideoStream targetInstance, String quality) {
            this.targetInstance = targetInstance;
            this.taskQuality = quality;
        }

        @Override
        protected void onPreExecute() {
            if (targetInstance == null) {
                // Reuse the instance already picked by LoadVideoTask
                if (videoStream == null) {
                    String quality = taskQuality != null ? taskQuality : config.getPreferredQuality();
                    videoStream = selectVideoStream(quality);
                }
            } else {
                videoStream = targetInstance;
            }
            if (videoStream != null) {
                updatePlaybackViaText(videoStream.getHost());
            } else {
                updatePlaybackViaText(api.getHost());
            }
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                Utils.waitForConnection(context);
            } catch (IOException e) {
                return null;
            }
            try {
                String id = params[0];
                if (isCancelled()) return null;
                File cachedVideo = getCachedVideoFile(id);
                if (cachedVideo.exists()) return cachedVideo.getAbsolutePath();
                String quality = params.length > 1 ? params[1] : (taskQuality != null ? taskQuality : (resolvedQuality != null ? resolvedQuality : config.getPreferredQuality()));
                if (quality == null) {
                    quality = "360";
                }
                requestedQuality = quality;
                int timeout = getTimeoutForQuality(quality);
                if (targetInstance != null) {
                    try {
                        if (config.isConvertVideos() && targetInstance instanceof Conversion) {
                            if (isCancelled()) return null;
                            return ((Conversion) targetInstance).getConvUrl(id, config.getConvertCodec());
                        }
                        if (isCancelled()) return null;
                        return targetInstance.getVideoUrl(id, quality, timeout);
                    } catch (Exception e) {
                        if (isCancelled()) return null;
                        // If it is a FileNotFoundException and conversion is NOT enabled,
                        // it means the video itself is missing (404), so we should rethrow it.
                        if (e instanceof java.io.FileNotFoundException && !config.isConvertVideos()) {
                            throw e;
                        }
                        // Exclude this instance from conversion list on any failure
                        if (config.isConvertVideos() && targetInstance instanceof Conversion) {
                            Manager.getInstance().getConversion().remove((Conversion) targetInstance);
                        }
                        // Set targetInstance to null to prevent the task from prioritizing it in subsequent Manager calls
                        targetInstance = null;
                    }
                }
                if (isCancelled()) return null;
                if (config.isConvertVideos())
                    return Manager.getInstance().getConvUrl(id, quality, config.getConvertCodec(), targetInstance, successInstance);
                return Manager.getInstance().getVideoUrl(id, quality, timeout, targetInstance, successInstance);
            } catch (java.io.FileNotFoundException e) {
                isFileNotFound = true;
                errorMessage = e.getMessage();
                return null;
            } catch (Exception e) {
                errorMessage = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String resultUrl) {
            if (isCancelled()) return;
            isUsingMetadataUrl = false;

            if (resultUrl != null) {
                if (successInstance[0] != null) {
                    videoStream = successInstance[0];
                }
                VideoCache.putStream(videoId, resultUrl, requestedQuality, videoStream);
                if (resultUrl.startsWith(Environment.getExternalStorageDirectory().getPath())) {
                    updatePlaybackViaText(getString(R.string.cache));
                    proceedPlay(resultUrl);
                } else {
                    if (videoStream != null) updatePlaybackViaText(videoStream.getHost());
                    TextView progressView = (TextView) findViewById(R.id.video_progress);
                    if (config.isConvertVideos()) {
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        if (progressView != null) {
                            progressView.setText(R.string.conv_long);
                            progressView.setVisibility(View.VISIBLE);
                        }
                        proceedPlay(resultUrl);
                    } else {
                        proceedPlay(resultUrl);
                    }
                }
            } else if (isFileNotFound) {
                if ("360".equals(requestedQuality)) {
                    resetVideo();
                    restoreVideoUI();
                    if (targetInstance != null) {
                        Manager.getInstance().removeDeadInstance(targetInstance);
                    }
                    Toast.makeText(context, "All instances failed to provide video.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, R.string.no_quality, Toast.LENGTH_SHORT).show();
                    /*resolvedQuality = "360";*/ videoBufferTimeout = 60000;
                    if (video != null && video.videoUrl != null && video.videoUrl.length() > 0) {
                        isUsingMetadataUrl = true;
                        updatePlaybackViaText(api.getHost());
                        proceedPlay(video.videoUrl);
                    } else {
                        isUsingMetadataUrl = false;
                        resolveStreamTask = new ResolveStreamTask(null, "360");
                        resolveStreamTask.execute(videoId, "360");
                    }
                }
            } else {
                resetVideo();
                restoreVideoUI();
                updatePlaybackViaText(videoStream != null ? videoStream.getHost() : api.getHost());

                if (errorMessage != null) {
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
                } else if (targetInstance != null) {
                    Manager.getInstance().removeDeadInstance(targetInstance);
                    Toast.makeText(context, "Failed to connect to this instance.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Failed to fetch video URL", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void proceedPlay(final String targetUrl) {
        boolean isLocal = targetUrl.startsWith(Environment.getExternalStorageDirectory().getPath()) || targetUrl.startsWith("file://");
        boolean forceDownload = config.isConvertVideos() && !isLocal;
        boolean shouldStream = (config.isStreamPlayback() && !forceDownload) || isLocal;

        if (isLocal)
            updatePlaybackViaText(getString(R.string.cache));
        else if (isUsingMetadataUrl)
            updatePlaybackViaText(api.getHost());
        else if (videoStream != null)
            updatePlaybackViaText(videoStream.getHost());

        if (config.isUseExternalPlayer()) {
            if (shouldStream) {
                findViewById(R.id.video_loading).setVisibility(View.GONE);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
                intent.setDataAndType(Uri.parse(targetUrl), "video/mp4");
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                downloadVideoTask = new DownloadVideoTask();
                downloadVideoTask.execute(targetUrl);
            }
        } else {
            resetVideo();
            videoPlaying = true;

            if (isActivityStopped) {
                // Keep the background resolution running, but do not touch the UI/player yet.
                // Save the URL, and let onResume handle the playback when the user returns.
                videoUrl = shouldStream ? targetUrl : null;
                if (!shouldStream) {
                    downloadVideoTask = new DownloadVideoTask();
                    downloadVideoTask.execute(targetUrl);
                }
                return;
            }

            applyOpenCoreLayoutFix();
            attachVideoListeners();

            if (shouldStream) {
                videoUrl = targetUrl;
                videoView.setVisibility(View.VISIBLE);
                loadVideoUri(targetUrl);
            } else {
                videoUrl = null;
                downloadVideoTask = new DownloadVideoTask();
                downloadVideoTask.execute(targetUrl);
            }
        }
    }

    private class DownloadVideoTask extends AsyncTask<String, Integer, File> {
        private TextView progressView;
        private boolean sdCardNotMounted = false;
        private boolean noSpaceError = false;

        @Override
        protected void onPreExecute() {
            progressView = (TextView) findViewById(R.id.video_progress);
            progressView.setVisibility(View.VISIBLE);
            // Prevent the screen from going to sleep while downloading
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        @Override
        protected File doInBackground(String... params) {
            try {
                if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                    sdCardNotMounted = true;
                    return null;
                }
                File sdCard = Environment.getExternalStorageDirectory();
                android.os.StatFs stat = new android.os.StatFs(sdCard.getPath());
                long availableSpace = (long) stat.getBlockSize() * (long) stat.getAvailableBlocks();
                if (availableSpace < 150L * 1024L * 1024L) { // 150 Megabytes
                    noSpaceError = true;
                    return null;
                }

                String downloadUrl = params[0];
                File videoFile = getCachedVideoFile(videoId);
                if (!videoFile.exists()) {
                    int timeout = config.isConvertVideos() ? HttpClient.CONVERSION_TIMEOUT : videoBufferTimeout;
                    HttpClient.downloadToFile(downloadUrl, videoFile.getAbsolutePath(), timeout, new HttpClient.DownloadProgressListener() {
                        private long lastUpdateTime = 0;
                        private long lastUpdateBytes = 0;

                        @Override
                        public boolean onProgress(final long bytesDownloaded, final long totalBytes) {
                            if (isCancelled()) return false;

                            // Don't overwrite the "Converting..." label until the server starts sending data
                            if (bytesDownloaded == 0) return true;

                            long currentTime = System.currentTimeMillis();
                            if (lastUpdateTime == 0) {
                                lastUpdateTime = currentTime;
                                lastUpdateBytes = bytesDownloaded;
                                return true;
                            }

                            long timeElapsed = currentTime - lastUpdateTime;
                            if (timeElapsed >= 500) {
                                long bytesSentInInterval = bytesDownloaded - lastUpdateBytes;
                                int speedKB = 0;
                                if (timeElapsed > 0) {
                                    speedKB = (int) ((bytesSentInInterval * 1000) / (timeElapsed * 1024));
                                }
                                int percent = -1;
                                if (totalBytes > 0) {
                                    percent = (int) ((bytesDownloaded * 100) / totalBytes);
                                }

                                // Pass current progress, instantaneous speed, and total KB downloaded
                                publishProgress(percent, speedKB, (int) (bytesDownloaded / 1024));

                                lastUpdateTime = currentTime;
                                lastUpdateBytes = bytesDownloaded;
                            }
                            return true;
                        }
                    });
                }
                if (isCancelled()) return null;
                return videoFile;
            } catch (Exception e) {
                e.printStackTrace();
                String msg = e.getMessage();
                if (e instanceof IOException && msg != null &&
                        (msg.toLowerCase().contains("no space left") || msg.contains("ENOSPC"))) {
                    noSpaceError = true;
                }
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (values.length >= 2) {
                int percent = values[0];
                int speedKB = values[1];
                if (percent >= 0) {
                    progressView.setText(getString(R.string.download_progress_percent, percent, speedKB));
                } else {
                    if (values.length >= 3) {
                        double downloadedMB = values[2] / 1024.0;
                        progressView.setText(String.format(getString(R.string.download_progress_mb), downloadedMB, speedKB));
                    } else {
                        progressView.setText(getString(R.string.download_speed, speedKB));
                    }
                }
            } else if (values.length == 1) {
                progressView.setText(getString(R.string.percent, values[0]));
            }
        }

        @Override
        protected void onCancelled() {
            // Restore screen sleep behavior if download was aborted
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (progressView != null) {
                progressView.setVisibility(View.GONE);
            }
        }

        @Override
        protected void onPostExecute(File videoFile) {
            if (isCancelled()) return;
            if (progressView != null) progressView.setVisibility(View.GONE);

            if (sdCardNotMounted) {
                resetVideo(); restoreVideoUI();
                Toast.makeText(context, R.string.sd_card, Toast.LENGTH_LONG).show();
                return;
            } if (noSpaceError) {
                resetVideo(); restoreVideoUI();
                Toast.makeText(context, R.string.sd_card_space, Toast.LENGTH_LONG).show();
                return;
            }

            if (videoFile != null) {
                if (config.isUseExternalPlayer()) {
                    findViewById(R.id.video_loading).setVisibility(View.GONE);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(videoFile), "video/mp4");
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    resetVideo();
                    videoUrl = Uri.fromFile(videoFile).toString();
                    videoPlaying = true;
                    if (isActivityStopped) return;
                    applyOpenCoreLayoutFix();
                    attachVideoListeners();

                    videoView.setVisibility(View.VISIBLE);
                    loadVideoUri(videoUrl);
                }
            } else {
                boolean isOffline = !Utils.hasConnection(context);
                Object instanceToRemove = isUsingMetadataUrl ? api : videoStream;
                if (instanceToRemove != null && !isOffline) {
                    Manager.getInstance().removeDeadInstance(instanceToRemove);
                }
                isUsingMetadataUrl = false;
                resetVideo();

                if (isOffline || streamRetryCount < MAX_STREAM_RETRIES) {
                    if (!isOffline) streamRetryCount++;
                    showVideoLoadingUI();
                    resolveStreamTask = new ResolveStreamTask(null);
                    resolveStreamTask.execute(videoId);
                } else {
                    restoreVideoUI();
                    Toast.makeText(context, "Download failed after multiple attempts.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private class LoadCommentsTask extends AsyncTask<String, Void, List<Comment>> {
        @Override
        protected List<Comment> doInBackground(String... params) {
            try {
                if (isCancelled()) return null;
                final String id = params[0];
                return Manager.getInstance().raceMetadata(new Manager.MetadataTask<List<Comment>>() {
                    public List<Comment> run(Metadata m) throws Exception {
                        List<Comment> result = m.getComments(id);
                        if (result == null || result.isEmpty()) throw new IOException("No comments");
                        return result;
                    }
                }, null);
            } catch (Exception e) {
                return new ArrayList<Comment>();
            }
        }

        @Override
        protected void onPostExecute(List<Comment> result) {
            if (isCancelled()) return;
            TextView noCommentsView = (TextView) findViewById(R.id.no_comments);

            if (result != null) {
                comments.clear();
                comments.addAll(result);
                commentsAdapter.notifyDataSetChanged();
                commentsLoaded = true;

                if (noCommentsView != null) {
                    if (comments.isEmpty()) {
                        noCommentsView.setVisibility(View.VISIBLE);
                    } else {
                        noCommentsView.setVisibility(View.GONE);
                    }
                }
            } else {
                if (noCommentsView != null) noCommentsView.setVisibility(View.GONE);
            }
            commentsLoading.setVisibility(View.GONE);

            triggerLayoutFix(commentsList);
        }
    }

    private class LoadRelatedTask extends AsyncTask<String, Void, List<VideoInfo>> {
        @Override
        protected List<VideoInfo> doInBackground(String... params) {
            try {
                if (isCancelled()) return null;
                return api.getRelated(params[0]);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<VideoInfo> result) {
            if (isCancelled()) return;
            TextView noRelatedView = (TextView) findViewById(R.id.no_related);
            if (result != null) {
                relatedVideos.clear();
                relatedVideos.addAll(result);
                relatedAdapter.notifyDataSetChanged();
                relatedLoaded = true;
                if (noRelatedView != null) {
                    if (relatedVideos.isEmpty()) {
                        noRelatedView.setVisibility(View.VISIBLE);
                    } else {
                        noRelatedView.setVisibility(View.GONE);
                    }
                }
            } else {
                if (noRelatedView != null) noRelatedView.setVisibility(View.GONE);
            }
            relatedLoading.setVisibility(View.GONE);
            triggerLayoutFix(relatedList);
        }
    }
}