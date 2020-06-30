package example.repositories

import example.domain.Account
import io.micronaut.data.annotation.Repository
import io.micronaut.data.repository.CrudRepository


//@JdbcRepository(dialect = Dialect.H2)
@Repository
interface AccountRepository : CrudRepository<Account, Long> {
    //fun findByAccountNameOwner(accountNameOwner: String): Optional<Account>
}
