package finance.controllers

import finance.domain.User
import finance.services.JwtTokenService
import finance.services.UserService
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*

@Controller("/api")
class LoginController(
    private val userService: UserService,
    private val jwtTokenService: JwtTokenService,
) : BaseController() {

    @Post("/login")
    fun login(@Body loginRequest: User): HttpResponse<Map<String, String>> {
        val user = userService.signIn(loginRequest)
        logger.info("Login request received: ${loginRequest.username}")
        if (user.isEmpty) {
            return HttpResponse.status<Map<String, String>>(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Invalid credentials"))
        }
        val token = jwtTokenService.buildToken(loginRequest.username)
        val cookie = jwtTokenService.buildTokenCookie(token, false)
        return HttpResponse.ok(mapOf("message" to "Login successful")).cookie(cookie)
    }

    @Post("/logout")
    fun logout(): HttpResponse<Void> {
        return HttpResponse.noContent<Void>().cookie(jwtTokenService.buildClearCookie())
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
}
