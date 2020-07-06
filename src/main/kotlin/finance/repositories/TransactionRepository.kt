package finance.repositories

import finance.domain.Transaction
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.repository.CrudRepository
import java.math.BigDecimal
import java.util.*

@Repository
interface TransactionRepository : CrudRepository<Transaction, Long> {

    //TODO: add LIMIT 1 result
    fun findByGuid(guid: String): Optional<Transaction>
    fun findByAccountNameOwnerIgnoreCaseOrderByTransactionDateDesc(accountNameOwner: String): List<Transaction>

    @Query("UPDATE #{#entityName} set amount = ?1 WHERE guid = ?2")
    fun setAmountByGuid(amount: BigDecimal, guild: String)

    @Query("UPDATE #{#entityName} set cleared = ?1 WHERE guid = ?2")
    fun setClearedByGuid(cleared: Int, guild: String)

    @Query("SELECT SUM(amount) as totalsCleared FROM #{#entityName} WHERE cleared = 1 AND accountNameOwner=?1")
    //@Query(value = "SELECT SUM(amount) AS totals t_transaction WHERE cleared = 1 AND account_name_owner=?1", nativeQuery = true)
    fun getTotalsByAccountNameOwnerCleared(accountNameOwner: String): Double

    // Using SpEL expression
    @Query("SELECT SUM(amount) as totals FROM #{#entityName} WHERE accountNameOwner=?1")
    fun getTotalsByAccountNameOwner(accountNameOwner: String): Double

    //@Query(value = "DELETE FROM t_transaction WHERE guid = ?1", nativeQuery = true)
    fun deleteByGuid(guid: String)

    @Query(value = "DELETE FROM t_transaction_categories WHERE transaction_id = ?1", nativeQuery = true)
    fun deleteByIdFromTransactionCategories(transactionId: Long)

    @Query(value = "SELECT * FROM t_transaction_categories WHERE transaction_id =?", nativeQuery = true)
    fun selectFromTransactionCategories(transactionId: Long): List<Long>
}
