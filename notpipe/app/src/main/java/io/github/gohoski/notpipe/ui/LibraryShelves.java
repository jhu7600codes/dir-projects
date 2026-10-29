package io.github.gohoski.notpipe.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import io.github.gohoski.notpipe.R;
import io.github.gohoski.notpipe.VideoListActivity;
import io.github.gohoski.notpipe.api.Yt2009Account;
import io.github.gohoski.notpipe.data.Playlist;
import io.github.gohoski.notpipe.data.VideoInfo;
import io.github.gohoski.notpipe.util.ImageLoader;
import io.github.gohoski.notpipe.util.LocalLibrary;

/**
 * Created by Claude on 28.10.2026.
 * Top of the Library page, laid out like YouTube's: your profile, a History row and a Playlists row
 * (local playlists, then the yt2009 account's, which show from cache first and refresh in the background).
 */

public class LibraryShelves {
    public interface Listener {
        void onSignIn();
        void onNewPlaylist();
        /** Something changed (e.g. a playlist was deleted) and the page should be rebuilt */
        void onChanged();
    }

    private static final int MAX_HISTORY = 15;
    private static final int CARD_WIDTH = 160, CARD_HEIGHT = 90;

    private final Activity activity;
    private final Listener listener;
    private final LinearLayout root;
    private List<Playlist> accountPlaylists;
    private boolean loadingAccount = false;
    private boolean sessionLost = false;
    private long lastAccountLoad = 0;
    private static final long ACCOUNT_REFRESH_MS = 60000;

