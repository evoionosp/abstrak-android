package org.evoionosp.abstrak.data.network
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit

class HttpServerUrlValidatorTest {
    private val validator = HttpServerUrlValidator()

    @Test
    fun `an empty url is invalid`() {
        assertFalse(validator.isValidServerUrl(""))
        assertFalse(validator.isValidServerUrl("   "))
    }

    @Test
    fun `full http and https urls are valid`() {
        assertValid(
            "https://example.com",
            "http://example.com",
            "HTTPS://EXAMPLE.COM:13378",
            "https://example.com:443",
            "https://example.com/",
            "https://audiobooks.example.com:13378/audiobookshelf",
        )
    }

    @Test
    fun `a bare host with an optional port is valid`() {
        assertValid("example.com", "example.com:13378", "localhost", "localhost:13378", "myserver.local:13378")
        assertValid("my-server:13378")
    }

    @Test
    fun `ipv4 and bracketed ipv6 addresses are valid`() {
        assertValid("192.168.1.10", "192.168.1.10:13378", "http://192.168.1.10:13378", "127.0.0.1:13378")
        assertValid("[::1]", "[::1]:13378", "https://[::1]:13378", "https://[2001:db8::1]:13378")
    }

    @Test
    fun `an at sign in the path is part of the path`() {
        assertValid("https://example.com/@abs", "example.com/@abs")
    }

    @Test
    fun `ports outside 1 to 65535 or with letters are invalid`() {
        assertValid("example.com:1", "example.com:65535")
        assertInvalid("example.com:0", "example.com:65536", "example.com:abc", "example.com:13378:99")
    }

    @Test
    fun `a url without a host is invalid`() {
        assertInvalid("https://", "https://:13378", "://example.com", "http:example.com")
    }

    @Test
    fun `a scheme other than http or https is invalid`() {
        assertInvalid("ftp://example.com", "http//example.com")
    }

    @Test
    fun `malformed hosts are invalid`() {
        assertInvalid("exa mple.com", "[::1", "https://[gggg::1]", "https://[::1]:abc", "https://[]:13378")
        assertInvalid("https://[1.2.3.4::1]")
    }

    @Test
    fun `user info, a query and a fragment are refused`() {
        assertInvalid(
            "https://user@example.com",
            "https://user:secret@example.com",
            "https://example.com?query=1",
            "https://example.com/abs?query=1",
            "https://example.com#fragment",
        )
    }

    @Test
    fun `accepts what Retrofit takes as a base url, less the refused parts`() {
        for (address in SAMPLES) {
            val parsed =
                runCatching { Retrofit.Builder().baseUrl(UrlUtils.normalizeBaseUrl(address)).build().baseUrl() }
                    .getOrNull()
            val expected =
                parsed != null &&
                    parsed.username.isEmpty() &&
                    parsed.password.isEmpty() &&
                    parsed.query == null &&
                    parsed.fragment == null &&
                    !parsed.encodedPath.startsWith("//")
            assertEquals(address, expected, validator.isValidServerUrl(address))
        }
    }

    private fun assertValid(vararg addresses: String) {
        addresses.forEach { assertTrue(it, validator.isValidServerUrl(it)) }
    }

    private fun assertInvalid(vararg addresses: String) {
        addresses.forEach { assertFalse(it, validator.isValidServerUrl(it)) }
    }

    private companion object {
        val SAMPLES =
            listOf(
                "",
                "example.com",
                "https://example.com/audiobookshelf",
                "my_host:13378",
                "example.com:",
                "example.com/pa th",
                "256.1.1.1",
                "192.168.1",
                "192.168.001.010",
                "1.2.3.4.5",
                "example..com",
                ".example.com",
                "-example.com",
                "https://example.com/@abs",
                "https://[1.2.3.4::1]",
                "https://[::ffff:1.2.3.4]",
                "https://user@example.com",
                "https://example.com?query=1",
                "https://example.com#fragment",
                "ftp://example.com",
                "example.com\\abs",
            )
    }
}
