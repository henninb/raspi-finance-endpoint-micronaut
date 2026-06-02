package finance.controllers

import finance.domain.Parameter
import finance.services.OwnerExtractorService
import finance.services.ParameterService
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import jakarta.inject.Inject
import java.util.*

@Controller("/api/parameter")
class ParameterController(
    @Inject val parameterService: ParameterService,
    @Inject val ownerExtractorService: OwnerExtractorService,
) {

    @Get("/active", produces = ["application/json"])
    fun selectAllActive(): HttpResponse<List<Parameter>> {
        val results = parameterService.findAllActive()
        return if (results.isEmpty()) HttpResponse.notFound() else HttpResponse.ok(results)
    }

    @Get("/{parameterName}", produces = ["application/json"])
    fun selectParameter(@PathVariable parameterName: String): HttpResponse<Parameter> {
        val optional: Optional<Parameter> = parameterService.findByParameter(parameterName)
        return if (optional.isPresent) HttpResponse.ok(optional.get()) else HttpResponse.notFound()
    }

    @Post(produces = ["application/json"])
    fun insertParameter(@Body parameter: Parameter, request: HttpRequest<*>): HttpResponse<Parameter> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        if (parameter.owner.isNullOrBlank()) parameter.owner = owner
        parameterService.insertParameter(parameter)
        return HttpResponse.status<Parameter>(HttpStatus.CREATED).body(parameter)
    }

    @Delete("/{parameterName}", produces = ["application/json"])
    fun deleteByParameterName(@PathVariable parameterName: String): HttpResponse<Parameter> {
        val optional: Optional<Parameter> = parameterService.findByParameter(parameterName)
        if (optional.isPresent) {
            parameterService.deleteByParameterName(parameterName)
            return HttpResponse.ok(optional.get())
        }
        return HttpResponse.notFound()
    }
}
