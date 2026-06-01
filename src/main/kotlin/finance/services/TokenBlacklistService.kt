package finance.services

import finance.domain.TokenBlacklist
import finance.repositories.TokenBlacklistRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.apache.logging.log4j.LogManager
import java.time.Instant

@Singleton
open class TokenBlacklistService(
    @Inject val tokenBlacklistRepository: TokenBlacklistRepository,
) {
    open fun blacklistToken(tokenHash: String, expiresAt: Instant) {
        val entry = TokenBlacklist(tokenHash = tokenHash, expiresAt = expiresAt)
        tokenBlacklistRepository.save(entry)
        logger.info("Token blacklisted: $tokenHash")
    }

    open fun isBlacklisted(tokenHash: String): Boolean = tokenBlacklistRepository.existsByTokenHash(tokenHash)

    open fun cleanupExpiredTokens(): Int {
        val count = tokenBlacklistRepository.deleteAllExpiredBefore(Instant.now())
        logger.info("Cleaned up $count expired token blacklist entries")
        return count
    }

    companion object {
        private val logger = LogManager.getLogger()
    }
}
