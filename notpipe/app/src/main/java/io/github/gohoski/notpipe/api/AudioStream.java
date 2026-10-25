package io.github.gohoski.notpipe.api;

import java.io.IOException;

/**
 * Created by Claude on 25.10.2026.
 * Instances that can return an audio-only stream (AAC in an MP4/M4A container)
 */

public interface AudioStream {
    String getName(); String getHost();

    String getAudioUrl(String id) throws IOException;
}
