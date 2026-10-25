package io.github.gohoski.notpipe.api;

import java.io.IOException;

import io.github.gohoski.notpipe.data.Playlist;

/**
 * Created by Claude on 25.10.2026.
 */

public interface PlaylistApi {
    String getName(); String getHost();

    /**
     * @param id YouTube playlist ID (PL…, UU…, OL…, etc.)
     * @return Playlist with its videos
     */
    Playlist getPlaylist(String id) throws IOException;
}
