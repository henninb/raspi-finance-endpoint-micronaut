package finance.services

import finance.domain.Account
import finance.domain.TransactionState
import java.math.BigDecimal
import java.util.*

interface IAccountService {

    fun account(accountNameOwner: String): Optional<Account>
    //fun findByActiveStatusAndAccountTypeAndTotalsIsGreaterThanOrderByAccountNameOwner(): List<Account>
    fun accounts(): List<Account>
    fun findAccountsThatRequirePayment(): List<String>
    fun sumOfAllTransactionsByTransactionState(transactionState: TransactionState): BigDecimal
    fun insertAccount(account: Account): Boolean
    fun deleteAccount(accountNameOwner: String): Boolean
    fun updateTotalsForAllAccounts(): Boolean
    fun updateAccount(account: Account): Boolean
    fun renameAccountNameOwner(oldAccountNameOwner: String, newAccountNameOwner: String): Boolean
}