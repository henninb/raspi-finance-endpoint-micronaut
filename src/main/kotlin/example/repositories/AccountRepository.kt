package example.repositories

import example.domain.Account
import io.micronaut.data.annotation.Repository
import io.micronaut.data.repository.CrudRepository
import java.util.*

@Repository
interface AccountRepository : CrudRepository<Account, Long> {
    //fun saveAndFlush(account: Account)
    fun findByAccountNameOwner(accountNameOwner: String): Optional<Account>
}
