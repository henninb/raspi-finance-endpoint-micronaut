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
    fun selectAllActive(request: HttpRequest<*>): HttpResponse<List<Parameter>> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        val results = parameterService.findAllActive(owner)
        return if (results.isEmpty()) HttpResponse.notFound() else HttpResponse.ok(results)
    }

    @Get("/{parameterName}", produces = ["application/json"])
    fun selectParameter(@PathVariable parameterName: String, request: HttpRequest<*>): HttpResponse<Parameter> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        val optional: Optional<Parameter> = parameterService.findByParameter(owner, parameterName)
        return if (optional.isPresent) HttpResponse.ok(optional.get()) else HttpResponse.notFound()
    }

    @Post(produces = ["application/json"])
    fun insertParameter(@Body parameter: Parameter, request: HttpRequest<*>): HttpResponse<Parameter> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        if (parameter.owner.isNullOrBlank()) parameter.owner = owner
        parameterService.insertParameter(parameter)
        return HttpResponse.status<Parameter>(HttpStatus.CREATED).body(parameter)
    }

    @Put("/{parameterName}", produces = ["application/json"])
    fun updateParameter(@PathVariable parameterName: String, @Body parameter: Parameter, request: HttpRequest<*>): HttpResponse<Parameter> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        if (parameterService.updateParameter(owner, parameterName, parameter)) {
            return parameterService.findByParameter(owner, parameterName)
                .map { HttpResponse.ok(it) }
                .orElse(HttpResponse.notFound())
        }
        return HttpResponse.notFound()
    }

    @Delete("/{parameterName}", produces = ["application/json"])
    fun deleteByParameterName(@PathVariable parameterName: String, request: HttpRequest<*>): HttpResponse<Parameter> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        val optional: Optional<Parameter> = parameterService.findByParameter(owner, parameterName)
        if (optional.isPresent) {
            parameterService.deleteByParameterName(owner, parameterName)
            return HttpResponse.ok(optional.get())
        }
        return HttpResponse.notFound()
    }
}
