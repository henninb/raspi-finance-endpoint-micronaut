package example.repositories

import example.domain.Account
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.repository.CrudRepository
import java.util.*

@Repository
interface AccountRepository : CrudRepository<Account, Long> {
    fun findByAccountNameOwner(accountNameOwner: String): Optional<Account>

    fun findByActiveStatusOrderByAccountNameOwner(activeStatus: Boolean): List<Account>

    fun deleteByAccountNameOwner(accountNameOwner: String)

    @Query(value = "UPDATE t_account SET totals = x.totals FROM (SELECT account_name_owner, SUM(amount) AS totals FROM t_transaction GROUP BY account_name_owner) x WHERE t_account.account_name_owner = x.account_name_owner", nativeQuery = true)
    fun updateAccountClearedTotals()

    @Query(value = "UPDATE t_account SET totals_balanced = x.totals_balanced FROM (SELECT account_name_owner, SUM(amount) AS totals_balanced FROM t_transaction WHERE cleared = 1 GROUP BY account_name_owner) x WHERE t_account.account_name_owner = x.account_name_owner", nativeQuery = true)
    fun updateAccountGrandTotals()

    @Query(value = "SELECT (A.debits - B.credits) FROM ( SELECT SUM(amount) AS debits FROM t_transaction WHERE account_type = 'debit' ) A,( SELECT SUM(amount) AS credits FROM t_transaction WHERE account_type = 'credit' ) B", nativeQuery = true)
    fun selectTotals(): Double

    @Query(value = "SELECT (A.debits - B.credits) FROM ( SELECT SUM(amount) AS debits FROM t_transaction WHERE account_type = 'debit' and cleared = 1) A,( SELECT SUM(amount) AS credits FROM t_transaction WHERE account_type = 'credit' and cleared = 1 ) B", nativeQuery = true)
    fun selectTotalsCleared(): Double

}
