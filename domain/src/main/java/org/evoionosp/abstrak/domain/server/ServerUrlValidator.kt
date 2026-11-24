package org.evoionosp.abstrak.domain.server

/**
 * Whether a server address the user typed can be the base URL of an Audiobookshelf server: an
 * http or https URL, or a bare host that the app combines with the selected protocol.
 *
 * The data layer answers with the same URL parser its HTTP clients use, so an address accepted
 * here is never rejected as malformed once connecting, and one the clients would accept is never
 * refused here. The check is syntactic only: a well-formed URL can still point at a server that
 * is unreachable, which the server check reports separately.
 */
fun interface ServerUrlValidator {
    fun isValidServerUrl(rawUrl: String): Boolean
}
