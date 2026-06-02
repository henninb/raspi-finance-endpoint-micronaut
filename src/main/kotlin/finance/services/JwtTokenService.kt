package finance.services

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpRequest
import io.micronaut.http.cookie.Cookie
import jakarta.inject.Singleton
import org.apache.logging.log4j.LogManager
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Singleton
class JwtTokenService(
    @Value("\${custom.project.jwt.key:default-secret-key-must-be-at-least-32-bytes}") jwtKey: String,
) {
    companion object {
        private val securityLogger = LogManager.getLogger("SECURITY.${JwtTokenService::class.java.simpleName}")
        const val ISSUER = "raspi-finance-endpoint"
        const val AUDIENCE = "raspi-finance-endpoint"
        const val CLAIM_USERNAME = "username"
        const val CLAIM_KEEP_LOGGED_IN = "keepLoggedIn"
        private const val MIN_JWT_KEY_BYTES = 32
        const val JWT_EXPIRY_MS = 60 * 60 * 1000L
        const val JWT_EXPIRY_SECONDS = 3600L
        const val JWT_LONG_EXPIRY_MS = 30L * 24 * 60 * 60 * 1000L
        const val JWT_LONG_EXPIRY_SECONDS = 30L * 24 * 60 * 60L
    }

    private val secretKey: SecretKey = run {
        val keyBytes = jwtKey.toByteArray(Charsets.UTF_8)
        check(keyBytes.size >= MIN_JWT_KEY_BYTES) {
            "FATAL: custom.project.jwt.key is ${keyBytes.size} bytes — minimum $MIN_JWT_KEY_BYTES bytes required"
        }
        securityLogger.info("SECURITY_CONFIG JWT key validated: {} bytes", keyBytes.size)
        Keys.hmacShaKeyFor(keyBytes)
    }

    fun extractToken(request: HttpRequest<*>): String? {
        val fromCookie = request.cookies.findCookie("token").map { it.value }.orElse(null)
        if (!fromCookie.isNullOrBlank()) return fromCookie
        val authHeader = request.headers.get("Authorization")
        if (!authHeader.isNullOrBlank() && authHeader.startsWith("Bearer ")) {
            return authHeader.removePrefix("Bearer ").trim()
        }
        return null
    }

    fun parseClaims(token: String): Claims =
        Jwts.parser()
            .requireIssuer(ISSUER)
            .requireAudience(AUDIENCE)
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

    fun buildToken(username: String, keepLoggedIn: Boolean = false): String {
        val expiryMs = if (keepLoggedIn) JWT_LONG_EXPIRY_MS else JWT_EXPIRY_MS
        val now = Date()
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .issuer(ISSUER)
            .audience().add(AUDIENCE).and()
            .subject(username)
            .claim(CLAIM_USERNAME, username)
            .claim(CLAIM_KEEP_LOGGED_IN, keepLoggedIn)
            .issuedAt(now)
            .notBefore(now)
            .expiration(Date(now.time + expiryMs))
            .signWith(secretKey)
            .compact()
    }

    fun expirySecondsFor(keepLoggedIn: Boolean): Long =
        if (keepLoggedIn) JWT_LONG_EXPIRY_SECONDS else JWT_EXPIRY_SECONDS

    fun buildTokenCookie(token: String, keepLoggedIn: Boolean): Cookie =
        Cookie.of("token", token)
            .domain(".bhenning.com")
            .path("/")
            .maxAge(expirySecondsFor(keepLoggedIn))
            .httpOnly(true)
            .secure(true)
            .sameSite(io.micronaut.http.cookie.SameSite.None)

    fun buildClearCookie(): Cookie =
        Cookie.of("token", "")
            .domain(".bhenning.com")
            .path("/")
            .maxAge(0)
            .httpOnly(true)
            .secure(true)
            .sameSite(io.micronaut.http.cookie.SameSite.None)
}
