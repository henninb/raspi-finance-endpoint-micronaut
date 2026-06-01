package finance.controllers

import finance.domain.Description
import finance.domain.MergeDescriptionsRequest
import finance.services.DescriptionService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import java.util.*
import jakarta.inject.Inject

@Controller("/description")
class DescriptionController(@Inject val descriptionService: DescriptionService) {

    @Get("/select/all", produces = ["application/json"])
    fun selectAllDescriptions(): HttpResponse<List<Description>> {
        val descriptions = descriptionService.fetchAllDescriptions()
        return HttpResponse.ok(descriptions)
    }

    @Post("/insert", produces = ["application/json"])
    fun insertDescription(@Body description: Description): HttpResponse<String> {
        descriptionService.insertDescription(description)
        return HttpResponse.ok("description inserted")
    }

    @Get("/select/{descriptionName}")
    fun selectDescriptionName(@PathVariable descriptionName: String): HttpResponse<String> {
        val descriptionOptional = descriptionService.findByDescriptionName(descriptionName)
        if (descriptionOptional.isPresent) {
            return HttpResponse.ok(BaseController.mapper.writeValueAsString(descriptionOptional.get()))
        }
        return HttpResponse.notFound("description not found for: $descriptionName")
    }

    @Delete("/delete/{descriptionName}", produces = ["application/json"])
    fun deleteByDescriptionName(@PathVariable descriptionName: String): HttpResponse<String> {
        val descriptionOptional: Optional<Description> = descriptionService.findByDescriptionName(descriptionName)
        if (descriptionOptional.isPresent) {
            descriptionService.deleteByDescriptionName(descriptionName)
            return HttpResponse.ok("description deleted")
        }
        return HttpResponse.badRequest("could not delete the description: $descriptionName.")
    }

    @Put("/update/{descriptionName}", consumes = ["application/json"], produces = ["application/json"])
    fun updateDescription(@PathVariable descriptionName: String, @Body description: Description): HttpResponse<String> {
        description.descriptionName = descriptionName
        val updated = descriptionService.updateDescription(description)
        return if (updated) HttpResponse.ok("description updated") else HttpResponse.notFound()
    }

    @Post("/merge", consumes = ["application/json"], produces = ["application/json"])
    fun mergeDescriptions(@Body request: MergeDescriptionsRequest): HttpResponse<String> {
        return try {
            if (request.targetName.isBlank() || request.sourceNames.isEmpty()) {
                return HttpResponse.badRequest("targetName and sourceNames are required")
            }
            descriptionService.mergeDescriptions(request.targetName, request.sourceNames)
            HttpResponse.ok("descriptions merged")
        } catch (e: RuntimeException) {
            HttpResponse.badRequest("could not merge descriptions: ${e.message}")
        }
    }
}
