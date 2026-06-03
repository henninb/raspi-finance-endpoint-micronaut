package finance.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Account
import finance.services.AccountService
import finance.services.OwnerExtractorService
import finance.services.TransactionService
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import jakarta.inject.Inject
import java.util.*

@Controller("/api/account")
class AccountController(
    @Inject val accountService: AccountService,
    @Inject val ownerExtractorService: OwnerExtractorService,
    @Inject val transactionService: TransactionService,
) {

    @Get(value = "/active", produces = ["application/json"])
    fun selectAllActiveAccounts(): HttpResponse<List<Account>> {
        accountService.updateTheGrandTotalForAllClearedTransactions()
        val accounts = accountService.findByActiveStatusOrderByAccountNameOwner()
        return if (accounts.isEmpty()) HttpResponse.notFound() else HttpResponse.ok(accounts)
    }

    @Get(value = "/{accountNameOwner}", produces = ["application/json"])
    fun selectByAccountNameOwner(@PathVariable accountNameOwner: String): HttpResponse<Account> {
        val optional: Optional<Account> = accountService.findByAccountNameOwner(accountNameOwner)
        return if (optional.isPresent) HttpResponse.ok(optional.get()) else HttpResponse.notFound()
    }

    @Post(produces = ["application/json"])
    fun insertAccount(@Body account: Account, request: HttpRequest<*>): HttpResponse<Account> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        if (account.owner.isNullOrBlank()) account.owner = owner
        accountService.insertAccount(account)
        return HttpResponse.status<Account>(HttpStatus.CREATED).body(account)
    }

    @Put(value = "/{accountNameOwner}", produces = ["application/json"])
    fun updateAccount(
        @PathVariable accountNameOwner: String,
        @Body account: Map<String, Any>,
        request: HttpRequest<*>
    ): HttpResponse<Account> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        val accountToBeUpdated = mapper.convertValue(account, Account::class.java)
        if (accountToBeUpdated.owner.isNullOrBlank()) accountToBeUpdated.owner = owner
        return if (accountService.updateAccount(accountToBeUpdated)) HttpResponse.ok(accountToBeUpdated)
        else HttpResponse.badRequest()
    }

    @Delete(value = "/{accountNameOwner}", produces = ["application/json"])
    fun deleteByAccountNameOwner(@PathVariable accountNameOwner: String): HttpResponse<Account> {
        val optional: Optional<Account> = accountService.findByAccountNameOwner(accountNameOwner)
        if (optional.isPresent) {
            accountService.deleteByAccountNameOwner(accountNameOwner)
            return HttpResponse.ok(optional.get())
        }
        return HttpResponse.badRequest()
    }

    @Get(value = "/totals", produces = ["application/json"])
    fun computeAccountTotals(): Map<String, String> = mapOf(
        "totals" to accountService.computeTheGrandTotalForAllTransactions().toString(),
        "totalsCleared" to accountService.computeTheGrandTotalForAllClearedTransactions().toString()
    )

    @Get(value = "/validation/refresh", produces = ["application/json"])
    fun refreshValidationDates(): HttpResponse<Void> {
        accountService.refreshValidationDates()
        return HttpResponse.ok()
    }

    @Get(value = "/payment/required", produces = ["application/json"])
    fun selectPaymentRequired(): HttpResponse<List<Account>> {
        val accounts = transactionService.findAccountsThatRequirePayment()
        return if (accounts.isEmpty()) HttpResponse.notFound() else HttpResponse.ok(accounts)
    }

    @Put(value = "/rename", produces = [MediaType.APPLICATION_JSON])
    fun renameAccountNameOwner(
        @QueryValue("old") oldAccountNameOwner: String,
        @QueryValue("new") newAccountNameOwner: String
    ): HttpResponse<Account> {
        if (accountService.renameAccountNameOwner(oldAccountNameOwner, newAccountNameOwner)) {
            val optional = accountService.findByAccountNameOwner(newAccountNameOwner)
            return if (optional.isPresent) HttpResponse.ok(optional.get()) else HttpResponse.badRequest()
        }
        return HttpResponse.badRequest()
    }

    @Put(value = "/deactivate/{accountNameOwner}", produces = ["application/json"])
    fun deactivateAccount(@PathVariable accountNameOwner: String): HttpResponse<Account> {
        return try { HttpResponse.ok(accountService.deactivateAccount(accountNameOwner)) }
        catch (e: RuntimeException) { HttpResponse.notFound() }
    }

    @Put(value = "/activate/{accountNameOwner}", produces = ["application/json"])
    fun activateAccount(@PathVariable accountNameOwner: String): HttpResponse<Account> {
        return try { HttpResponse.ok(accountService.activateAccount(accountNameOwner)) }
        catch (e: RuntimeException) { HttpResponse.notFound() }
    }

    companion object {
        private val mapper = ObjectMapper().apply {
            findAndRegisterModules()
            disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}
