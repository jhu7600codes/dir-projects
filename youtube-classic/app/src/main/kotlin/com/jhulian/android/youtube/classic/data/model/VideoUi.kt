package com.jhulian.android.youtube.classic.data.model

import org.json.JSONObject
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

/**
 * One row in a video list (home/trending/subscriptions/search/channel).
 * Built either from a NewPipeExtractor [StreamInfoItem] (search, trending,
 * channel tabs - everything that doesn't need a signed-in session) or from
 * a raw innertube `videoRenderer` JSON blob (home/subscriptions feeds,
 * which only exist behind the cookie session - see
 * [com.jhulian.android.youtube.classic.network.InnertubeFeedClient]), so both paths end up
 * feeding the same [com.jhulian.android.youtube.classic.ui.common.VideoListAdapter].
 */
data class VideoUi(
    val videoId: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String?,
    val channelName: String,
    val channelUrl: String?,
    val channelAvatarUrl: String?,
    val durationText: String?,
    val isLive: Boolean,
    val metadataLine: String,
)

fun List<Image>?.bestUrl(): String? = this?.maxByOrNull { it.height }?.url

fun StreamInfoItem.toVideoUi(): VideoUi {
    val metadataParts = mutableListOf<String>()
    if (viewCount >= 0) {
        metadataParts.add(com.jhulian.android.youtube.classic.util.Formatters.viewCount(viewCount))
    }
    val uploadText = textualUploadDate
        ?: uploadDate?.offsetDateTime()?.let {
            com.jhulian.android.youtube.classic.util.Formatters.relativeTime(it.toEpochSecond())
        }
    if (!uploadText.isNullOrBlank()) metadataParts.add(uploadText)

    return VideoUi(
        videoId = extractVideoId(url),
        url = url,
        title = name.orEmpty(),
        thumbnailUrl = thumbnails.bestUrl(),
        channelName = uploaderName.orEmpty(),
        channelUrl = uploaderUrl,
        channelAvatarUrl = uploaderAvatars.bestUrl(),
        durationText = if (streamType == StreamType.LIVE_STREAM) null
            else if (duration > 0) com.jhulian.android.youtube.classic.util.Formatters.duration(duration) else null,
        isLive = streamType == StreamType.LIVE_STREAM || streamType == StreamType.AUDIO_LIVE_STREAM,
        metadataLine = metadataParts.joinToString("  •  "),
    )
}

/**
 * Home/subscriptions feed item from a raw innertube `videoRenderer`.
 * Deliberately tolerant of missing fields - the JSON shape drifts as
 * YouTube ships changes, and a missing subtitle shouldn't drop the whole row.
 */
fun videoRendererToVideoUi(renderer: JSONObject): VideoUi? {
    val videoId = renderer.optString("videoId").takeIf { it.isNotBlank() } ?: return null
    val title = renderer.optJSONObject("title")
        ?.optJSONArray("runs")
        ?.optJSONObject(0)
        ?.optString("text")
        ?: renderer.optJSONObject("title")?.optString("simpleText")
        ?: return null

    val thumbnailUrl = renderer.optJSONObject("thumbnail")
        ?.optJSONArray("thumbnails")
        ?.let { arr -> (0 until arr.length()).map { arr.getJSONObject(it) } }
        ?.maxByOrNull { it.optInt("height") }
        ?.optString("url")

    val channelName = renderer.optJSONObject("longBylineText")?.textOrNull()
        ?: renderer.optJSONObject("shortBylineText")?.textOrNull()
        ?: renderer.optJSONObject("ownerText")?.textOrNull()
        ?: ""

    val channelUrlSuffix = (renderer.optJSONObject("longBylineText") ?: renderer.optJSONObject("shortBylineText"))
        ?.optJSONArray("runs")?.optJSONObject(0)
        ?.optJSONObject("navigationEndpoint")
        ?.optJSONObject("browseEndpoint")
        ?.optString("canonicalBaseUrl")

    val duration = renderer.optJSONObject("lengthText")?.textOrNull()
    val isLive = renderer.optJSONArray("badges")?.let { badges ->
        (0 until badges.length()).any { i ->
            badges.getJSONObject(i).optJSONObject("metadataBadgeRenderer")
                ?.optString("style")?.contains("LIVE") == true
        }
    } ?: false

    val views = renderer.optJSONObject("shortViewCountText")?.textOrNull()
        ?: renderer.optJSONObject("viewCountText")?.textOrNull()
    val published = renderer.optJSONObject("publishedTimeText")?.textOrNull()
    val metadata = listOfNotNull(views, published).joinToString("  •  ")

    return VideoUi(
        videoId = videoId,
        url = "https://www.youtube.com/watch?v=$videoId",
        title = title,
        thumbnailUrl = thumbnailUrl,
        channelName = channelName,
        channelUrl = channelUrlSuffix?.let { "https://www.youtube.com$it" },
        channelAvatarUrl = null,
        durationText = duration,
        isLive = isLive,
        metadataLine = metadata,
    )
}

