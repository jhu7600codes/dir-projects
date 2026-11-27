package app.lineagifier.catalog

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Download(
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val url: String,
    val fallbackUrl: String? = null,
    val sha256: String,
    val size: Long,
)

@Serializable
data class Alternative(
    val name: String,
    val packageId: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val url: String,
    val fallbackUrl: String? = null,
    val sha256: String,
    val size: Long,
) {
    fun download() = Download(versionName, versionCode, minSdk, url, fallbackUrl, sha256, size)
}

@Serializable
enum class Status { prebuilt, build_from_source, same_as_stock, unsupported }

@Serializable
data class Entry(
    val id: String,
    val category: String,
    val name: String,
    val packageId: String,
    val status: Status,
    val source: String,
    val notes: String,
    val role: String? = null,
    val replaces: List<String> = emptyList(),
    val download: Download? = null,
    val alternatives: List<Alternative> = emptyList(),
) {
    /** every apk lineagifier can install for this slot: the lineage app itself if it's prebuilt, then alternatives */
    fun installables(): List<Installable> = buildList {
        if (status == Status.prebuilt && download != null) add(Installable(id, name, packageId, download, isLineage = true))
        alternatives.forEach { add(Installable(id, it.name, it.packageId, it.download(), isLineage = false)) }
    }

    /** package ids that count as "the replacement is installed" (including a self-built lineage .dev build) */
    fun replacementIds(): List<String> = buildList {
        if (status != Status.same_as_stock && status != Status.unsupported) {
            add(packageId); add("$packageId.dev")
        }
        alternatives.forEach { add(it.packageId) }
    }.distinct().filter { it !in replaces }
}

data class Installable(
    val entryId: String,
    val name: String,
    val packageId: String,
    val download: Download,
    val isLineage: Boolean,
)

@Serializable
data class Catalog(
    val schema: Int,
    val verifiedOn: String,
    val verificationNotes: String = "",
    val entries: List<Entry>,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun parse(text: String): Catalog = json.decodeFromString(serializer(), text)
    }
}
