package finance.repositories

import finance.domain.Account
import finance.domain.AccountType
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import java.math.BigDecimal
import java.util.*
import jakarta.transaction.Transactional

@Repository
interface AccountRepository : JpaRepository<Account, Long> {
    fun findByAccountNameOwner(accountNameOwner: String): Optional<Account>
    fun findByOwnerAndAccountNameOwner(owner: String, accountNameOwner: String): Optional<Account>
    fun findByActiveStatusOrderByAccountNameOwner(activeStatus: Boolean = true): List<Account>
    fun findByOwnerAndActiveStatusOrderByAccountNameOwner(owner: String, activeStatus: Boolean = true): List<Account>
    fun findByOwnerAndActiveStatusOrderByAccountNameOwner(owner: String, activeStatus: Boolean = true, pageable: Pageable): Page<Account>

    @Query(
        value = "SELECT * FROM t_account WHERE account_type = 'credit' AND active_status = true AND (outstanding > 0 OR future > 0 OR cleared > 0) ORDER BY account_name_owner",
        nativeQuery = true
    )
    fun findByActiveStatusAndAccountTypeAndTotalsIsGreaterThanOrderByAccountNameOwner(
        activeStatus: Boolean = true,
        accountType: AccountType = AccountType.Credit,
        totals: BigDecimal = BigDecimal(0.0)
    ): List<Account>

    @Transactional
    fun deleteByAccountNameOwner(accountNameOwner: String)

    @Transactional
    @Query(
        value = "UPDATE t_account SET cleared = x.cleared, outstanding = x.outstanding, future = x.future, date_updated = now() FROM " +
            "(SELECT account_name_owner, " +
            "SUM(CASE WHEN transaction_state = 'cleared' THEN amount ELSE 0 END) AS cleared, " +
            "SUM(CASE WHEN transaction_state = 'outstanding' THEN amount ELSE 0 END) AS outstanding, " +
            "SUM(CASE WHEN transaction_state = 'future' THEN amount ELSE 0 END) AS future " +
            "FROM t_transaction WHERE active_status = true GROUP BY account_name_owner) AS x " +
            "WHERE t_account.account_name_owner = x.account_name_owner",
        nativeQuery = true
    )
    fun updateTotalsForAllAccounts()

    @Query(
        value = "SELECT COALESCE((A.debits - B.credits), 0.0) FROM ( SELECT SUM(amount) AS debits FROM t_transaction WHERE account_type = 'debit' AND active_status = true) A,( SELECT SUM(amount) AS credits FROM t_transaction WHERE account_type = 'credit' AND active_status = true) B",
        nativeQuery = true
    )
    fun computeTheGrandTotalForAllTransactions(): BigDecimal

    @Query(
        value = "SELECT COALESCE((A.debits - B.credits), 0.0) FROM ( SELECT SUM(amount) AS debits FROM t_transaction WHERE account_type = 'debit' AND transaction_state = 'cleared' AND active_status = true) A,( SELECT SUM(amount) AS credits FROM t_transaction WHERE account_type = 'credit' and transaction_state = 'cleared' AND active_status = true) B",
        nativeQuery = true
    )
    fun computeTheGrandTotalForAllClearedTransactions(): BigDecimal

    @Query(
        value = "SELECT account_name_owner FROM t_transaction WHERE transaction_state = 'cleared' and account_name_owner in (select account_name_owner from t_account where account_type = 'credit' and active_status = true) or (transaction_state = 'outstanding' and account_type = 'credit' and description ='payment') group by account_name_owner having sum(amount) > 0 order by account_name_owner",
        nativeQuery = true
    )
    fun findAccountsThatRequirePayment(): List<String>

    @Transactional
    @Query(
        value = "UPDATE t_account SET validation_date = NOW(), date_updated = NOW() WHERE active_status = true",
        nativeQuery = true
    )
    fun updateValidationDateForAllAccounts()

    @Transactional
    @Query(
        value = "UPDATE t_account SET validation_date = NOW(), date_updated = NOW() WHERE account_id = :accountId",
        nativeQuery = true
    )
    fun updateValidationDateByAccountId(accountId: Long)
}
