package finance.controllers

import finance.domain.LoginRequest
import finance.domain.User
import finance.services.JwtTokenService
import finance.services.LoginAttemptService
import finance.services.TokenBlacklistService
import finance.services.UserService
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import java.security.MessageDigest
import java.time.Instant

@Controller("/api")
class LoginController(
    private val userService: UserService,
    private val jwtTokenService: JwtTokenService,
    private val loginAttemptService: LoginAttemptService,
    private val tokenBlacklistService: TokenBlacklistService,
) : BaseController() {

    @Post("/login")
    fun login(@Body loginRequest: LoginRequest): HttpResponse<Map<String, String>> {
        logger.info("Login request received: ${loginRequest.username}")

        if (loginAttemptService.isLocked(loginRequest.username)) {
            val remaining = loginAttemptService.remainingLockSeconds(loginRequest.username)
            logger.warn("Account locked: ${loginRequest.username}, $remaining seconds remaining")
            return HttpResponse.status<Map<String, String>>(HttpStatus.TOO_MANY_REQUESTS)
                .body(mapOf("error" to "Account locked. Try again in $remaining seconds."))
        }

        val userToCheck = User().apply {
            username = loginRequest.username
            password = loginRequest.password
        }
        val user = userService.signIn(userToCheck)
        if (user.isEmpty) {
            loginAttemptService.recordFailure(loginRequest.username)
            return HttpResponse.status<Map<String, String>>(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Invalid credentials"))
        }

        loginAttemptService.recordSuccess(loginRequest.username)
        val token = jwtTokenService.buildToken(loginRequest.username, loginRequest.keepLoggedIn)
        val cookie = jwtTokenService.buildTokenCookie(token, loginRequest.keepLoggedIn)
        return HttpResponse.ok(mapOf("message" to "Login successful")).cookie(cookie)
    }

    @Post("/logout")
    fun logout(request: HttpRequest<*>): HttpResponse<Void> {
        val token = jwtTokenService.extractToken(request)
        if (!token.isNullOrBlank()) {
            try {
                val claims = jwtTokenService.parseClaims(token)
                val expiry = claims.expiration?.toInstant() ?: Instant.now().plusSeconds(3600)
                val tokenHash = sha256(token)
                tokenBlacklistService.blacklistToken(tokenHash, expiry)
            } catch (e: Exception) {
                logger.warn("Could not blacklist token on logout: ${e.message}")
            }
        }
        return HttpResponse.noContent<Void>().cookie(jwtTokenService.buildClearCookie())
    }

    @Post("/refresh")
    fun refresh(request: HttpRequest<*>): HttpResponse<Map<String, String>> {
        val token = jwtTokenService.extractToken(request)
        if (token.isNullOrBlank()) {
            return HttpResponse.status<Map<String, String>>(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "No token provided"))
        }
        return try {
            val tokenHash = sha256(token)
            if (tokenBlacklistService.isBlacklisted(tokenHash)) {
                return HttpResponse.status<Map<String, String>>(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Token has been revoked"))
            }
            val claims = jwtTokenService.parseClaims(token)
            val username = claims[JwtTokenService.CLAIM_USERNAME] as? String
            val keepLoggedIn = claims[JwtTokenService.CLAIM_KEEP_LOGGED_IN] as? Boolean ?: false
            if (username.isNullOrBlank()) {
                return HttpResponse.status<Map<String, String>>(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Invalid token"))
            }
            val newToken = jwtTokenService.buildToken(username, keepLoggedIn)
            val cookie = jwtTokenService.buildTokenCookie(newToken, keepLoggedIn)
            HttpResponse.ok(mapOf("message" to "Token refreshed")).cookie(cookie)
        } catch (e: Exception) {
            logger.warn("Token refresh failed: ${e.message}")
            HttpResponse.status<Map<String, String>>(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Invalid or expired token"))
        }
    }

    @Post("/register")
    @Consumes("application/json")
    fun register(@Body newUser: User): HttpResponse<Map<String, String>> {
        logger.info("Register request received: ${newUser.username}")
        try {
            userService.signUp(newUser)
        } catch (e: IllegalArgumentException) {
            logger.info("Username ${newUser.username} already exists.")
            return HttpResponse.status<Map<String, String>>(HttpStatus.CONFLICT).body(mapOf("error" to "Username already exists"))
        }
        logger.info("User registered, generating JWT")
        val token = jwtTokenService.buildToken(newUser.username)
        val cookie = jwtTokenService.buildTokenCookie(token, false)
        return HttpResponse.status<Map<String, String>>(HttpStatus.CREATED)
            .body(mapOf("message" to "Registration successful"))
            .cookie(cookie)
    }

    @Get("/me")
    fun getCurrentUser(request: HttpRequest<*>): HttpResponse<Any> {
        val token = jwtTokenService.extractToken(request)
        if (token.isNullOrBlank()) {
            return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        }
        return try {
            val tokenHash = sha256(token)
            if (tokenBlacklistService.isBlacklisted(tokenHash)) {
                return HttpResponse.status(HttpStatus.UNAUTHORIZED)
            }
            val claims = jwtTokenService.parseClaims(token)
            val username = claims[JwtTokenService.CLAIM_USERNAME] as? String
            if (username.isNullOrBlank()) return HttpResponse.status(HttpStatus.UNAUTHORIZED)
            val user = userService.findUserByUsername(username) ?: return HttpResponse.status(HttpStatus.NOT_FOUND)
            HttpResponse.ok(user)
        } catch (e: Exception) {
            logger.error("Error processing /api/me request", e)
            HttpResponse.status(HttpStatus.UNAUTHORIZED)
        }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
