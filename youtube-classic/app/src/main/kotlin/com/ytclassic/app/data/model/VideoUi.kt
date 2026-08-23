package com.ytclassic.app.data.model

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
 * [com.ytclassic.app.network.InnertubeFeedClient]), so both paths end up
 * feeding the same [com.ytclassic.app.ui.common.VideoListAdapter].
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
        metadataParts.add(com.ytclassic.app.util.Formatters.viewCount(viewCount))
    }
    val uploadText = textualUploadDate
        ?: uploadDate?.offsetDateTime()?.let {
            com.ytclassic.app.util.Formatters.relativeTime(it.toEpochSecond())
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
            else if (duration > 0) com.ytclassic.app.util.Formatters.duration(duration) else null,
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
    val shortMarker = "youtu.be/"
    val shortIdx = url.indexOf(shortMarker)
    if (shortIdx >= 0) {
        return url.substring(shortIdx + shortMarker.length)
    }
    return url
}
