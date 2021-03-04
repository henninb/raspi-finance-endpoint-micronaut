package finance.controllers

import finance.domain.Parameter
import finance.services.ParameterService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import java.util.*
import javax.inject.Inject

@Controller("/parameter")
class ParameterController(@Inject val parameterService: ParameterService) {
    //https://hornsup:8080/parm/select/payment_account
    @Get("/select/{parameterName}", produces = ["application/json"])
    fun selectParameter(@PathVariable parameterName: String): HttpResponse<Parameter> {
        val parameterOptional: Optional<Parameter> = parameterService.findByParameter(parameterName)
        if (!parameterOptional.isPresent) {
            return HttpResponse.notFound()
//            BaseController.logger.error("no parameter found.")
//            throw ResponseStatusException(HttpStatus.NOT_FOUND, "could not find the parm.")
        }
        return HttpResponse.ok(parameterOptional.get())
    }

    //curl --header "Content-Type: application/json" -X POST -d '{"parm":"test"}' http://localhost:8080/parm/insert
    @Post("/insert", produces = ["application/json"])
    fun insertParameter(@Body parameter: Parameter): HttpResponse<String> {
        parameterService.insertParameter(parameter)
        //BaseController.logger.debug("insertParameter")
        return HttpResponse.ok("parameter inserted")
    }

    @Delete("/delete/{parameterName}", produces = ["application/json"])
    fun deleteByParameterName(@PathVariable parameterName: String): HttpResponse<String> {
        parameterService.deleteByParameterName(parameterName)
        return HttpResponse.ok("parameter deleted")
    }
}