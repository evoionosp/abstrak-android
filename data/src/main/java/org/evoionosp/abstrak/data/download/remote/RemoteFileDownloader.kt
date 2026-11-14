package org.evoionosp.abstrak.data.download.remote
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response

sealed interface DownloadOutcome {
    data class Success(
        val bytesWritten: Long,
        val totalBytes: Long?,
        val resumed: Boolean,
    ) : DownloadOutcome

    data class Failed(
        val error: DownloadError,
    ) : DownloadOutcome
}

sealed interface DownloadError {
    data object Auth : DownloadError

    data object Forbidden : DownloadError

    data class Http(
        val code: Int,
    ) : DownloadError

    data object Network : DownloadError
}

data class ResumeValidators(
    val etag: String?,
    val lastModified: String?,
)

class RemoteFileDownloader
    @Inject
    constructor(
        @Named("downloadHttpClient") private val httpClient: Call.Factory,
        @Named("io") private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend fun download(
            url: String,
            destFile: File,
            accessToken: String,
            resumeValidators: ResumeValidators? = null,
            onProgress: suspend (bytesDownloaded: Long, totalBytes: Long?) -> Unit,
            onHeaders: suspend (etag: String?, lastModified: String?) -> Unit = { _, _ -> },
        ): DownloadOutcome =
            withContext(ioDispatcher) {
                destFile.parentFile?.mkdirs()
                val existingLength = destFile.takeIf { it.exists() }?.length() ?: 0L
                val first =
                    fetch(
                        url = url,
                        destFile = destFile,
                        accessToken = accessToken,
                        resumeFrom = existingLength,
                        resumeValidators = resumeValidators,
                        onProgress = onProgress,
                        onHeaders = onHeaders,
                    )
                if (first != null) return@withContext first
                destFile.delete()
                fetch(
                    url = url,
                    destFile = destFile,
                    accessToken = accessToken,
                    resumeFrom = 0L,
                    resumeValidators = null,
                    onProgress = onProgress,
                    onHeaders = onHeaders,
                ) ?: DownloadOutcome.Failed(DownloadError.Network)
            }

        private suspend fun fetch(
            url: String,
            destFile: File,
            accessToken: String,
            resumeFrom: Long,
            resumeValidators: ResumeValidators?,
            onProgress: suspend (bytesDownloaded: Long, totalBytes: Long?) -> Unit,
            onHeaders: suspend (etag: String?, lastModified: String?) -> Unit,
        ): DownloadOutcome? {
            val request =
                try {
                    buildRequest(url, accessToken, resumeFrom, resumeValidators)
                } catch (exception: IllegalArgumentException) {
                    currentCoroutineContext().ensureActive()
                    return DownloadOutcome.Failed(DownloadError.Network)
                }
            val call = httpClient.newCall(request)
            val response =
                try {
                    executeCancellable(call)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: IOException) {
                    currentCoroutineContext().ensureActive()
                    return DownloadOutcome.Failed(DownloadError.Network)
                }
            return response.use { answered ->
                coroutineScope {
                    val watchdog =
                        launch(start = CoroutineStart.UNDISPATCHED) {
                            try {
                                awaitCancellation()
                            } finally {
                                runCatching { call.cancel() }
                            }
                        }
                    try {
                        if (answered.code == RANGE_NOT_SATISFIABLE && resumeFrom > 0) {
                            return@coroutineScope if (answered.rangeCompleteSize() == resumeFrom) {
                                onProgress(resumeFrom, resumeFrom)
                                DownloadOutcome.Success(resumeFrom, resumeFrom, resumed = true)
                            } else {
                                null
                            }
                        }
                        if (!answered.isSuccessful) {
                            return@coroutineScope DownloadOutcome.Failed(answered.toError())
                        }
                        onHeaders(answered.header(ETAG_HEADER), answered.header(LAST_MODIFIED_HEADER))
                        val resuming = answered.code == PARTIAL_CONTENT && resumeFrom > 0
                        if (resuming && answered.rangeStart() != resumeFrom) {
                            return@coroutineScope null
                        }
                        if (!resuming && resumeFrom > 0) {
                            destFile.delete()
                        }
                        val startOffset = if (resuming) resumeFrom else 0L
                        val total = answered.totalBytes(startOffset)
                        var written = startOffset
                        var lastEmitMillis = 0L
                        FileOutputStream(destFile, resuming).buffered().use { sink ->
                            answered.body.byteStream().use { source ->
                                val buffer = ByteArray(BUFFER_SIZE)
                                while (true) {
                                    currentCoroutineContext().ensureActive()
                                    val read = source.read(buffer)
                                    if (read == -1) break
                                    sink.write(buffer, 0, read)
                                    written += read
                                    val now = System.currentTimeMillis()
                                    if (now - lastEmitMillis >= PROGRESS_THROTTLE_MILLIS) {
                                        lastEmitMillis = now
                                        onProgress(written, total)
                                    }
                                }
                            }
                            sink.flush()
                        }
                        onProgress(written, total)
                        if (destFile.length() != written) {
                            destFile.delete()
                            return@coroutineScope DownloadOutcome.Failed(DownloadError.Network)
                        }
                        if (total != null && written != total) {
                            return@coroutineScope DownloadOutcome.Failed(DownloadError.Network)
                        }
                        return@coroutineScope DownloadOutcome.Success(written, total, resumed = resuming)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: IOException) {
                        currentCoroutineContext().ensureActive()
                        return@coroutineScope DownloadOutcome.Failed(DownloadError.Network)
                    } finally {
                        watchdog.cancel()
                    }
                }
            }
        }

        private fun buildRequest(
            url: String,
            accessToken: String,
            resumeFrom: Long,
            resumeValidators: ResumeValidators?,
        ): Request =
            Request
                .Builder()
                .url(url)
                .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$accessToken")
                .header(ACCEPT_ENCODING_HEADER, IDENTITY_ENCODING)
                .apply {
                    if (resumeFrom > 0) {
                        header(RANGE_HEADER, "bytes=$resumeFrom-")
                        ifRangeValue(resumeValidators)?.let { header(IF_RANGE_HEADER, it) }
                    }
                }.get()
                .build()

        private fun ifRangeValue(validators: ResumeValidators?): String? {
            val etag = validators?.etag?.takeIf { it.isNotBlank() }
            if (etag != null && !etag.startsWith(WEAK_ETAG_PREFIX)) return etag
            return validators?.lastModified?.takeIf { it.isNotBlank() }
        }

        private suspend fun executeCancellable(call: Call): Response =
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation { call.cancel() }
                try {
                    val response = call.execute()
                    continuation.resume(response) { _ -> response.close() }
                } catch (exception: Exception) {
                    if (!continuation.isCompleted) continuation.resumeWith(Result.failure(exception))
                }
            }

        private fun Response.toError(): DownloadError =
            when (code) {
                401 -> DownloadError.Auth
                403 -> DownloadError.Forbidden
                else -> DownloadError.Http(code)
            }

        private fun Response.totalBytes(startOffset: Long): Long? {
            if (code == PARTIAL_CONTENT) {
                header(CONTENT_RANGE_HEADER)
                    ?.substringAfterLast('/')
                    ?.toLongOrNull()
                    ?.takeIf { it > 0 }
                    ?.let { return it }
                return null
            }
            val length = body.contentLength().takeIf { it >= 0 } ?: return null
            return length + startOffset
        }

        private fun Response.rangeCompleteSize(): Long? =
            header(CONTENT_RANGE_HEADER)
                ?.substringAfterLast('/')
                ?.toLongOrNull()
                ?.takeIf { it >= 0 }

        private fun Response.rangeStart(): Long? =
            header(CONTENT_RANGE_HEADER)
                ?.substringAfter("$CONTENT_RANGE_UNIT ", "")
                ?.substringBefore('-')
                ?.toLongOrNull()
                ?.takeIf { it >= 0 }

        private companion object {
            const val AUTHORIZATION_HEADER = "Authorization"
            const val BEARER_PREFIX = "Bearer "
            const val RANGE_HEADER = "Range"
            const val IF_RANGE_HEADER = "If-Range"
            const val ETAG_HEADER = "ETag"
            const val LAST_MODIFIED_HEADER = "Last-Modified"
            const val CONTENT_RANGE_HEADER = "Content-Range"
            const val CONTENT_RANGE_UNIT = "bytes"
            const val ACCEPT_ENCODING_HEADER = "Accept-Encoding"
            const val IDENTITY_ENCODING = "identity"
            const val WEAK_ETAG_PREFIX = "W/"
            const val PARTIAL_CONTENT = 206
            const val RANGE_NOT_SATISFIABLE = 416
            const val BUFFER_SIZE = 8_192
            const val PROGRESS_THROTTLE_MILLIS = 500L
        }
    }
