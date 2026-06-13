package finance.controllers

import finance.domain.Description
import finance.domain.MergeDescriptionsRequest
import finance.services.DescriptionService
import finance.services.OwnerExtractorService
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import jakarta.inject.Inject
import java.util.*

@Controller("/api/description")
class DescriptionController(
    @Inject val descriptionService: DescriptionService,
    @Inject val ownerExtractorService: OwnerExtractorService,
) {

    @Get("/active", produces = ["application/json"])
    fun selectAllDescriptions(request: HttpRequest<*>): HttpResponse<List<Description>> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        return HttpResponse.ok(descriptionService.fetchAllDescriptions(owner))
    }

    @Get("/{descriptionName}", produces = ["application/json"])
    fun selectDescriptionName(@PathVariable descriptionName: String, request: HttpRequest<*>): HttpResponse<Description> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        val optional = descriptionService.findByOwnerAndDescriptionName(owner, descriptionName)
        return if (optional.isPresent) HttpResponse.ok(optional.get()) else HttpResponse.notFound()
    }

    @Post(produces = ["application/json"])
    fun insertDescription(@Body description: Description, request: HttpRequest<*>): HttpResponse<Description> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        if (description.owner.isBlank()) description.owner = owner
        descriptionService.insertDescription(description)
        return HttpResponse.status<Description>(HttpStatus.CREATED).body(description)
    }

    @Put("/{descriptionName}", consumes = ["application/json"], produces = ["application/json"])
    fun updateDescription(@PathVariable descriptionName: String, @Body description: Description, request: HttpRequest<*>): HttpResponse<Description> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        description.descriptionName = descriptionName
        if (description.owner.isBlank()) description.owner = owner
        return if (descriptionService.updateDescription(description)) HttpResponse.ok(description) else HttpResponse.notFound()
    }

    @Delete("/{descriptionName}", produces = ["application/json"])
    fun deleteByDescriptionName(@PathVariable descriptionName: String, request: HttpRequest<*>): HttpResponse<Description> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        val optional: Optional<Description> = descriptionService.findByOwnerAndDescriptionName(owner, descriptionName)
        if (optional.isPresent) {
            descriptionService.deleteByDescriptionName(owner, descriptionName)
            return HttpResponse.ok(optional.get())
        }
        return HttpResponse.notFound()
    }

    @Post("/merge", consumes = ["application/json"], produces = ["application/json"])
    fun mergeDescriptions(@Body mergeRequest: MergeDescriptionsRequest, request: HttpRequest<*>): HttpResponse<Description> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        if (mergeRequest.targetName.isBlank() || mergeRequest.sourceNames.isEmpty()) return HttpResponse.badRequest()
        return try {
            descriptionService.mergeDescriptions(mergeRequest.targetName, mergeRequest.sourceNames)
            val result = descriptionService.findByOwnerAndDescriptionName(owner, mergeRequest.targetName)
            if (result.isPresent) HttpResponse.ok(result.get()) else HttpResponse.badRequest()
        } catch (e: RuntimeException) {
            HttpResponse.badRequest()
        }
    }
}
