package com.maytube.app.browse

import android.content.Context

/**
 * A rolling list of recently watched video ids, kept purely so
 * Yt2009Api.fetchHome can feed the same `ids` signal into
 * /yt2009_recommended that yt2009's own homepage JS sends from its
 * cookie/localStorage-tracked watch history (see
 * assets/site-assets/homepage-recommended.js) -- without this, that
 * endpoint has nothing to personalize against and returns "YT2009_NO_DATA".
 */
class WatchHistory(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordWatched(videoId: String) {
        val current = getRecent().toMutableList()
        current.remove(videoId)
        current.add(0, videoId)
        while (current.size > MAX_HISTORY) current.removeAt(current.size - 1)
        prefs.edit().putString(KEY_IDS, current.joinToString(",")).apply()
    }

    fun getRecent(): List<String> =
        prefs.getString(KEY_IDS, null)?.split(",")?.filter { it.isNotBlank() }.orEmpty()

    companion object {
        private const val PREFS_NAME = "maytube_watch_history"
        private const val KEY_IDS = "recent_ids"
        private const val MAX_HISTORY = 25
    }
}
