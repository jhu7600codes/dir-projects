package io.github.gohoski.notpipe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.github.gohoski.notpipe.api.Yt2009Account;
import io.github.gohoski.notpipe.config.ConfigManager;
import io.github.gohoski.notpipe.data.Channel;
import io.github.gohoski.notpipe.data.Playlist;
import io.github.gohoski.notpipe.ui.VideoActions;
import io.github.gohoski.notpipe.util.LocalLibrary;

/**
 * Created by Claude on 25.10.2026.
 * The "Library" hub: background player, local subscriptions and playlists, downloads and the yt2009 YouTube account.
 */

public class LibraryActivity extends Activity {
    private static final int NOW_PLAYING = 0, FEED = 1, CHANNELS = 2, PLAYLIST = 3, NEW_PLAYLIST = 4,
            OPEN_PLAYLIST = 5, DOWNLOADS = 6, SIGN_IN = 7, WATCH_LATER = 9,
            ACCOUNT_SUBS = 10, ACCOUNT_PLAYLISTS = 11, SIGN_OUT = 12, HEADER = 13, HISTORY = 14, FOR_YOU = 15,
            SHORTS = 16, UPLOADS = 17, UPLOAD = 18, IMPORT_FILE = 19;
    private static final int PICK_SUBSCRIPTIONS_FILE = 1;

    private Context context;
    private final List<Row> rows = new ArrayList<Row>();
    private RowAdapter adapter;
    private boolean isDestroyedFlag = false;

    private static class Row {
        int kind;
        String text, subtext;
        String data;

