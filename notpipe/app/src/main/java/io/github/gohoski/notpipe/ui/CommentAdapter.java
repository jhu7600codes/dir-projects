package io.github.gohoski.notpipe.ui;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import io.github.gohoski.notpipe.R;
import io.github.gohoski.notpipe.Utils;
import io.github.gohoski.notpipe.data.Comment;
import io.github.gohoski.notpipe.util.ChannelIconResolver;
import io.github.gohoski.notpipe.util.ImageLoader;

/**
 * Adapter for comments with lazy loading support.
 * Only loads images for items that have been explicitly marked as visible.
 */
public class CommentAdapter extends ArrayAdapter<Comment> {
    private Context context;

    public CommentAdapter(Context context, int resource, List<Comment> objects) {
        super(context, resource, objects);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        View row = convertView;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(R.layout.item_comment, parent, false);
            holder = new ViewHolder();
            holder.channelTitle = (TextView) row.findViewById(R.id.channel_title);
            holder.content = (TextView) row.findViewById(R.id.content);
            holder.date = (TextView) row.findViewById(R.id.date);
            holder.channelThumb = (ImageView) row.findViewById(R.id.channel_thumbnail);
            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }

        Comment comment = getItem(position);
        holder.channelTitle.setText(comment.channel);
        holder.content.setText(comment.content);
        String date = Utils.formatTimeAgo(context, comment.publishedAt);
        if (comment.replyCount > 0) {
            date += " · " + context.getResources().getQuantityString(R.plurals.replies, comment.replyCount, comment.replyCount);
        }
        holder.date.setText(date);

        // Prepare holder for lazy loading
        holder.comment = comment;
        holder.imagesLoaded = false;

        // Reset thumbnail to prevent recycled view artifacts
        holder.channelThumb.setImageBitmap(null);
        if (!(parent instanceof AdapterLinearLayout)) {
            loadAvatar(comment, holder.channelThumb);
            holder.imagesLoaded = true;
        }

        return row;
    }

    /**
     * Called by VideoActivity when this row enters the screen.
     */
    public void loadImagesForView(View row) {
        if (row.getTag() instanceof ViewHolder) {
            ViewHolder holder = (ViewHolder) row.getTag();
            if (!holder.imagesLoaded && holder.comment != null) {
                loadAvatar(holder.comment, holder.channelThumb);
                holder.imagesLoaded = true;
            }
        }
    }

    /**
     * Instance image proxies often refuse avatars, so this falls back to the direct YouTube URL
     * and then to looking the channel's icon up by its ID.
     */
    private static void loadAvatar(final Comment comment, final ImageView view) {
        String url = comment.channelThumbnail;
        if (url == null || url.length() == 0) {
            resolveAvatar(comment, view);
            return;
        }
        ImageLoader.loadImage(url, view, true, new ImageLoader.ImageLoaderCallback() {
            public void onFail() {
                String url = comment.channelThumbnail;
                int proxy = url.indexOf("/ggpht/");
                if (proxy >= 0) {
                    ImageLoader.loadImage("https://yt3.ggpht.com" + url.substring(proxy + 6), view, true,
                            new ImageLoader.ImageLoaderCallback() {
                                public void onFail() {
                                    resolveAvatar(comment, view);
                                }
                            });
                } else {
                    resolveAvatar(comment, view);
                }
            }
        });
    }

    private static void resolveAvatar(final Comment comment, final ImageView view) {
        if (comment.channelId == null || !comment.channelId.startsWith("UC")) return;
        String icon = ChannelIconResolver.getResolved(comment.channelId);
        if (icon != null) {
            if (!ChannelIconResolver.FAILED.equals(icon)) ImageLoader.loadImage(icon, view);
            return;
        }
        ChannelIconResolver.requestFallback(comment.channelId, new ChannelIconResolver.OnIconResolvedListener() {
            public void onIconResolved(String channelId, String url) {
                if (url != null && url.length() > 0 && !ChannelIconResolver.FAILED.equals(url)) {
                    ImageLoader.loadImage(url, view);
                }
            }
        });
    }

    private static class ViewHolder {
        TextView channelTitle;
        TextView content;
        TextView date;
        ImageView channelThumb;
        Comment comment;
        boolean imagesLoaded; // Tracks if we already triggered the load for this specific view
    }
}