    public LibraryShelves(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, 0, 0, dp(8));
    }

    public View getView() {
        return root;
    }

    /** Rebuilds the profile, History and Playlists rows; account data loads in the background */
    public void refresh() {
        root.removeAllViews();
        Yt2009Account account = Yt2009Account.getInstance();
        root.addView(profile(account));
        addHistory();
        addPlaylists(account);
        if (account.isSignedIn() && !loadingAccount
                && System.currentTimeMillis() - lastAccountLoad > ACCOUNT_REFRESH_MS) loadAccount();
    }

    private View profile(final Yt2009Account account) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(16), dp(16), dp(12));
        ImageView avatar = new ImageView(activity);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setBackgroundColor(0xFF333333);
        String avatarUrl = account.isSignedIn() ? account.getProfileAvatar() : "";
        if (avatarUrl.length() > 0) {
            ImageLoader.loadImage(avatarUrl, avatar, true);
        } else {
            avatar.setImageResource(R.drawable.main_icon);
        }
        row.addView(avatar, new LinearLayout.LayoutParams(dp(56), dp(56)));

        LinearLayout texts = new LinearLayout(activity);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(14), 0, 0, 0);
        TextView name = new TextView(activity);
        name.setTextColor(Color.WHITE);
        name.setTextSize(20);
        name.setTypeface(null, Typeface.BOLD);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        TextView sub = new TextView(activity);
        sub.setTextSize(13);
        sub.setTextColor(0xFF9A9A9A);
        if (account.isSignedIn()) {
            String profileName = account.getProfileName();
            name.setText(profileName == null || profileName.length() == 0
                    ? activity.getString(R.string.youtube_account) : profileName);
            String handle = account.getProfileHandle();
            sub.setText(handle.length() > 0 ? handle + " · " + account.getInstanceHost()
                    : activity.getString(R.string.signed_in_via, account.getInstanceHost()));
        } else {
            name.setText(R.string.not_signed_in);
            sub.setText(R.string.tap_to_sign_in);
            sub.setTextColor(0xFF33B5E5);
            row.setClickable(true);
            row.setBackgroundResource(R.drawable.tv_focus);
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    listener.onSignIn();
                }
            });
        }
        texts.addView(name);
        texts.addView(sub);
        row.addView(texts, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private void addHistory() {
        List<VideoInfo> history = LocalLibrary.getInstance().getHistory();
        if (history.isEmpty()) return;
        root.addView(sectionTitle(activity.getString(R.string.history), new View.OnClickListener() {
            public void onClick(View v) {
                VideoListActivity.open(activity, VideoListActivity.SOURCE_HISTORY, null, activity.getString(R.string.history));
            }
        }));
        LinearLayout row = shelfRow();
        for (int i = 0; i < history.size() && i < MAX_HISTORY; i++) {
            final VideoInfo video = history.get(i);
            View card = card(video.thumbnail, video.title, video.channel, video.duration, null);
            card.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    VideoActions.openItem(activity, video);
                }
            });
            row.addView(card);
        }
    }

    private void addPlaylists(final Yt2009Account account) {
        root.addView(sectionTitle(activity.getString(R.string.playlists), new View.OnClickListener() {
            public void onClick(View v) {
                showAllPlaylists();
            }
        }));
        LinearLayout row = shelfRow();
        final LocalLibrary library = LocalLibrary.getInstance();
        List<String> names = library.getPlaylistNames();
        for (int i = 0; i < names.size(); i++) {
            final String name = names.get(i);
            List<VideoInfo> videos = library.getPlaylistVideos(name);
            int size = videos.size();
            View card = card(videos.isEmpty() ? null : videos.get(0).thumbnail, name,
                    activity.getString(R.string.on_this_device), String.valueOf(size), null);
            card.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    VideoListActivity.open(activity, VideoListActivity.SOURCE_LOCAL, name, name);
                }
            });
            card.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    new AlertDialog.Builder(activity)
                            .setTitle(name)
                            .setMessage(R.string.delete_playlist_confirm)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    library.deletePlaylist(name);
                                    listener.onChanged();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null).show();
                    return true;
                }
            });
            row.addView(card);
        }
        if (account.isSignedIn()) {
            View watchLater = card(null, activity.getString(R.string.watch_later),
                    activity.getString(R.string.youtube_account), null, null);
            watchLater.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    VideoListActivity.open(activity, VideoListActivity.SOURCE_WATCH_LATER, null,
                            activity.getString(R.string.watch_later));
                }
            });
            row.addView(watchLater);
            List<Playlist> playlists = accountPlaylists != null ? accountPlaylists : account.getCachedPlaylists();
            for (int i = 0; i < playlists.size(); i++) {
                final Playlist p = playlists.get(i);
                View card = card(p.thumbnail, p.title, activity.getString(R.string.youtube_account),
                        p.videoCount >= 0 ? String.valueOf(p.videoCount) : null, null);
                card.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        VideoListActivity.open(activity, VideoListActivity.SOURCE_ACCOUNT_PLAYLIST, p.id, p.title);
                    }
                });
                row.addView(card);
            }
        }
        View create = card(null, activity.getString(R.string.new_playlist), null, null, "+");
        create.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                listener.onNewPlaylist();
            }
        });
        row.addView(create);
    }

    /** Every playlist in one list, for "View all" */
    private void showAllPlaylists() {
        final LocalLibrary library = LocalLibrary.getInstance();
        final List<String> names = library.getPlaylistNames();
        final List<Playlist> remote = Yt2009Account.getInstance().isSignedIn()
                ? (accountPlaylists != null ? accountPlaylists : Yt2009Account.getInstance().getCachedPlaylists())
                : new java.util.ArrayList<Playlist>();
        String[] items = new String[names.size() + remote.size()];
        for (int i = 0; i < names.size(); i++) items[i] = names.get(i) + " (" + library.getPlaylistSize(names.get(i)) + ")";
        for (int i = 0; i < remote.size(); i++) {
            Playlist p = remote.get(i);
            items[names.size() + i] = p.videoCount >= 0 ? p.title + " (" + p.videoCount + ")" : p.title;
        }
        new AlertDialog.Builder(activity).setTitle(R.string.playlists)
                .setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which < names.size()) {
                            VideoListActivity.open(activity, VideoListActivity.SOURCE_LOCAL, names.get(which), names.get(which));
                        } else {
                            Playlist p = remote.get(which - names.size());
                            VideoListActivity.open(activity, VideoListActivity.SOURCE_ACCOUNT_PLAYLIST, p.id, p.title);
                        }
                    }
                }).show();
    }

    /** Loads the profile and playlists in parallel-ish, then redraws */
    private void loadAccount() {
        loadingAccount = true;
        lastAccountLoad = System.currentTimeMillis();
        new AsyncTask<Void, Void, List<Playlist>>() {
            @Override
            protected List<Playlist> doInBackground(Void... params) {
                final Yt2009Account account = Yt2009Account.getInstance();
                try {
                    // A forgotten login makes every other request hang, so check it first; this also
                    // marks the account as signed out so the page offers signing in again
                    if (!account.checkLinked()) {
                        sessionLost = true;
                        return null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Thread profile = new Thread(new Runnable() {
                    public void run() {
                        try {
                            account.loadProfile();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                profile.start();
                List<Playlist> playlists = null;
                try {
                    playlists = account.getPlaylists();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    profile.join(35000);
                } catch (InterruptedException ignored) {}
                return playlists;
            }

            @Override
            protected void onPostExecute(List<Playlist> playlists) {
                if (activity.isFinishing()) return;
                if (sessionLost) {
                    sessionLost = false;
                    android.widget.Toast.makeText(activity, R.string.session_lost, android.widget.Toast.LENGTH_LONG).show();
                    loadingAccount = false;
                    listener.onChanged();
                    return;
                }
                if (playlists != null) accountPlaylists = playlists;
                refresh();
                loadingAccount = false;
            }
        }.execute();
    }

    private View sectionTitle(String text, View.OnClickListener viewAll) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(12), dp(8), dp(8));
        TextView title = new TextView(activity);
        title.setText(text);
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        row.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView all = new TextView(activity);
        all.setText(R.string.view_all);
        all.setTextColor(0xFF33B5E5);
        all.setTextSize(14);
        all.setPadding(dp(8), dp(6), dp(8), dp(6));
        all.setClickable(true);
        all.setBackgroundResource(R.drawable.tv_focus);
        all.setOnClickListener(viewAll);
        row.addView(all);
        return row;
    }

    private LinearLayout shelfRow() {
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(activity);
        row.setPadding(dp(12), 0, dp(12), 0);
        scroll.addView(row);
        root.addView(scroll);
        return row;
    }

    /**
     * @param badge text in the thumbnail's corner (duration or video count), may be null
     * @param placeholder big text shown instead of a thumbnail, may be null
     */
    private View card(String thumbnail, String title, String subtitle, String badge, String placeholder) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(CARD_WIDTH), LinearLayout.LayoutParams.WRAP_CONTENT);
        params.leftMargin = params.rightMargin = dp(4);
        card.setLayoutParams(params);
        card.setClickable(true);
        card.setBackgroundResource(R.drawable.tv_focus);

        FrameLayout frame = new FrameLayout(activity);
        frame.setBackgroundColor(0xFF2A2A2A);
        ImageView thumb = new ImageView(activity);
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(thumb, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
        if (thumbnail != null && thumbnail.length() > 0) {
            ImageLoader.loadImage(thumbnail, thumb, false);
        } else if (placeholder == null) {
            thumb.setImageResource(R.drawable.nav_library);
            thumb.setScaleType(ImageView.ScaleType.CENTER);
        }
        if (placeholder != null) {
            TextView big = new TextView(activity);
            big.setText(placeholder);
            big.setTextColor(Color.WHITE);
            big.setTextSize(36);
            big.setGravity(Gravity.CENTER);
            frame.addView(big, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
        }
        if (badge != null && badge.length() > 0) {
            TextView b = new TextView(activity);
            b.setText(badge);
            b.setTextColor(Color.WHITE);
            b.setTextSize(11);
            b.setBackgroundColor(0xCC000000);
            b.setPadding(dp(4), dp(1), dp(4), dp(1));
            FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.RIGHT);
            bp.rightMargin = bp.bottomMargin = dp(4);
            frame.addView(b, bp);
        }
        card.addView(frame, new LinearLayout.LayoutParams(dp(CARD_WIDTH), dp(CARD_HEIGHT)));

        TextView t = new TextView(activity);
        t.setText(title);
        t.setTextColor(Color.WHITE);
        t.setTextSize(13);
        t.setMaxLines(2);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setPadding(0, dp(6), 0, 0);
        card.addView(t);
        if (subtitle != null && subtitle.length() > 0) {
            TextView s = new TextView(activity);
            s.setText(subtitle);
            s.setTextColor(0xFF9A9A9A);
            s.setTextSize(12);
            s.setSingleLine(true);
            s.setEllipsize(TextUtils.TruncateAt.END);
            card.addView(s);
        }
        return card;
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, activity.getResources().getDisplayMetrics());
    }
}