/**
 * Shorts entry from a `reelItemRenderer` (the renderer YouTube uses for
 * Shorts shelves in Home/search). Shorts are just regular videos under
 * `/shorts/{id}` rather than `/watch?v={id}` - NewPipeExtractor's
 * `StreamInfo.getInfo()` already knows that URL shape, so once one of these
 * makes it into a [VideoUi] it plays back through the exact same
 * [com.jhulian.android.youtube.classic.ui.player.PlayerActivity] path as
 * any other video; there's no separate vertical-swipe Shorts viewer here,
 * just "Shorts show up in your feed and are watchable" rather than being
 * silently dropped.
 */
fun reelItemRendererToVideoUi(renderer: JSONObject): VideoUi? {
    val videoId = renderer.optString("videoId").takeIf { it.isNotBlank() } ?: return null
    val title = renderer.optJSONObject("headline")?.textOrNull()
        ?: renderer.optJSONObject("accessibilityText")?.textOrNull()
        ?: "Short"

    val thumbnailUrl = renderer.optJSONObject("thumbnail")
        ?.optJSONArray("thumbnails")
        ?.let { arr -> (0 until arr.length()).map { arr.getJSONObject(it) } }
        ?.maxByOrNull { it.optInt("height") }
        ?.optString("url")

    val views = renderer.optJSONObject("viewCountText")?.textOrNull()
        ?: renderer.optJSONObject("accessibilityText")?.textOrNull()

    return VideoUi(
        videoId = videoId,
        url = "https://www.youtube.com/shorts/$videoId",
        title = title,
        thumbnailUrl = thumbnailUrl,
        channelName = "",
        channelUrl = null,
        channelAvatarUrl = null,
        durationText = "Short",
        isLive = false,
        metadataLine = views.orEmpty(),
    )
}

private fun JSONObject.textOrNull(): String? {
    optString("simpleText").takeIf { it.isNotBlank() }?.let { return it }
    val runs = optJSONArray("runs") ?: return null
    val builder = StringBuilder()
    for (i in 0 until runs.length()) {
        builder.append(runs.getJSONObject(i).optString("text"))
    }
    return builder.toString().takeIf { it.isNotBlank() }
}

private fun extractVideoId(url: String): String {
    val marker = "v="
    val idx = url.indexOf(marker)
    if (idx >= 0) {
        val start = idx + marker.length
        val end = url.indexOf('&', start)
        return if (end == -1) url.substring(start) else url.substring(start, end)
    }
    // youtu.be/<id> short links.
    val shortLinkMarker = "youtu.be/"
    val shortLinkIdx = url.indexOf(shortLinkMarker)
    if (shortLinkIdx >= 0) {
        return url.substring(shortLinkIdx + shortLinkMarker.length)
    }
    // /shorts/<id> - Shorts are regular videos under a different URL path;
    // NewPipeExtractor's StreamInfo.getInfo() plays them the same way.
    val shortsMarker = "/shorts/"
    val shortsIdx = url.indexOf(shortsMarker)
    if (shortsIdx >= 0) {
        val start = shortsIdx + shortsMarker.length
        val end = url.indexOf('?', start).let { if (it == -1) url.length else it }
        return url.substring(start, end)
    }
    return url
}
