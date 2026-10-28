package io.github.gohoski.notpipe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import io.github.gohoski.notpipe.data.VideoInfo;
import io.github.gohoski.notpipe.util.Compat;

/**
 * Created by Claude on 25.10.2026.
 * Lists files downloaded by DownloadService. Audio is played through the background player, video through an external player.
 */

public class DownloadsActivity extends Activity {
    private final List<File> files = new ArrayList<File>();
    private FileAdapter adapter;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        io.github.gohoski.notpipe.ui.NavBar.attach(this, io.github.gohoski.notpipe.ui.NavBar.NONE);
        setTitle(R.string.downloads);
        context = this;
        Compat.ensureStoragePermission(this);

        ListView list = (ListView) findViewById(R.id.list);
        adapter = new FileAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                open(files.get(position));
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showFileDialog(files.get(position));
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        files.clear();
        File[] all = DownloadService.getDownloadsDir().listFiles();
        if (all != null) {
            Arrays.sort(all, new Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    long diff = b.lastModified() - a.lastModified();
                    return diff > 0 ? 1 : (diff < 0 ? -1 : 0);
                }
            });
            for (int i = 0; i < all.length; i++) {
                if (all[i].isFile() && !all[i].getName().endsWith(".part")) files.add(all[i]);
            }
        }
        adapter.notifyDataSetChanged();
        TextView empty = (TextView) findViewById(R.id.empty);
        empty.setText(R.string.no_downloads);
        empty.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static boolean isAudio(File file) {
        return file.getName().endsWith(".m4a");
    }

    /** @return the video ID from "Title [id].ext", or null */
    private static String videoIdOf(File file) {
        String name = file.getName();
        int start = name.lastIndexOf('[');
        int end = name.lastIndexOf(']');
        return start >= 0 && end > start ? name.substring(start + 1, end) : null;
    }

    private static String titleOf(File file) {
        String name = file.getName();
        int bracket = name.lastIndexOf(" [");
        return bracket > 0 ? name.substring(0, bracket) : name;
    }

    private void open(File file) {
        String id = videoIdOf(file);
        if (isAudio(file) && id != null) {
            playInBackground(file);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), file.getName().endsWith(".3gp") ? "video/3gpp" : "video/mp4");
        try {
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.no_player, Toast.LENGTH_SHORT).show();
        }
    }

    private void playInBackground(File file) {
        String id = videoIdOf(file);
        if (id == null) return;
        List<VideoInfo> queue = new ArrayList<VideoInfo>();
        int start = 0;
        for (int i = 0; i < files.size(); i++) {
            String otherId = videoIdOf(files.get(i));
            if (otherId == null || !isAudio(files.get(i))) continue;
            if (files.get(i).equals(file)) start = queue.size();
            queue.add(new VideoInfo(otherId, titleOf(files.get(i)), "", "", "", "", "", -1, null));
        }
        if (queue.isEmpty()) {
            queue.add(new VideoInfo(id, titleOf(file), "", "", "", "", "", -1, null));
        }
        MusicService.play(this, queue, start);
        Toast.makeText(this, R.string.playing_in_background, Toast.LENGTH_SHORT).show();
    }

    private void showFileDialog(final File file) {
        final String id = videoIdOf(file);
        List<String> items = new ArrayList<String>();
        items.add(getString(R.string.play));
        if (id != null) {
            items.add(getString(R.string.play_in_background));
            items.add(getString(R.string.open_video));
        }
        items.add(getString(R.string.delete));
        final String[] array = items.toArray(new String[items.size()]);
        new AlertDialog.Builder(this)
                .setTitle(titleOf(file))
                .setItems(array, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String item = array[which];
                        if (item.equals(getString(R.string.play))) {
                            open(file);
                        } else if (item.equals(getString(R.string.play_in_background))) {
                            List<VideoInfo> single = new ArrayList<VideoInfo>();
                            single.add(new VideoInfo(id, titleOf(file), "", "", "", "", "", -1, null));
                            MusicService.play(context, single, 0);
                        } else if (item.equals(getString(R.string.open_video))) {
                            Intent intent = new Intent(context, VideoActivity.class);
                            intent.putExtra("ID", id);
                            startActivity(intent);
                        } else {
                            new AlertDialog.Builder(context)
                                    .setTitle(titleOf(file))
                                    .setMessage(R.string.delete_file_confirm)
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface d, int w) {
                                            file.delete();
                                            refresh();
                                        }
                                    })
                                    .setNegativeButton(android.R.string.cancel, null).show();
                        }
                    }
                }).show();
    }

    private class FileAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return files.size();
        }

        @Override
        public Object getItem(int position) {
            return files.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            File file = files.get(position);
            View item = convertView != null ? convertView
                    : getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
            ((TextView) item.findViewById(android.R.id.text1)).setText(titleOf(file));
            ((TextView) item.findViewById(android.R.id.text2)).setText(getString(isAudio(file) ? R.string.audio : R.string.video)
                    + " · " + String.format("%.1f MB", file.length() / 1048576.0));
            return item;
        }
    }
}
