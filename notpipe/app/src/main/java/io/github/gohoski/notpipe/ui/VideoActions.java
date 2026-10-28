package io.github.gohoski.notpipe.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;

import io.github.gohoski.notpipe.DownloadService;
import io.github.gohoski.notpipe.R;
import io.github.gohoski.notpipe.api.Yt2009Account;
import io.github.gohoski.notpipe.data.Playlist;
import io.github.gohoski.notpipe.data.VideoInfo;
import io.github.gohoski.notpipe.util.Compat;
import io.github.gohoski.notpipe.util.LocalLibrary;

/**
 * Created by Claude on 25.10.2026.
 * Dialogs shared by every screen that shows videos: download and "add to playlist".
 */

public class VideoActions {
    private VideoActions() {}

    /**
     * Lets the user pick a video quality or audio only, then queues the download.
     */
    public static void showDownloadDialog(final Activity activity, final VideoInfo video) {
        if (!Compat.ensureStoragePermission(activity)) {
            Toast.makeText(activity, R.string.storage_permission, Toast.LENGTH_LONG).show();
            return;
        }
        final String[] displayQualities = activity.getResources().getStringArray(R.array.qualities);
        final String[] items = new String[displayQualities.length + 1];
        items[0] = activity.getString(R.string.audio_only);
        System.arraycopy(displayQualities, 0, items, 1, displayQualities.length);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.download)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String quality = which == 0 ? DownloadService.AUDIO : displayQualities[which - 1].replace("p", "");
                        DownloadService.enqueue(activity, video.id, video.title, quality);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Queues the audio of every video in the list.
     */
    public static void downloadAllAudio(Activity activity, List<VideoInfo> videos) {
        if (!Compat.ensureStoragePermission(activity)) {
            Toast.makeText(activity, R.string.storage_permission, Toast.LENGTH_LONG).show();
            return;
        }
        for (int i = 0; i < videos.size(); i++) {
            DownloadService.enqueue(activity, videos.get(i).id, videos.get(i).title, DownloadService.AUDIO);
        }
    }

