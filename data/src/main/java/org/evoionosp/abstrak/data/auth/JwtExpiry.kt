package org.evoionosp.abstrak.data.auth
import android.util.Base64
import org.json.JSONException
import org.json.JSONObject

internal object JwtExpiry {
    private const val JWT_PART_COUNT = 3
    private const val PAYLOAD_INDEX = 1
    private const val EXPIRY_CLAIM = "exp"
    private const val BASE64_FLAGS = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP

    fun expiresAtEpochSeconds(token: String): Long? {
        val parts = token.split('.')
        if (parts.size != JWT_PART_COUNT) return null
        return try {
            val payload = Base64.decode(parts[PAYLOAD_INDEX], BASE64_FLAGS)
            val claims = JSONObject(String(payload, Charsets.UTF_8))
            if (claims.has(EXPIRY_CLAIM)) claims.getLong(EXPIRY_CLAIM) else null
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: JSONException) {
            null
        }
    }
}
