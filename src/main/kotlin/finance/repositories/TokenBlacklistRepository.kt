package finance.repositories

import finance.domain.TokenBlacklist
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import jakarta.transaction.Transactional
import java.time.Instant

@Repository
interface TokenBlacklistRepository : JpaRepository<TokenBlacklist, Long> {
    fun existsByTokenHash(tokenHash: String): Boolean

    @Transactional
    @Query(
        value = "DELETE FROM t_token_blacklist WHERE expires_at < :now",
        nativeQuery = true,
    )
    fun deleteAllExpiredBefore(now: Instant): Int
}
