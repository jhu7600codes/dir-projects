package io.github.gohoski.notpipe.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;

import io.github.gohoski.notpipe.DownloadService;
import io.github.gohoski.notpipe.R;
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
     * Shows local playlists plus a "New playlist" entry and adds the video to the chosen one.
     */
    public static void showAddToPlaylistDialog(final Activity activity, final VideoInfo video) {
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
