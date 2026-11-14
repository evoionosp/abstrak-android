package org.evoionosp.abstrak.data.download.file
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

class DownloadFileStore
    @Inject
    constructor(
        @Named("downloadRoot") private val rootDir: File,
    ) {
        fun bookDir(audiobookId: String): File = File(rootDir, sanitized(audiobookId))

        fun trackFile(
            audiobookId: String,
            trackIndex: Int,
            extension: String,
        ): File = File(bookDir(audiobookId), "$TRACK_PREFIX$trackIndex.$extension")

        fun tempFile(
            audiobookId: String,
            trackIndex: Int,
        ): File = File(bookDir(audiobookId), "$TRACK_PREFIX$trackIndex.$TEMP_EXTENSION")

        fun extensionFor(
            mimeType: String?,
            remoteUrl: String,
        ): String {
            mimeType
                ?.lowercase(Locale.US)
                ?.substringBefore(';')
                ?.trim()
                ?.let { MIME_EXTENSIONS[it] }
                ?.let { return it }
            val fileName = remoteUrl.substringBefore('?').substringAfterLast('/')
            val urlExt = fileName.substringAfterLast('.', "").lowercase(Locale.US)
            if (urlExt.length in 2..4 && urlExt.all { it.isLetterOrDigit() }) return urlExt
            return FALLBACK_EXTENSION
        }

        fun publishTempFile(
            tempFile: File,
            finalFile: File,
        ) {
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }
            finalFile.parentFile
                ?.listFiles { file ->
                    file != finalFile && file.name.startsWith(finalFile.nameWithoutExtension + ".")
                }.orEmpty()
                .forEach { it.delete() }
        }

        fun deleteTempFile(
            audiobookId: String,
            trackIndex: Int,
        ) {
            tempFile(audiobookId, trackIndex).delete()
        }

        fun deleteTrackFiles(
            audiobookId: String,
            trackIndex: Int,
        ) {
            bookDir(audiobookId)
                .listFiles { file -> file.name.startsWith("$TRACK_PREFIX$trackIndex.") }
                .orEmpty()
                .forEach { it.delete() }
        }

        fun deleteOrphanedFiles(
            referencedPaths: Set<String>,
            cutoffMillis: Long,
        ): Int {
            var deleted = 0
            rootDir.listFiles().orEmpty().forEach { bookDir ->
                if (!bookDir.isDirectory) return@forEach
                bookDir.listFiles().orEmpty().forEach { file ->
                    if (
                        file.isFile &&
                        file.absolutePath !in referencedPaths &&
                        file.lastModified() < cutoffMillis &&
                        file.delete()
                    ) {
                        deleted++
                    }
                }
                if (bookDir.listFiles().isNullOrEmpty()) bookDir.delete()
            }
            return deleted
        }

        fun deleteBookFiles(audiobookId: String) {
            bookDir(audiobookId).deleteRecursively()
        }

        fun deleteAll() {
            rootDir.deleteRecursively()
        }

        private fun sanitized(audiobookId: String): String {
            require(
                audiobookId.isNotBlank() &&
                    audiobookId != "." &&
                    audiobookId != ".." &&
                    '/' !in audiobookId &&
                    '\\' !in audiobookId,
            ) {
                "Invalid audiobook id for download storage"
            }
            return audiobookId
        }

        private companion object {
            const val TRACK_PREFIX = "track_"
            const val TEMP_EXTENSION = "part"
            const val FALLBACK_EXTENSION = "bin"
            val MIME_EXTENSIONS =
                mapOf(
                    "audio/mpeg" to "mp3",
                    "audio/mp3" to "mp3",
                    "audio/mp4" to "m4a",
                    "audio/m4b" to "m4b",
                    "audio/x-m4b" to "m4b",
                    "audio/x-m4a" to "m4a",
                    "audio/aac" to "aac",
                    "audio/ogg" to "ogg",
                    "audio/opus" to "opus",
                    "audio/flac" to "flac",
                    "audio/x-flac" to "flac",
                    "audio/wav" to "wav",
                    "audio/x-wav" to "wav",
                    "audio/webm" to "webm",
                )
        }
    }
