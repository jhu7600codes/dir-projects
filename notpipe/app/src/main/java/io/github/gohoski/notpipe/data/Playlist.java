package io.github.gohoski.notpipe.data;

import java.util.List;

/**
 * Created by Claude on 25.10.2026.
 * A YouTube or local playlist. Use -1 for videoCount if not available from API
 */

public class Playlist {
    public final String id, title, author, thumbnail;
    public final int videoCount;
    public final List<VideoInfo> videos;

    public Playlist(String id, String title, String author, String thumbnail, int videoCount, List<VideoInfo> videos) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.thumbnail = thumbnail;
        this.videoCount = videoCount;
        this.videos = videos;
    }

    @Override
    public String toString() {
        return "Playlist{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", videoCount=" + videoCount +
                '}';
    }
}
