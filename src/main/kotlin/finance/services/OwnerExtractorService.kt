package finance.services

import io.micronaut.http.HttpRequest
import jakarta.inject.Singleton

@Singleton
open class OwnerExtractorService(private val jwtTokenService: JwtTokenService) {

    open fun extractOwner(request: HttpRequest<*>): String? {
        val token = jwtTokenService.extractToken(request) ?: return null
        return try {
            jwtTokenService.parseClaims(token)[JwtTokenService.CLAIM_USERNAME] as? String
        } catch (e: Exception) {
            null
        }
    }
}
