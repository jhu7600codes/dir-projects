package com.jhulian.android.youtube.classic.network

import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

data class SponsorSegment(
    val category: String,
    val startSeconds: Double,
    val endSeconds: Double,
)

/**
 * Queries the public, crowd-sourced SponsorBlock database
 * (https://sponsor.ajay.app) for a video's skippable segments.
 *
 * Uses the privacy-preserving hash-prefix lookup rather than sending the
 * plain video ID: SHA-256 the video ID, send only the first [HASH_PREFIX_LEN]
 * hex characters, and the server returns every video sharing that prefix -
 * the client then keeps only the exact match locally. This is the same
 * k-anonymity scheme SponsorBlock's own browser extension uses, so the
 * server never learns which specific video a given device is watching.
 * See https://wiki.sponsor.ajay.app/w/API_Docs#GET_/api/skipSegments/:sha256HashPrefix
 */
object SponsorBlockClient {

    private const val BASE_URL = "https://sponsor.ajay.app/api"
    private const val HASH_PREFIX_LEN = 4

    private val client = OkHttpClient.Builder().build()

    suspend fun fetchSegments(videoId: String, categories: List<String>): List<SponsorSegment> =
        withContext(Dispatchers.IO) {
            val prefix = sha256Hex(videoId).take(HASH_PREFIX_LEN)
            val categoriesJson = JSONArray(categories).toString()
            val url = "$BASE_URL/skipSegments/$prefix" +
                "?categories=${java.net.URLEncoder.encode(categoriesJson, "UTF-8")}" +
                "&service=YouTube"

            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.code == 404) return@withContext emptyList()
                if (!response.isSuccessful) return@withContext emptyList()

                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@withContext emptyList()

                val results = JSONArray(body)
                val segments = mutableListOf<SponsorSegment>()
                for (i in 0 until results.length()) {
                    val entry = results.getJSONObject(i)
                    if (entry.optString("videoID") != videoId) continue
                    val segmentsArray = entry.optJSONArray("segments") ?: continue
                    for (j in 0 until segmentsArray.length()) {
                        val segmentObj = segmentsArray.getJSONObject(j)
                        val range = segmentObj.optJSONArray("segment") ?: continue
                        if (range.length() != 2) continue
                        segments.add(
                            SponsorSegment(
                                category = segmentObj.optString("category"),
                                startSeconds = range.getDouble(0),
                                endSeconds = range.getDouble(1),
                            ),
                        )
                    }
                }
                segments
            }
        }

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
