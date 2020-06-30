package example.controllers

import example.repositories.PersonRepository
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import javax.inject.Inject

@Controller("/transactions")
class TransactionController (@Inject val personRepository: PersonRepository) {

    val payload = """
        [{"test":123}]
    """.trimIndent()

    @Get("/person")
    @Produces(MediaType.TEXT_PLAIN)
    fun index(): String {
        val person = personRepository.findById(1L)
        return person.get().name
    }

    @Get("/test")
    @Produces(MediaType.APPLICATION_JSON)
    fun findAll(): String {
        return payload
    }
}