package io.github.gohoski.notpipe.api;

import java.io.IOException;
import java.util.List;

import io.github.gohoski.notpipe.data.VideoInfo;

/**
 * Created by Claude on 25.10.2026.
 */

public interface ShortsApi {
    String getName(); String getHost();

    /**
     * @param id Channel ID (UC…)
     */
    List<VideoInfo> getChannelShorts(String id) throws IOException;
}
