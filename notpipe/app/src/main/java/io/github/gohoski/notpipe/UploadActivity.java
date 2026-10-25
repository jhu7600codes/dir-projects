package io.github.gohoski.notpipe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import io.github.gohoski.notpipe.api.Yt2009Account;

/**
 * Created by Claude on 25.10.2026.
 * Uploads a video from the device to the YouTube channel linked through yt2009.
 */

public class UploadActivity extends Activity {
    private static final int PICK_VIDEO = 1;
    private static final String[] PRIVACY_VALUES = {"public", "unlisted", "private"};

    private Uri videoUri;
    private Context context;
    private UploadTask uploadTask;
    private boolean isDestroyedFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);
        setTitle(R.string.upload_video);
        context = this;

        if (!Yt2009Account.getInstance().isSignedIn()) {
            Toast.makeText(this, R.string.sign_in_required, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Spinner privacy = (Spinner) findViewById(R.id.upload_privacy);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.privacy_options));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        privacy.setAdapter(adapter);

        findViewById(R.id.upload_pick).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                try {
                    startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_video)), PICK_VIDEO);
                } catch (Exception e) {
                    Toast.makeText(context, R.string.no_picker, Toast.LENGTH_SHORT).show();
                }
            }
        });
        findViewById(R.id.upload_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startUpload();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_VIDEO || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        videoUri = data.getData();
        String name = videoUri.getLastPathSegment();
        ((TextView) findViewById(R.id.upload_file)).setText(name == null ? videoUri.toString() : name);
        EditText title = (EditText) findViewById(R.id.upload_title);
        if (title.getText().length() == 0 && name != null && name.contains(".")) {
            title.setText(name.substring(0, name.lastIndexOf('.')));
        }
    }

    private void startUpload() {
        if (uploadTask != null) return;
        String title = ((EditText) findViewById(R.id.upload_title)).getText().toString().trim();
        if (videoUri == null) {
            Toast.makeText(this, R.string.no_file_chosen, Toast.LENGTH_SHORT).show();
            return;
        }
        if (title.length() == 0) {
            Toast.makeText(this, R.string.enter_title, Toast.LENGTH_SHORT).show();
            return;
        }
        uploadTask = new UploadTask();
        uploadTask.execute(title,
                ((EditText) findViewById(R.id.upload_description)).getText().toString(),
                ((EditText) findViewById(R.id.upload_tags)).getText().toString(),
                PRIVACY_VALUES[((Spinner) findViewById(R.id.upload_privacy)).getSelectedItemPosition()]);
    }

    private void askToken() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        new AlertDialog.Builder(this)
                .setTitle(R.string.instance_token)
                .setMessage(R.string.instance_token_)
                .setView(input)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Yt2009Account.getInstance().setInstanceToken(input.getText().toString().trim());
                        startUpload();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyedFlag = true;
    }

    private class UploadTask extends AsyncTask<String, Integer, String> {
        private Exception error;
        private File tempFile;

        @Override
        protected void onPreExecute() {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            ((Button) findViewById(R.id.upload_start)).setEnabled(false);
            ProgressBar progress = (ProgressBar) findViewById(R.id.upload_progress);
            progress.setProgress(0);
            progress.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.upload_status)).setText(R.string.uploading);
        }

        @Override
        protected String doInBackground(String... params) {
            InputStream in = null;
            try {
                long length = -1;
                if ("file".equals(videoUri.getScheme())) {
                    length = new File(videoUri.getPath()).length();
                } else {
                    try {
                        AssetFileDescriptor fd = getContentResolver().openAssetFileDescriptor(videoUri, "r");
                        length = fd.getLength();
                        fd.close();
                    } catch (Exception ignored) {}
                }
                in = getContentResolver().openInputStream(videoUri);
                if (length < 0) {
                    // Length is needed up front for a fixed-length upload, so copy the stream first
                    tempFile = new File(getCacheDir(), "upload.tmp");
                    OutputStream out = new FileOutputStream(tempFile);
                    byte[] buffer = new byte[16384];
                    int n;
                    while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                    out.close();
                    in.close();
                    length = tempFile.length();
                    in = new FileInputStream(tempFile);
                }
                String mime = getContentResolver().getType(videoUri);
                return Yt2009Account.getInstance().upload(in, length, mime, params[0], params[1], params[2], params[3],
                        new Yt2009Account.UploadProgressListener() {
                            private long lastUpdate = 0;

                            @Override
                            public void onProgress(long sent, long total) {
                                long now = System.currentTimeMillis();
                                if (now - lastUpdate > 500 && total > 0) {
                                    lastUpdate = now;
                                    publishProgress((int) (sent * 100 / total));
                                }
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
                error = e;
                return null;
            } finally {
                if (in != null) {
                    try { in.close(); } catch (Exception ignored) {}
                }
                if (tempFile != null) tempFile.delete();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            ((ProgressBar) findViewById(R.id.upload_progress)).setProgress(values[0]);
            ((TextView) findViewById(R.id.upload_status)).setText(getString(R.string.uploading_percent, values[0]));
        }

        @Override
        protected void onPostExecute(final String videoId) {
            uploadTask = null;
            if (isDestroyedFlag) return;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            ((Button) findViewById(R.id.upload_start)).setEnabled(true);
            findViewById(R.id.upload_progress).setVisibility(View.GONE);
            TextView status = (TextView) findViewById(R.id.upload_status);
            if (error instanceof Yt2009Account.TokenRequiredException) {
                status.setText("");
                askToken();
                return;
            }
            if (error != null) {
                status.setText(getString(R.string.upload_failed, error.getMessage()));
                return;
            }
            status.setText(R.string.uploaded);
            if (videoId != null) {
                new AlertDialog.Builder(context)
                        .setTitle(R.string.uploaded)
                        .setMessage(R.string.uploaded_)
                        .setPositiveButton(R.string.open_video, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(context, VideoActivity.class);
                                intent.putExtra("ID", videoId);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .setNegativeButton(android.R.string.ok, null).show();
            }
        }
    }
}
