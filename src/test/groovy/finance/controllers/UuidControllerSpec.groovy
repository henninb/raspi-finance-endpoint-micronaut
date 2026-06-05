package finance.controllers

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import spock.lang.Specification

class UuidControllerSpec extends Specification {

    private UuidController controller = new UuidController()

    void 'test generateUuid - returns 200 with uuid'() {
        when:
        HttpResponse<Map<String, Object>> response = controller.generateUuid()

        then:
        response.status == HttpStatus.OK
        response.body().containsKey('uuid')
        response.body().containsKey('timestamp')
        response.body()['source'] == 'server'
        (response.body()['uuid'] as String).length() == 36
    }

    void 'test generateBatchUuids - returns 200 with batch'() {
        when:
        HttpResponse<Map<String, Object>> response = controller.generateBatchUuids(3)

        then:
        response.status == HttpStatus.OK
        response.body()['count'] == 3
        (response.body()['uuids'] as List).size() == 3
    }

    void 'test generateBatchUuids - returns 200 with default count of 1'() {
        when:
        HttpResponse<Map<String, Object>> response = controller.generateBatchUuids(1)

        then:
        response.status == HttpStatus.OK
        response.body()['count'] == 1
    }

    void 'test generateBatchUuids - returns 400 for count zero'() {
        when:
        HttpResponse<Map<String, Object>> response = controller.generateBatchUuids(0)

        then:
        response.status == HttpStatus.BAD_REQUEST
    }

    void 'test generateBatchUuids - returns 400 for count over 100'() {
        when:
        HttpResponse<Map<String, Object>> response = controller.generateBatchUuids(101)

        then:
        response.status == HttpStatus.BAD_REQUEST
    }

    void 'test healthCheck - returns 200 with healthy status'() {
        when:
        HttpResponse<Map<String, Object>> response = controller.healthCheck()

        then:
        response.status == HttpStatus.OK
        response.body()['status'] == 'healthy'
        response.body()['service'] == 'uuid-generation'
    }
}
