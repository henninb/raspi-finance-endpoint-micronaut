package finance.controllers

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import java.util.UUID

@Controller("/api/csrf")
class CsrfController {

    @Get
    fun getCsrfToken(): HttpResponse<Map<String, String>> {
        return HttpResponse.ok(mapOf(
            "token" to UUID.randomUUID().toString(),
            "headerName" to "X-CSRF-TOKEN",
            "parameterName" to "_csrf"
        ))
    }
}
