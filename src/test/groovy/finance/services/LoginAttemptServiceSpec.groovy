package finance.services

import spock.lang.Specification

class LoginAttemptServiceSpec extends Specification {

    private LoginAttemptService service = new LoginAttemptService()

    void 'test isLocked - new username not locked'() {
        expect:
        !service.isLocked('user@example.com')
    }

    void 'test recordFailure and isLocked - below max attempts not locked'() {
        given:
        String username = 'testuser'

        when:
        5.times { service.recordFailure(username) }

        then:
        !service.isLocked(username)
    }

    void 'test recordFailure - at max attempts triggers lockout'() {
        given:
        String username = 'lockeduser'

        when:
        10.times { service.recordFailure(username) }

        then:
        service.isLocked(username)
    }

    void 'test recordSuccess - clears failed attempts'() {
        given:
        String username = 'cleareduser'
        5.times { service.recordFailure(username) }

        when:
        service.recordSuccess(username)

        then:
        !service.isLocked(username)
    }

    void 'test isLocked - case insensitive username'() {
        given:
        String username = 'CaseSensitiveUser'
        10.times { service.recordFailure(username) }

        expect:
        service.isLocked('casesensitiveuser')
        service.isLocked('CASESENSITIVEUSER')
        service.isLocked(username)
    }

    void 'test remainingLockSeconds - zero for unknown username'() {
        expect:
        service.remainingLockSeconds('unknown') == 0L
    }

    void 'test remainingLockSeconds - positive when locked'() {
        given:
        String username = 'lockedForSeconds'
        10.times { service.recordFailure(username) }

        when:
        long remaining = service.remainingLockSeconds(username)

        then:
        remaining > 0L
        remaining <= 900L
    }

    void 'test recordFailure - additional failures after lockout keep locked'() {
        given:
        String username = 'moreattempts'
        10.times { service.recordFailure(username) }

        when:
        5.times { service.recordFailure(username) }

        then:
        service.isLocked(username)
    }

    void 'test cleanupExpiredEntries - does not throw'() {
        when:
        service.cleanupExpiredEntries()

        then:
        noExceptionThrown()
    }
}
