package finance.services

import finance.domain.TokenBlacklist
import finance.repositories.TokenBlacklistRepository
import spock.lang.Specification

import java.time.Instant

class TokenBlacklistServiceSpec extends Specification {

    private TokenBlacklistRepository tokenBlacklistRepositoryMock = GroovyMock(TokenBlacklistRepository)
    private TokenBlacklistService service = new TokenBlacklistService(tokenBlacklistRepositoryMock)

    void 'test blacklistToken - saves entry to repository'() {
        given:
        String tokenHash = 'abc123hash'
        Instant expiresAt = Instant.now().plusSeconds(3600)

        when:
        service.blacklistToken(tokenHash, expiresAt)

        then:
        1 * tokenBlacklistRepositoryMock.save({ TokenBlacklist entry ->
            assert entry.tokenHash == tokenHash
            assert entry.expiresAt == expiresAt
            entry
        })
        0 * _
    }

    void 'test isBlacklisted - returns true when blacklisted'() {
        given:
        String tokenHash = 'blacklisted123'

        when:
        Boolean result = service.isBlacklisted(tokenHash)

        then:
        result
        1 * tokenBlacklistRepositoryMock.existsByTokenHash(tokenHash) >> true
        0 * _
    }

    void 'test isBlacklisted - returns false when not blacklisted'() {
        given:
        String tokenHash = 'validtoken123'

        when:
        Boolean result = service.isBlacklisted(tokenHash)

        then:
        !result
        1 * tokenBlacklistRepositoryMock.existsByTokenHash(tokenHash) >> false
        0 * _
    }

    void 'test cleanupExpiredTokens - deletes expired entries and returns count'() {
        when:
        int count = service.cleanupExpiredTokens()

        then:
        count == 5
        1 * tokenBlacklistRepositoryMock.deleteAllExpiredBefore(_ as Instant) >> 5
        0 * _
    }

    void 'test cleanupExpiredTokens - returns zero when no expired entries'() {
        when:
        int count = service.cleanupExpiredTokens()

        then:
        count == 0
        1 * tokenBlacklistRepositoryMock.deleteAllExpiredBefore(_ as Instant) >> 0
        0 * _
    }
}
