package com.ytclassic.app.util

import org.json.JSONArray
import org.json.JSONObject

/**
 * Innertube responses are a deeply, inconsistently nested tree that YouTube
 * reshapes constantly (renderers wrapped in more renderers wrapped in
 * continuation items...). Rather than hand-modeling the exact path to any
 * one field - which breaks the moment YouTube ships an unrelated redesign -
 * these walk the whole tree looking for objects by key, which is the same
 * trick yt-dlp's innertube extractor and most from-scratch clients use.
 */
object JsonWalk {

    /** Every JSON object anywhere in the tree that has a property named [key]. */
    fun findAllObjectsWithKey(root: Any?, key: String): List<JSONObject> {
        val results = mutableListOf<JSONObject>()
        walk(root) { node ->
            if (node is JSONObject && node.has(key)) {
                val value = node.opt(key)
                if (value is JSONObject) results.add(value)
            }
        }
        return results
    }

    /** The first string value found anywhere in the tree under property [key]. */
    fun findFirstString(root: Any?, key: String): String? {
        var found: String? = null
        walk(root) { node ->
            if (found == null && node is JSONObject && node.has(key)) {
                val value = node.opt(key)
                if (value is String) found = value
            }
        }
        return found
    }

    private fun walk(node: Any?, visit: (Any) -> Unit) {
        when (node) {
            is JSONObject -> {
                visit(node)
                for (key in node.keys()) {
                    walk(node.opt(key), visit)
                }
            }
            is JSONArray -> {
                visit(node)
                for (i in 0 until node.length()) {
                    walk(node.opt(i), visit)
                }
            }
        }
    }
}
