package finance.services

import finance.domain.User
import finance.repositories.UserRepository
import org.mindrot.jbcrypt.BCrypt
import spock.lang.Specification

class UserServiceSpec extends Specification {

    private UserRepository userRepositoryMock = GroovyMock(UserRepository)
    private UserService service = new UserService(userRepositoryMock)

    private User buildUser(String username, String plainPassword) {
        User user = new User()
        user.username = username
        user.password = plainPassword
        user.firstName = 'Test'
        user.lastName = 'User'
        return user
    }

    void 'test signIn - correct password returns user'() {
        given:
        String rawPassword = 'secret123'
        User dbUser = buildUser('brian', BCrypt.hashpw(rawPassword, BCrypt.gensalt(4)))
        User loginUser = buildUser('brian', rawPassword)

        when:
        Optional<User> result = service.signIn(loginUser)

        then:
        result.isPresent()
        1 * userRepositoryMock.findByUsername('brian') >> Optional.of(dbUser)
        0 * _
    }

    void 'test signIn - wrong password returns empty'() {
        given:
        String storedPassword = BCrypt.hashpw('correctpassword', BCrypt.gensalt(4))
        User dbUser = buildUser('brian', storedPassword)
        User loginUser = buildUser('brian', 'wrongpassword')

        when:
        Optional<User> result = service.signIn(loginUser)

        then:
        !result.isPresent()
        1 * userRepositoryMock.findByUsername('brian') >> Optional.of(dbUser)
        0 * _
    }

    void 'test signIn - user not found returns empty'() {
        given:
        User loginUser = buildUser('nonexistent', 'password')

        when:
        Optional<User> result = service.signIn(loginUser)

        then:
        !result.isPresent()
        1 * userRepositoryMock.findByUsername('nonexistent') >> Optional.empty()
        0 * _
    }

    void 'test signUp - new user is saved with hashed password'() {
        given:
        User newUser = buildUser('newbrian', 'plainpassword')
        User savedUser = buildUser('newbrian', BCrypt.hashpw('plainpassword', BCrypt.gensalt(4)))

        when:
        User result = service.signUp(newUser)

        then:
        result == savedUser
        1 * userRepositoryMock.findByUsername('newbrian') >> Optional.empty()
        1 * userRepositoryMock.saveAndFlush(_ as User) >> savedUser
        0 * _
    }

    void 'test signUp - duplicate username throws IllegalArgumentException'() {
        given:
        User existingUser = buildUser('existing', 'somepassword')
        User newUser = buildUser('existing', 'anotherpassword')

        when:
        service.signUp(newUser)

        then:
        thrown(IllegalArgumentException)
        1 * userRepositoryMock.findByUsername('existing') >> Optional.of(existingUser)
        0 * _
    }

    void 'test findUserByUsername - found returns user with blank password'() {
        given:
        User dbUser = buildUser('brian', 'hashedpassword')

        when:
        User result = service.findUserByUsername('brian')

        then:
        result != null
        result.password == ''
        1 * userRepositoryMock.findByUsername('brian') >> Optional.of(dbUser)
        0 * _
    }

    void 'test findUserByUsername - not found returns null'() {
        when:
        User result = service.findUserByUsername('nobody')

        then:
        result == null
        1 * userRepositoryMock.findByUsername('nobody') >> Optional.empty()
        0 * _
    }
}
