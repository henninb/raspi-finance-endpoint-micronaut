package finance.controllers

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import java.time.Instant
import java.util.UUID

@Controller("/api/uuid")
class UuidController : BaseController() {

    @Post(value = "/generate", produces = [MediaType.APPLICATION_JSON])
    fun generateUuid(): HttpResponse<Map<String, Any>> {
        logger.info("UUID generation requested")
        return try {
            val uuid = UUID.randomUUID().toString()
            val response: Map<String, Any> = mapOf(
                "uuid" to uuid,
                "timestamp" to Instant.now().toEpochMilli(),
                "source" to "server",
            )
            logger.debug("Generated UUID: {}", uuid)
            HttpResponse.ok(response)
        } catch (e: Exception) {
            logger.error("Error generating UUID", e)
            HttpResponse.serverError(mapOf("error" to "Failed to generate UUID"))
        }
    }

    @Post(value = "/generate/batch", produces = [MediaType.APPLICATION_JSON])
    fun generateBatchUuids(@QueryValue(defaultValue = "1") count: Int): HttpResponse<Map<String, Any>> {
        logger.info("Batch UUID generation requested for {} UUIDs", count)
        if (count <= 0 || count > 100) {
            logger.warn("Invalid UUID count requested: {}", count)
            return HttpResponse.badRequest(mapOf("error" to "Count must be between 1 and 100"))
        }
        return try {
            val uuids = (1..count).map { UUID.randomUUID().toString() }
            val response: Map<String, Any> = mapOf(
                "uuids" to uuids,
                "count" to uuids.size,
                "timestamp" to Instant.now().toEpochMilli(),
                "source" to "server",
            )
            logger.debug("Generated {} UUIDs", uuids.size)
            HttpResponse.ok(response)
        } catch (e: Exception) {
            logger.error("Error generating batch UUIDs", e)
            HttpResponse.serverError(mapOf("error" to "Failed to generate UUIDs"))
        }
    }

    @Post(value = "/health", produces = [MediaType.APPLICATION_JSON])
    fun healthCheck(): HttpResponse<Map<String, Any>> =
        HttpResponse.ok(
            mapOf(
                "status" to "healthy",
                "service" to "uuid-generation",
                "timestamp" to Instant.now().toEpochMilli(),
            )
        )
}
