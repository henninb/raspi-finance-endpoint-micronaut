package example.controllers

import example.domain.Account
import example.domain.AccountType
import example.domain.Category
import example.domain.Transaction
import example.services.AccountService
import example.services.CategoryService
import example.services.TransactionService
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Date
import java.sql.Timestamp
import javax.inject.Inject

@Controller("/transactions")
class TransactionController(@Inject val transactionService: TransactionService,
@Inject val categoryService: CategoryService,
                            @Inject val accountService: AccountService) {

    val payload = """
        [{"test":123}]
    """.trimIndent()

    @Get("/person")
    @Produces(MediaType.TEXT_PLAIN)
    fun index(): String {
        var transaction = Transaction()
        var category = Category()
        var account = Account()

        //transaction.transactionId = 1002
        transaction.guid = "4ea3be58-3993-46de-88a2-4ffc7f1d73bd"
        //transaction.accountId = 1
        transaction.accountType = AccountType.Credit
        transaction.accountNameOwner = "foo_brian"
        transaction.transactionDate = Date(1553645394)
        transaction.description = "aliexpress.com"
        transaction.category = "online"
        transaction.amount = BigDecimal(3.14).setScale(2, RoundingMode.HALF_UP)
        transaction.cleared = 1
        transaction.reoccurring = false
        transaction.notes = ""
        transaction.dateUpdated = Timestamp(1553645394000)
        transaction.dateAdded = Timestamp(1553645394000)
        transaction.sha256 = ""
        transactionService.insertTransaction(transaction)

        category.category = "foobar"
        categoryService.insertCategory(category)

        account.accountNameOwner = "test_brian"
        account.accountType = AccountType.Credit
        account.activeStatus = true
        account.moniker = "1234"
        account.totals = BigDecimal("0.0")
        account.totalsBalanced = BigDecimal("0.0")
        account.dateClosed = Timestamp(0)
        account.dateUpdated = Timestamp(1553645394000)
        account.dateAdded = Timestamp(1553645394000)

        accountService.insertAccount(account)

        return "test"
    }

    @Get("/test")
    @Produces(MediaType.APPLICATION_JSON)
    fun findAll(): String {
        return payload
    }
}