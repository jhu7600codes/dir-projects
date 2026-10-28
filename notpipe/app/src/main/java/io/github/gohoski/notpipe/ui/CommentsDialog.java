package io.github.gohoski.notpipe.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.github.gohoski.notpipe.R;
import io.github.gohoski.notpipe.api.Manager;
import io.github.gohoski.notpipe.api.Metadata;
import io.github.gohoski.notpipe.api.Yt2009Account;
import io.github.gohoski.notpipe.data.Comment;

/**
 * Created by Claude on 28.10.2026.
 * Comments in a dialog (used by Shorts), with replies and posting through the yt2009 account.
 */

public class CommentsDialog {
    private CommentsDialog() {}

    public static void show(final Activity activity, final String videoId) {
        int pad = dp(activity, 8);
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, pad, pad, 0);

        final List<Comment> comments = new ArrayList<Comment>();
        final CommentAdapter adapter = new CommentAdapter(activity, R.layout.item_comment, comments);

        View composer = composer(activity, videoId, new OnPosted() {
            public void onPosted(Comment comment) {
                comments.add(0, comment);
                adapter.notifyDataSetChanged();
            }
        });
        layout.addView(composer);

        final ProgressBar loading = new ProgressBar(activity);
        layout.addView(loading, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        final ListView list = new ListView(activity);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showReplies(activity, videoId, comments.get(position));
            }
        });
        layout.addView(list, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                activity.getResources().getDisplayMetrics().heightPixels / 2));

        new AlertDialog.Builder(activity).setTitle(R.string.comments).setView(layout)
                .setPositiveButton(android.R.string.ok, null).show();

        new AsyncTask<Void, Void, List<Comment>>() {
            @Override
            protected List<Comment> doInBackground(Void... params) {
                try {
                    return Manager.getInstance().raceMetadata(new Manager.MetadataTask<List<Comment>>() {
                        public List<Comment> run(Metadata m) throws Exception {
                            List<Comment> result = m.getComments(videoId);
                            if (result == null || result.isEmpty()) throw new java.io.IOException("No comments");
                            return result;
                        }
                    }, null);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<Comment> result) {
                loading.setVisibility(View.GONE);
                if (result == null) {
                    Toast.makeText(activity, R.string.no_comments, Toast.LENGTH_SHORT).show();
                    return;
                }
                comments.addAll(result);
                adapter.notifyDataSetChanged();
            }
        }.execute();
    }

    public interface OnPosted {
        void onPosted(Comment comment);
    }

    /**
     * Row with a text box and Send button, or a sign-in hint when not signed in through yt2009.
     */
    public static View composer(final Activity activity, final String videoId, final OnPosted listener) {
        if (!Yt2009Account.getInstance().isSignedIn()) {
            TextView hint = new TextView(activity);
            hint.setText(R.string.sign_in_to_comment);
            hint.setPadding(dp(activity, 4), dp(activity, 4), dp(activity, 4), dp(activity, 8));
            return hint;
        }
        LinearLayout row = new LinearLayout(activity);
        final EditText input = new EditText(activity);
        input.setHint(R.string.add_comment);
        input.setMaxLines(4);
        row.addView(input, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button send = new Button(activity);
        send.setText(R.string.send);
        row.addView(send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String text = input.getText().toString().trim();
                if (text.length() == 0) return;
                input.setText("");
                VideoActions.runAccountAction(activity, new VideoActions.AccountAction() {
                    public void run(Yt2009Account account) throws Exception {
                        account.postComment(videoId, text);
                    }
                }, R.string.comment_posted);
                if (listener != null) {
                    listener.onPosted(new Comment(activity.getString(R.string.you), "", text, new Date(), ""));
                }
            }
        });
        return row;
    }

    public static void showReplies(final Activity activity, final String videoId, final Comment comment) {
        if (comment.repliesToken == null) return;
        Toast.makeText(activity, R.string.loading_replies, Toast.LENGTH_SHORT).show();
        new AsyncTask<Void, Void, List<Comment>>() {
            @Override
            protected List<Comment> doInBackground(Void... params) {
                try {
                    return Manager.getInstance().getCommentReplies(videoId, comment.repliesToken);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<Comment> replies) {
                if (activity.isFinishing()) return;
                if (replies == null || replies.isEmpty()) {
                    Toast.makeText(activity, R.string.load_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < replies.size(); i++) {
                    if (i > 0) sb.append("\n\n");
                    sb.append(replies.get(i).channel).append(": ").append(replies.get(i).content);
                }
                TextView text = new TextView(activity);
                text.setText(sb.toString());
                text.setPadding(15, 15, 15, 15);
                ScrollView scroll = new ScrollView(activity);
                scroll.addView(text);
                new AlertDialog.Builder(activity).setTitle(comment.channel).setView(scroll)
                        .setPositiveButton(android.R.string.ok, null).show();
            }
        }.execute();
    }

    private static int dp(Activity activity, float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, activity.getResources().getDisplayMetrics());
    }
}
