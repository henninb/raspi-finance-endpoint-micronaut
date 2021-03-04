package finance.controllers

import finance.domain.Description
import finance.services.DescriptionService
import finance.services.TransactionService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import java.util.*
import javax.inject.Inject

@Controller("/description")
class DescriptionController(@Inject val descriptionService: DescriptionService) {

    //https://hornsup:8080/description/select/all
    @Get( "/select/all", produces = ["application/json"])
    fun selectAllDescriptions(): HttpResponse<List<Description>> {
        val descriptions = descriptionService.fetchAllDescriptions()

        return HttpResponse.ok(descriptions)
    }

    //curl -k --header "Content-Type: application/json" -X POST -d '{"description":"test", "activeStatus":true}' 'https://hornsup:8080/description/insert'
    @Post( "/insert", produces = ["application/json"])
    fun insertDescription(@Body description: Description): HttpResponse<String> {
        descriptionService.insertDescription(description)
        //BaseController.logger.info("description inserted")
        return HttpResponse.ok("description inserted")
    }

    //curl -k 'https://localhost:8080/description/select/zzz'
    @Get("/select/{description_name}")
    fun selectDescriptionName(@PathVariable("description_name") descriptionName: String): HttpResponse<String> {
        val descriptionOptional = descriptionService.findByDescriptionName(descriptionName)
        if (descriptionOptional.isPresent) {
            return HttpResponse.ok(BaseController.mapper.writeValueAsString(descriptionOptional.get()))
        }
        return HttpResponse.notFound("description not found for: $descriptionName")
    //throw ResponseStatusException(HttpStatus.NOT_FOUND, "description not found for: $descriptionName")
    }

    @Delete("/delete/{descriptionName}", produces = ["application/json"])
    fun deleteByDescription(@PathVariable descriptionName: String): HttpResponse<String> {
        val descriptionOptional: Optional<Description> = descriptionService.findByDescriptionName(descriptionName)

        if (descriptionOptional.isPresent) {
            descriptionService.deleteByDescriptionName(descriptionName)
            return HttpResponse.ok("description deleted")
        }

        return HttpResponse.badRequest("could not delete the description: $descriptionName.")
        //throw ResponseStatusException(HttpStatus.BAD_REQUEST, "could not delete the description: $descriptionName.")
    }
}