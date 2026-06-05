package finance.controllers

import finance.domain.User
import finance.services.JwtTokenService
import finance.services.UserService
import io.jsonwebtoken.Claims
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.cookie.Cookie
import spock.lang.Specification

@SuppressWarnings("GroovyAccessibility")
class LoginControllerSpec extends Specification {

    private UserService userServiceMock = GroovyMock(UserService)
    private JwtTokenService jwtTokenServiceMock = GroovyMock(JwtTokenService)
    private LoginController controller = new LoginController(userServiceMock, jwtTokenServiceMock)
    private HttpRequest requestMock = Mock(HttpRequest)
    private Cookie testCookie = Cookie.of("token", "test-jwt-token")

    private User buildUser(String username = 'brian', String password = 'secret') {
        User user = new User()
        user.username = username
        user.password = password
        user.firstName = 'Test'
        user.lastName = 'User'
        return user
    }

    void 'test login - returns 200 on success'() {
        given:
        User loginRequest = buildUser()
        User dbUser = buildUser()

        when:
        HttpResponse response = controller.login(loginRequest)

        then:
        response.status == HttpStatus.OK
        1 * userServiceMock.signIn(loginRequest) >> Optional.of(dbUser)
        1 * jwtTokenServiceMock.buildToken('brian', false) >> 'jwt-token'
        1 * jwtTokenServiceMock.buildTokenCookie('jwt-token', false) >> testCookie
        0 * _
    }

    void 'test login - returns 401 when credentials invalid'() {
        given:
        User loginRequest = buildUser()

        when:
        HttpResponse response = controller.login(loginRequest)

        then:
        response.status == HttpStatus.UNAUTHORIZED
        1 * userServiceMock.signIn(loginRequest) >> Optional.empty()
        0 * _
    }

    void 'test logout - returns 204'() {
        when:
        HttpResponse response = controller.logout()

        then:
        response.status == HttpStatus.NO_CONTENT
        1 * jwtTokenServiceMock.buildClearCookie() >> testCookie
        0 * _
    }

    void 'test register - returns 201 on success'() {
        given:
        User newUser = buildUser('newuser')

        when:
        HttpResponse response = controller.register(newUser)

        then:
        response.status == HttpStatus.CREATED
        1 * userServiceMock.signUp(newUser) >> newUser
        1 * jwtTokenServiceMock.buildToken('newuser', false) >> 'jwt-token'
        1 * jwtTokenServiceMock.buildTokenCookie('jwt-token', false) >> testCookie
        0 * _
    }

    void 'test register - returns 409 when username already exists'() {
        given:
        User newUser = buildUser('existing')

        when:
        HttpResponse response = controller.register(newUser)

        then:
        response.status == HttpStatus.CONFLICT
        1 * userServiceMock.signUp(newUser) >> { throw new IllegalArgumentException('Username already exists') }
        0 * _
    }

    void 'test getCurrentUser - returns 401 when no token'() {
        when:
        HttpResponse response = controller.getCurrentUser(requestMock)

        then:
        response.status == HttpStatus.UNAUTHORIZED
        1 * jwtTokenServiceMock.extractToken(requestMock) >> null
        0 * _
    }

    void 'test getCurrentUser - returns 200 with user when token valid'() {
        given:
        User user = buildUser()
        user.password = ''
        Claims claimsStub = Stub(Claims)
        claimsStub.get(JwtTokenService.CLAIM_USERNAME) >> 'brian'

        when:
        HttpResponse response = controller.getCurrentUser(requestMock)

        then:
        response.status == HttpStatus.OK
        1 * jwtTokenServiceMock.extractToken(requestMock) >> 'valid-token'
        1 * jwtTokenServiceMock.parseClaims('valid-token') >> claimsStub
        1 * userServiceMock.findUserByUsername('brian') >> user
        0 * _
    }

    void 'test getCurrentUser - returns 401 when token parsing throws exception'() {
        when:
        HttpResponse response = controller.getCurrentUser(requestMock)

        then:
        response.status == HttpStatus.UNAUTHORIZED
        1 * jwtTokenServiceMock.extractToken(requestMock) >> 'invalid-token'
        1 * jwtTokenServiceMock.parseClaims('invalid-token') >> { throw new Exception('invalid token') }
        0 * _
    }

    void 'test getCurrentUser - returns 404 when user not found in db'() {
        given:
        Claims claimsStub = Stub(Claims)
        claimsStub.get(JwtTokenService.CLAIM_USERNAME) >> 'brian'

        when:
        HttpResponse response = controller.getCurrentUser(requestMock)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * jwtTokenServiceMock.extractToken(requestMock) >> 'valid-token'
        1 * jwtTokenServiceMock.parseClaims('valid-token') >> claimsStub
        1 * userServiceMock.findUserByUsername('brian') >> null
        0 * _
    }
}
