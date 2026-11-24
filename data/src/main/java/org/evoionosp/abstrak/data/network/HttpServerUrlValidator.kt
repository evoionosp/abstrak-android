package org.evoionosp.abstrak.data.network
import javax.inject.Inject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.evoionosp.abstrak.domain.server.ServerUrlValidator

/**
 * Validates an address the way the HTTP clients will read it: [UrlUtils.normalizeBaseUrl], then
 * OkHttp's HttpUrl, the parse Retrofit's baseUrl makes. On top of that it refuses:
 * - user info: OkHttp never sends it as credentials, while the base URL is stored and logged;
 * - a query or fragment: Retrofit resolves every request against the base path, dropping them;
 * - a path starting with "//": what a mistyped scheme (ftp://host, http//host) becomes once
 *   https:// is put in front, leaving the scheme as the host.
 */
class HttpServerUrlValidator
    @Inject
    constructor() : ServerUrlValidator {
        override fun isValidServerUrl(rawUrl: String): Boolean {
            if (rawUrl.isBlank()) return false
            val url = UrlUtils.normalizeBaseUrl(rawUrl).toHttpUrlOrNull() ?: return false
            return url.username.isEmpty() &&
                url.password.isEmpty() &&
                url.query == null &&
                url.fragment == null &&
                !url.encodedPath.startsWith("//") &&
                // Retrofit's own rule: a base URL's path ends in '/'.
                url.pathSegments.last().isEmpty()
        }
    }
