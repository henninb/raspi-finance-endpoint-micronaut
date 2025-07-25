package finance.controllers

import finance.domain.User
import finance.services.UserService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.cookie.Cookie
import io.micronaut.http.exceptions.HttpStatusException
import java.util.*

@Controller("/api")
class LoginController(private val userService: UserService) : BaseController() {

    @Value("\${custom.project.jwt.key}")
    private lateinit var jwtKey: String

    // curl -k --header "Content-Type: application/json" --request POST --data '{"username": "testuser", "password": "password123"}' https://localhost:8443/api/login
    @Post("/login")
    fun login(
        @Body loginRequest: User
    ): HttpResponse<Void> {
        // Validate user credentials.
        val user = userService.signIn(loginRequest)
        logger.info("Login request received: ${loginRequest.username}")
        if (user.isEmpty) {
            return HttpResponse.status(HttpStatus.FORBIDDEN)
        }

        // Generate JWT after validating credentials.
        val now = Date()
        val expiration = Date(now.time + 60 * 60 * 1000) // 1 hour expiration
        val token = Jwts.builder()
            .claim("username", loginRequest.username)
            .setNotBefore(now)
            .setExpiration(expiration)
            .signWith(SignatureAlgorithm.HS256, jwtKey.toByteArray())
            .compact()

        val cookie = Cookie.of("token", token)
            .domain(".bhenning.com")
            .path("/")
            .maxAge(7 * 24 * 60 * 60)
            .httpOnly(true)
            .secure(true)
            .sameSite(io.micronaut.http.cookie.SameSite.None)

        return HttpResponse.noContent<Void>().cookie(cookie)
    }

    // curl -k --header "Content-Type: application/json" --request POST https://localhost:8443/api/logout
    @Post("/logout")
    fun logout(): HttpResponse<Void> {
        val cookie = Cookie.of("token", "")
            .domain(".bhenning.com")
            .path("/")
            .maxAge(0)
            .httpOnly(true)
            .secure(true)
            .sameSite(io.micronaut.http.cookie.SameSite.None)

        return HttpResponse.noContent<Void>().cookie(cookie)
    }

    // curl -k --header "Content-Type: application/json" --request POST --data '{"username": "newuser", "password": "password123", "email": "user@example.com"}' https://localhost:8443/api/register
    @Post("/register")
    @Consumes("application/json")
    fun register(
        @Body newUser: User
    ): HttpResponse<Void> {
        logger.info("Register request received: $newUser")
        try {
            // Register the new user.
            userService.signUp(newUser)
        } catch (e: IllegalArgumentException) {
            logger.info("Username ${newUser.username} already exists.")
            return HttpResponse.status(HttpStatus.CONFLICT)
        }

        // Auto-login: generate a JWT token for the new user.
        logger.info("User registered, generating JWT")
        val now = Date()
        val expiration = Date(now.time + 60 * 60 * 1000) // 1 hour expiration

        val token = Jwts.builder()
            .claim("username", newUser.username)
            .setNotBefore(now)
            .setExpiration(expiration)
            .signWith(SignatureAlgorithm.HS256, jwtKey.toByteArray())
            .compact()

        val cookie = Cookie.of("token", token)
            .httpOnly(true)
            .secure(true)
            .maxAge(24 * 60 * 60)
            .sameSite(io.micronaut.http.cookie.SameSite.None) // needed for cross-origin cookie sharing
            .path("/")

        return HttpResponse.status<Void>(HttpStatus.CREATED).cookie(cookie)
    }

    // curl -k --header "Cookie: token=your_jwt_token" https://localhost:8443/api/me
    @Get("/me")
    fun getCurrentUser(@CookieValue(value = "token", defaultValue = "") token: String): HttpResponse<Any> {
        return try {
            // Check if JWT key is configured
            if (!this::jwtKey.isInitialized || jwtKey.isBlank()) {
                logger.error("JWT key is not properly configured")
                return HttpResponse.serverError("Server configuration error")
            }

            // Check if the token cookie is present.
            if (token.isBlank()) {
                logger.info("No token found in the request")
                return HttpResponse.status(HttpStatus.UNAUTHORIZED)
            }

            // Parse and validate the JWT.
            val claims = Jwts.parser()
                .setSigningKey(jwtKey.toByteArray())
                .parseClaimsJws(token)
                .body

            val username = claims["username"] as? String
            if (username.isNullOrBlank()) {
                logger.info("Token does not contain a valid username claim")
                return HttpResponse.status(HttpStatus.UNAUTHORIZED)
            }

            // Optionally, fetch the full user details from your database.
            val user = userService.findUserByUsername(username)
            if (user == null) {
                logger.info("No user found for username: $username")
                return HttpResponse.status(HttpStatus.NOT_FOUND)
            }

            // Return user information (excluding sensitive data).
            HttpResponse.ok(user)
        } catch (e: Exception) {
            logger.error("Error processing /api/me request", e)
            HttpResponse.serverError("Internal server error")
        }
    }
}