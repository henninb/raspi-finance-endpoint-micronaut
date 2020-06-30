package example.repositories

import example.domain.Person
import io.micronaut.data.annotation.Repository
import io.micronaut.data.repository.CrudRepository

@Repository
interface PersonRepository : CrudRepository<Person, Long> {
}