package finance.controllers

import finance.domain.Description
import finance.domain.MergeDescriptionsRequest
import finance.services.DescriptionService
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import jakarta.inject.Inject
import java.util.*

@Controller("/api/description")
class DescriptionController(@Inject val descriptionService: DescriptionService) {

    @Get("/active", produces = ["application/json"])
    fun selectAllDescriptions(): HttpResponse<List<Description>> =
        HttpResponse.ok(descriptionService.fetchAllDescriptions())

    @Get("/{descriptionName}", produces = ["application/json"])
    fun selectDescriptionName(@PathVariable descriptionName: String): HttpResponse<Description> {
        val optional = descriptionService.findByDescriptionName(descriptionName)
        return if (optional.isPresent) HttpResponse.ok(optional.get()) else HttpResponse.notFound()
    }

    @Post(produces = ["application/json"])
    fun insertDescription(@Body description: Description): HttpResponse<Description> {
        descriptionService.insertDescription(description)
        return HttpResponse.status<Description>(HttpStatus.CREATED).body(description)
    }

    @Put("/{descriptionName}", consumes = ["application/json"], produces = ["application/json"])
    fun updateDescription(@PathVariable descriptionName: String, @Body description: Description): HttpResponse<Description> {
        description.descriptionName = descriptionName
        return if (descriptionService.updateDescription(description)) HttpResponse.ok(description) else HttpResponse.notFound()
    }

    @Delete("/{descriptionName}", produces = ["application/json"])
    fun deleteByDescriptionName(@PathVariable descriptionName: String): HttpResponse<Description> {
        val optional: Optional<Description> = descriptionService.findByDescriptionName(descriptionName)
        if (optional.isPresent) {
            descriptionService.deleteByDescriptionName(descriptionName)
            return HttpResponse.ok(optional.get())
        }
        return HttpResponse.badRequest()
    }

    @Post("/merge", consumes = ["application/json"], produces = ["application/json"])
    fun mergeDescriptions(@Body request: MergeDescriptionsRequest): HttpResponse<Description> {
        if (request.targetName.isBlank() || request.sourceNames.isEmpty()) return HttpResponse.badRequest()
        return try {
            descriptionService.mergeDescriptions(request.targetName, request.sourceNames)
            val result = descriptionService.findByDescriptionName(request.targetName)
            if (result.isPresent) HttpResponse.ok(result.get()) else HttpResponse.badRequest()
        } catch (e: RuntimeException) {
            HttpResponse.badRequest()
        }
    }
}