        Row(int kind, String text, String subtext, String data) {
            this.kind = kind;
            this.text = text;
            this.subtext = subtext;
            this.data = data;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setTitle(R.string.library);
        context = this;

        ListView list = (ListView) findViewById(R.id.list);
        adapter = new RowAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onRowClick(rows.get(position));
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Row row = rows.get(position);
                if (row.kind != PLAYLIST) return false;
                new AlertDialog.Builder(context)
                        .setTitle(row.data)
                        .setMessage(R.string.delete_playlist_confirm)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                LocalLibrary.getInstance().deletePlaylist(row.data);
                                buildRows();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null).show();
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildRows();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_SUBSCRIPTIONS_FILE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        int added;
        try {
            java.io.InputStream in = getContentResolver().openInputStream(data.getData());
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            in.close();
            added = LocalLibrary.getInstance().importSubscriptionsFile(new String(out.toByteArray(), "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
            added = -1;
        }
        Toast.makeText(this, added < 0 ? getString(R.string.import_file_bad)
                : getString(R.string.subscriptions_imported, added), Toast.LENGTH_LONG).show();
        buildRows();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyedFlag = true;
    }

    private void buildRows() {
        rows.clear();
        LocalLibrary library = LocalLibrary.getInstance();

        if (MusicService.isActive()) {
            rows.add(new Row(HEADER, getString(R.string.background_player), null, null));
            rows.add(new Row(NOW_PLAYING, MusicService.getCurrentTitle(),
                    getString(MusicService.isPaused() ? R.string.paused : R.string.playing), null));
        }

        Yt2009Account account = Yt2009Account.getInstance();
        rows.add(new Row(HEADER, getString(R.string.discover), null, null));
        rows.add(new Row(FOR_YOU, getString(R.string.recommended_for_you), getString(account.isSignedIn()
                ? R.string.recommended_account : R.string.recommended_history), null));
        rows.add(new Row(SHORTS, getString(R.string.shorts), getString(R.string.shorts_), null));

        rows.add(new Row(HEADER, getString(R.string.subscriptions), null, null));
        int subs = library.getSubscriptions().size();
        rows.add(new Row(FEED, getString(R.string.subscriptions_feed), getResources().getQuantityString(R.plurals.channels_count, subs, subs), null));
        rows.add(new Row(CHANNELS, getString(R.string.channels), null, null));
        rows.add(new Row(IMPORT_FILE, getString(R.string.import_file), getString(R.string.import_file_), null));

        rows.add(new Row(HEADER, getString(R.string.playlists), null, null));
        List<String> names = library.getPlaylistNames();
        for (int i = 0; i < names.size(); i++) {
            rows.add(new Row(PLAYLIST, names.get(i), getResources().getQuantityString(R.plurals.video_count_short, library.getPlaylistSize(names.get(i)), library.getPlaylistSize(names.get(i))), names.get(i)));
        }
        rows.add(new Row(NEW_PLAYLIST, getString(R.string.new_playlist), null, null));
        rows.add(new Row(OPEN_PLAYLIST, getString(R.string.open_playlist), getString(R.string.open_playlist_), null));

        rows.add(new Row(HEADER, getString(R.string.on_this_device), null, null));
        rows.add(new Row(HISTORY, getString(R.string.history), null, null));
        rows.add(new Row(DOWNLOADS, getString(R.string.downloads), null, null));

        rows.add(new Row(HEADER, getString(R.string.youtube_account), null, null));
        if (account.isSignedIn()) {
            rows.add(new Row(WATCH_LATER, getString(R.string.watch_later), null, null));
            rows.add(new Row(ACCOUNT_PLAYLISTS, getString(R.string.your_playlists), null, null));
            rows.add(new Row(UPLOADS, getString(R.string.your_videos), null, null));
            rows.add(new Row(UPLOAD, getString(R.string.upload_video), getString(R.string.upload_video_), null));
            rows.add(new Row(ACCOUNT_SUBS, getString(R.string.import_subscriptions), getString(R.string.import_subscriptions_), null));
            rows.add(new Row(SIGN_OUT, getString(R.string.sign_out), getString(R.string.signed_in_via, account.getInstanceHost()), null));
        } else {
            rows.add(new Row(SIGN_IN, getString(R.string.sign_in_yt2009), getString(R.string.sign_in_yt2009_), null));
        }
        adapter.notifyDataSetChanged();
    }

    private void onRowClick(Row row) {
        switch (row.kind) {
            case NOW_PLAYING:
                showPlayerControls();
                break;
            case FEED:
                VideoListActivity.open(this, VideoListActivity.SOURCE_FEED, null, getString(R.string.subscriptions_feed));
                break;
            case IMPORT_FILE:
                Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                pick.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(pick, getString(R.string.import_file)), PICK_SUBSCRIPTIONS_FILE);
                } catch (Exception e) {
                    Toast.makeText(this, R.string.no_picker, Toast.LENGTH_SHORT).show();
                }
                break;
            case CHANNELS:
                showChannels();
                break;
            case PLAYLIST:
                VideoListActivity.open(this, VideoListActivity.SOURCE_LOCAL, row.data, row.data);
                break;
            case NEW_PLAYLIST:
                VideoActions.showNewPlaylistDialog(this, new VideoActions.OnPlaylistCreated() {
                    @Override
                    public void onCreated(String name) {
                        buildRows();
                    }
                });
                break;
            case OPEN_PLAYLIST:
                showOpenPlaylistDialog();
                break;
            case FOR_YOU:
                VideoListActivity.open(this, VideoListActivity.SOURCE_FOR_YOU, null, getString(R.string.recommended_for_you));
                break;
            case SHORTS:
                startActivity(new Intent(this, ShortsActivity.class));
                break;
            case UPLOADS:
                VideoListActivity.open(this, VideoListActivity.SOURCE_UPLOADS, null, getString(R.string.your_videos));
                break;
            case UPLOAD:
                startActivity(new Intent(this, UploadActivity.class));
                break;
            case HISTORY:
                VideoListActivity.open(this, VideoListActivity.SOURCE_HISTORY, null, getString(R.string.history));
                break;
            case DOWNLOADS:
                startActivity(new Intent(this, DownloadsActivity.class));
                break;
            case SIGN_IN:
                showSignIn();
                break;
            case WATCH_LATER:
                VideoListActivity.open(this, VideoListActivity.SOURCE_WATCH_LATER, null, getString(R.string.watch_later));
                break;
            case ACCOUNT_PLAYLISTS:
                new AccountPlaylistsTask().execute();
                break;
            case ACCOUNT_SUBS:
                new ImportSubscriptionsTask().execute();
                break;
            case SIGN_OUT:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.sign_out)
                        .setMessage(R.string.sign_out_confirm)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Yt2009Account.getInstance().signOut();
                                buildRows();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null).show();
                break;
        }
    }

    private void showPlayerControls() {
        if (!MusicService.isActive()) {
            buildRows();
            return;
        }
        String[] items = new String[]{
                getString(MusicService.isPaused() ? R.string.resume : R.string.pause),
                getString(R.string.previous), getString(R.string.next), getString(R.string.stop),
                getString(R.string.open_video)
        };
        new AlertDialog.Builder(this)
                .setTitle(MusicService.getCurrentTitle())
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) MusicService.sendAction(context, MusicService.ACTION_TOGGLE);
                        else if (which == 1) MusicService.sendAction(context, MusicService.ACTION_PREVIOUS);
                        else if (which == 2) MusicService.sendAction(context, MusicService.ACTION_NEXT);
                        else if (which == 3) MusicService.sendAction(context, MusicService.ACTION_STOP);
                        else {
                            String id = MusicService.getCurrentId();
                            if (id != null) {
                                Intent intent = new Intent(context, VideoActivity.class);
                                intent.putExtra("ID", id);
                                startActivity(intent);
                            }
                        }
                        listView().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!isDestroyedFlag) buildRows();
                            }
                        }, 500);
                    }
                }).show();
    }

    private ListView listView() {
        return (ListView) findViewById(R.id.list);
    }

    private void showChannels() {
        final List<Channel> channels = LocalLibrary.getInstance().getSubscriptions();
        if (channels.isEmpty()) {
            Toast.makeText(this, R.string.no_subscriptions, Toast.LENGTH_LONG).show();
            return;
        }
        String[] names = new String[channels.size()];
        for (int i = 0; i < channels.size(); i++) {
            names[i] = channels.get(i).title;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.channels)
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(context, ChannelActivity.class);
                        intent.putExtra("ID", channels.get(which).id);
                        startActivity(intent);
                    }
                }).show();
    }

    private void showOpenPlaylistDialog() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(R.string.open_playlist_);
        new AlertDialog.Builder(this)
                .setTitle(R.string.open_playlist)
                .setView(input)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String id = VideoListActivity.extractPlaylistId(input.getText().toString());
                        if (id == null) {
                            Toast.makeText(context, R.string.incorrect_playlist, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        VideoListActivity.open(context, VideoListActivity.SOURCE_PLAYLIST, id, getString(R.string.playlist));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    /* ---------- yt2009 sign in ---------- */

    private void showSignIn() {
        final List<String> instances = ConfigManager.getInstance().getConfig().getYt2009Instances();
        final String[] items = new String[instances.size() + 1];
        for (int i = 0; i < instances.size(); i++) {
            items[i] = instances.get(i).replace("https://", "").replace("http://", "");
        }
        items[instances.size()] = getString(R.string.url);
        new AlertDialog.Builder(this)
                .setTitle(R.string.select_ins)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which < instances.size()) {
                            confirmTrust(instances.get(which));
                        } else {
                            askInstanceUrl();
                        }
                    }
                }).show();
    }

    private void askInstanceUrl() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText("http://");
        new AlertDialog.Builder(this)
                .setTitle(R.string.url)
                .setView(input)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String url = input.getText().toString().trim();
                        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
                        if (!(url.startsWith("http://") || url.startsWith("https://")) || url.length() < 12) {
                            Toast.makeText(context, R.string.incorrect_url, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        confirmTrust(url);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private void confirmTrust(final String baseUrl) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.sign_in_yt2009)
                .setMessage(getString(R.string.sign_in_warning, baseUrl.replace("https://", "").replace("http://", "")))
                .setPositiveButton(R.string.continue_, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new StartSignInTask().execute(baseUrl);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    /** Google sign-in on this device (WebView) or yt2009's original PC page with the device code */
    private void chooseSignInPlace(final String baseUrl, final String code) {
        if (isDestroyedFlag) return;
        // Google's sign-in page needs a modern WebView (Android 5.0+), older devices only have the PC way
        if (NotPipe.SDK < 21) {
            showCodeDialog(baseUrl, code);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.sign_in_where)
                .setItems(new String[]{getString(R.string.sign_in_here), getString(R.string.sign_in_pc)},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    startActivity(new Intent(context, GsignActivity.class));
                                } else {
                                    showCodeDialog(baseUrl, code);
                                }
                            }
                        })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Yt2009Account.getInstance().signOut();
                    }
                }).show();
    }

    private void showCodeDialog(final String baseUrl, final String code) {
        if (isDestroyedFlag) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.sign_in_yt2009)
                .setMessage(getString(R.string.sign_in_steps, baseUrl + "/gsign", code))
                .setCancelable(false)
                .setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new CheckSignInTask(baseUrl, code).execute();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Yt2009Account.getInstance().signOut();
                    }
                }).show();
    }

    private void setLoading(boolean loading) {
        findViewById(R.id.loading).setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private class StartSignInTask extends AsyncTask<String, Void, String> {
        private String baseUrl;

        @Override
        protected void onPreExecute() {
            setLoading(true);
        }

        @Override
        protected String doInBackground(String... params) {
            baseUrl = params[0];
            try {
                return Yt2009Account.getInstance().startSignIn(baseUrl);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String code) {
            if (isDestroyedFlag) return;
            setLoading(false);
            if (code == null) {
                Toast.makeText(context, R.string.sign_in_failed, Toast.LENGTH_LONG).show();
                return;
            }
            chooseSignInPlace(baseUrl, code);
        }
    }

    private class CheckSignInTask extends AsyncTask<Void, Void, Boolean> {
        private final String baseUrl, code;

        CheckSignInTask(String baseUrl, String code) {
            this.baseUrl = baseUrl;
            this.code = code;
        }

        @Override
        protected void onPreExecute() {
            setLoading(true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                return Yt2009Account.getInstance().checkLinked();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Boolean linked) {
            if (isDestroyedFlag) return;
            setLoading(false);
            if (linked != null && linked) {
                Toast.makeText(context, R.string.signed_in, Toast.LENGTH_SHORT).show();
                buildRows();
            } else {
                Toast.makeText(context, linked == null ? R.string.sign_in_failed : R.string.not_signed_in_yet, Toast.LENGTH_LONG).show();
                showCodeDialog(baseUrl, code);
            }
        }
    }

    private class AccountPlaylistsTask extends AsyncTask<Void, Void, List<Playlist>> {
        @Override
        protected void onPreExecute() {
            setLoading(true);
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
            if (isDestroyedFlag) return;
            setLoading(false);
            if (playlists == null) {
                Toast.makeText(context, R.string.load_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            if (playlists.isEmpty()) {
                Toast.makeText(context, R.string.no_playlists, Toast.LENGTH_SHORT).show();
                return;
            }
            String[] names = new String[playlists.size()];
            for (int i = 0; i < playlists.size(); i++) {
                Playlist p = playlists.get(i);
                names[i] = p.videoCount >= 0 ? p.title + " (" + p.videoCount + ")" : p.title;
            }
            new AlertDialog.Builder(context)
                    .setTitle(R.string.your_playlists)
                    .setItems(names, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Playlist p = playlists.get(which);
                            VideoListActivity.open(context, VideoListActivity.SOURCE_ACCOUNT_PLAYLIST, p.id, p.title);
                        }
                    }).show();
        }
    }

    private class ImportSubscriptionsTask extends AsyncTask<Void, Void, Integer> {
        @Override
        protected void onPreExecute() {
            setLoading(true);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                List<Channel> channels = Yt2009Account.getInstance().getSubscriptions();
                return LocalLibrary.getInstance().importSubscriptions(channels);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Integer added) {
            if (isDestroyedFlag) return;
            setLoading(false);
            if (added == null) {
                Toast.makeText(context, R.string.load_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(context, getString(R.string.subscriptions_imported, added), Toast.LENGTH_SHORT).show();
            buildRows();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_library, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_open_playlist) {
            showOpenPlaylistDialog();
            return true;
        } else if (item.getItemId() == R.id.action_new_playlist) {
            onRowClick(new Row(NEW_PLAYLIST, null, null, null));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class RowAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return rows.size();
        }

        @Override
        public Object getItem(int position) {
            return rows.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return rows.get(position).kind != HEADER;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return rows.get(position).kind == HEADER ? 0 : 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Row row = rows.get(position);
            if (row.kind == HEADER) {
                // Same section header style as system lists: the blue Holo divider on 3.0+, the grey bar before
                TextView header = convertView instanceof TextView ? (TextView) convertView
                        : new TextView(context, null, android.R.attr.listSeparatorTextViewStyle);
                header.setText(row.text);
                return header;
            }
            View item = convertView != null ? convertView
                    : getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
            ((TextView) item.findViewById(android.R.id.text1)).setText(row.text);
            TextView sub = (TextView) item.findViewById(android.R.id.text2);
            sub.setText(row.subtext);
            sub.setVisibility(row.subtext == null ? View.GONE : View.VISIBLE);
            return item;
        }
    }
}