    /**
     * When signed in through yt2009, first asks whether to save on the device or to the YouTube account.
     */
    public static void showAddToPlaylistDialog(final Activity activity, final VideoInfo video) {
        if (!Yt2009Account.getInstance().isSignedIn()) {
            showLocalPlaylistDialog(activity, video);
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.add_to_playlist)
                .setItems(new String[]{activity.getString(R.string.on_this_device), activity.getString(R.string.youtube_account)},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) showLocalPlaylistDialog(activity, video);
                                else new AccountPlaylistsTask(activity, video).execute();
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Opens a list item: a video, or a channel/playlist row from {@link io.github.gohoski.notpipe.api.Discovery}.
     */
    public static void openItem(Activity activity, VideoInfo item) {
        android.content.Intent intent;
        if (item.id.startsWith(io.github.gohoski.notpipe.api.Discovery.CHANNEL_PREFIX)) {
            intent = new android.content.Intent(activity, io.github.gohoski.notpipe.ChannelActivity.class);
            intent.putExtra("ID", item.id.substring(io.github.gohoski.notpipe.api.Discovery.CHANNEL_PREFIX.length()));
        } else if (item.id.startsWith(io.github.gohoski.notpipe.api.Discovery.PLAYLIST_PREFIX)) {
            io.github.gohoski.notpipe.VideoListActivity.open(activity, io.github.gohoski.notpipe.VideoListActivity.SOURCE_PLAYLIST,
                    item.id.substring(io.github.gohoski.notpipe.api.Discovery.PLAYLIST_PREFIX.length()), item.title);
            return;
        } else {
            io.github.gohoski.notpipe.util.ImageLoader.clearCache();
            System.gc();
            intent = new android.content.Intent(activity, io.github.gohoski.notpipe.VideoActivity.class);
            intent.putExtra("ID", item.id);
        }
        activity.startActivity(intent);
    }

    /**
     * Like: always saved to the local "Liked videos" playlist, and sent to YouTube too when signed in.
     * Dislike only exists on YouTube, so it needs the account.
     */
    public static void like(final Activity activity, final VideoInfo video, final boolean like) {
        boolean signedIn = Yt2009Account.getInstance().isSignedIn();
        if (like) {
            LocalLibrary.getInstance().addToPlaylist(activity.getString(R.string.liked_videos), video);
            if (!signedIn) {
                Toast.makeText(activity, R.string.saved_to_liked, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        runAccountAction(activity, new AccountAction() {
            public void run(Yt2009Account account) throws Exception {
                account.rate(video.id, like);
            }
        }, like ? R.string.liked : R.string.disliked);
    }

    public static void share(Activity activity, VideoInfo video) {
        try {
            activity.startActivity(android.content.Intent.createChooser(
                    new android.content.Intent(android.content.Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(android.content.Intent.EXTRA_TEXT, "https://youtu.be/" + video.id)
                            .putExtra(android.content.Intent.EXTRA_SUBJECT, video.title),
                    activity.getString(R.string.share)));
        } catch (Exception ignored) {}
    }

    /**
     * Runs an account action in the background and toasts the result.
     */
    public static void runAccountAction(final Activity activity, final AccountAction action, final int successText) {
        if (!Yt2009Account.getInstance().isSignedIn()) {
            Toast.makeText(activity, R.string.sign_in_required, Toast.LENGTH_LONG).show();
            return;
        }
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    action.run(Yt2009Account.getInstance());
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Exception e) {
                Toast.makeText(activity, e == null ? activity.getString(successText)
                        : activity.getString(R.string.action_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    public interface AccountAction {
        void run(Yt2009Account account) throws Exception;
    }

    private static class AccountPlaylistsTask extends AsyncTask<Void, Void, List<Playlist>> {
        private final Activity activity;
        private final VideoInfo video;

        AccountPlaylistsTask(Activity activity, VideoInfo video) {
            this.activity = activity;
            this.video = video;
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(activity, R.string.loading_playlists, Toast.LENGTH_SHORT).show();
        }

        @Override
        protected List<Playlist> doInBackground(Void... params) {
            try {
                return Yt2009Account.getInstance().getPlaylists();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(final List<Playlist> playlists) {
            if (activity.isFinishing()) return;
            if (playlists == null) {
                Toast.makeText(activity, R.string.load_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            final String[] items = new String[playlists.size() + 2];
            items[0] = activity.getString(R.string.watch_later);
            items[1] = activity.getString(R.string.new_youtube_playlist);
            for (int i = 0; i < playlists.size(); i++) {
                items[i + 2] = playlists.get(i).title;
            }
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.youtube_account)
                    .setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 0) {
                                runAccountAction(activity, new AccountAction() {
                                    public void run(Yt2009Account account) throws Exception {
                                        account.addToWatchLater(video.id);
                                    }
                                }, R.string.saved_to_youtube);
                            } else if (which == 1) {
                                showNewYoutubePlaylistDialog(activity, video);
                            } else {
                                final Playlist playlist = playlists.get(which - 2);
                                runAccountAction(activity, new AccountAction() {
                                    public void run(Yt2009Account account) throws Exception {
                                        account.addToPlaylist(playlist.id, video.id);
                                    }
                                }, R.string.saved_to_youtube);
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    private static void showNewYoutubePlaylistDialog(final Activity activity, final VideoInfo video) {
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(15, 5, 15, 5);
        final EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setHint(R.string.playlist_name);
        layout.addView(input);
        final CheckBox isPrivate = new CheckBox(activity);
        isPrivate.setText(R.string.private_playlist);
        layout.addView(isPrivate);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.new_youtube_playlist)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String name = input.getText().toString().trim();
                        if (name.length() == 0) return;
                        final boolean priv = isPrivate.isChecked();
                        runAccountAction(activity, new AccountAction() {
                            public void run(Yt2009Account account) throws Exception {
                                account.createPlaylist(name, video == null ? null : video.id, priv);
                            }
                        }, R.string.saved_to_youtube);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Shows local playlists plus a "New playlist" entry and adds the video to the chosen one.
     */
    public static void showLocalPlaylistDialog(final Activity activity, final VideoInfo video) {
        final LocalLibrary library = LocalLibrary.getInstance();
        final List<String> names = library.getPlaylistNames();
        final String[] items = new String[names.size() + 1];
        items[0] = activity.getString(R.string.new_playlist);
        for (int i = 0; i < names.size(); i++) {
            items[i + 1] = names.get(i);
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.add_to_playlist)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            showNewPlaylistDialog(activity, new OnPlaylistCreated() {
                                @Override
                                public void onCreated(String name) {
                                    addTo(activity, name, video);
                                }
                            });
                        } else {
                            addTo(activity, names.get(which - 1), video);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public interface OnPlaylistCreated {
        void onCreated(String name);
    }

    public static void showNewPlaylistDialog(final Activity activity, final OnPlaylistCreated listener) {
        final EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setHint(R.string.playlist_name);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.new_playlist)
                .setView(input)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString().trim();
                        if (name.length() == 0) return;
                        if (!LocalLibrary.getInstance().createPlaylist(name)) {
                            Toast.makeText(activity, R.string.playlist_exists, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (listener != null) listener.onCreated(name);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static void addTo(Activity activity, String name, VideoInfo video) {
        boolean added = LocalLibrary.getInstance().addToPlaylist(name, video);
        Toast.makeText(activity, activity.getString(added ? R.string.added_to_playlist : R.string.already_in_playlist, name),
                Toast.LENGTH_SHORT).show();
    }
}
