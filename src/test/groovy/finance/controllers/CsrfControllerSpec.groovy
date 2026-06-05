package finance.controllers

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import spock.lang.Specification

class CsrfControllerSpec extends Specification {

    private CsrfController controller = new CsrfController()

    void 'test getCsrfToken - returns 200 with token fields'() {
        when:
        HttpResponse<Map<String, String>> response = controller.getCsrfToken()

        then:
        response.status == HttpStatus.OK
        response.body().containsKey('token')
        response.body()['headerName'] == 'X-CSRF-TOKEN'
        response.body()['parameterName'] == '_csrf'
        (response.body()['token'] as String).length() == 36
    }

    void 'test getCsrfToken - returns unique token each call'() {
        when:
        HttpResponse<Map<String, String>> response1 = controller.getCsrfToken()
        HttpResponse<Map<String, String>> response2 = controller.getCsrfToken()

        then:
        response1.body()['token'] != response2.body()['token']
    }
}
