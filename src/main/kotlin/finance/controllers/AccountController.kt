package finance.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Account
import finance.services.AccountService
import io.micronaut.context.annotation.Parameter
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import jakarta.inject.Inject
import java.util.*

@Controller("/account")
class AccountController(@Inject val accountService: AccountService) {

    //http://localhost:8080/account/totals
    @Get(value = "totals", produces = ["application/json"])
    fun computeAccountTotals(): Map<String, String> {
        val response: MutableMap<String, String> = HashMap()
        response["totals"] = accountService.computeTheGrandTotalForAllTransactions().toString()
        response["totalsCleared"] = accountService.computeTheGrandTotalForAllClearedTransactions().toString()
        return response
    }

    //curl --header "Content-Type: application/json" https://hornsup:8080/account/payment/required
    @Get(value = "/payment/required", produces = ["application/json"])
    fun selectPaymentRequired(): HttpResponse<List<String>> {

        val accountNameOwners = accountService.findAccountsThatRequirePayment()
        if (accountNameOwners.isEmpty()) {
            return HttpResponse.notFound()
            //logger.info("no accountNameOwners found.")
            //throw ResponseStatusException(HttpStatus.NOT_FOUND, "could not find any accountNameOwners.")
        }
        return HttpResponse.ok(accountNameOwners)
    }

    //http://localhost:8080/account/select/active
    @Get(value = "/select/active", produces = ["application/json"])
    fun selectAllActiveAccounts(): HttpResponse<List<Account>> {
        //TODO: create a separate endpoint for the totals
        accountService.updateTheGrandTotalForAllClearedTransactions()
        val accounts: List<Account> = accountService.findByActiveStatusOrderByAccountNameOwner()
        if (accounts.isEmpty()) {
            //BaseController.logger.info("no accounts found.")
            //throw ResponseStatusException(HttpStatus.NOT_FOUND, "could not find any accounts.")
            //TODO: micronaut - need to fix
            return HttpResponse.notFound()

        }
        //BaseController.logger.info("select active accounts: ${accounts.size}")
        return HttpResponse.ok(accounts)
    }

    //http://localhost:8080/account/select/test_brian
    @Get(value = "/select/{accountNameOwner}", produces = ["application/json"])
    fun selectByAccountNameOwner(@PathVariable accountNameOwner: String): HttpResponse<Account> {
        val accountOptional: Optional<Account> = accountService.findByAccountNameOwner(accountNameOwner)
        if (accountOptional.isPresent) {
            return HttpResponse.ok(accountOptional.get())
        }

        return HttpResponse.notFound()
    }

    //curl -k --header "Content-Type: application/json" --request POST --data '{"accountNameOwner":"test_brian", "accountType": "credit", "activeStatus": "true","moniker": "0000", "totals": 0.00, "totalsBalanced": 0.00, "dateClosed": 0, "dateUpdated": 0, "dateAdded": 0}' 'https://localhost:8080/account/insert'
    @Post(value = "/insert", produces = ["application/json"])
    fun insertAccount(@Body account: Account): HttpResponse<String> {
        accountService.insertAccount(account)
        return HttpResponse.ok("account inserted")
    }

    //curl -k --header "Content-Type: application/json" --request DELETE 'https://localhost:8080/account/delete/test_brian'
    @Delete(value = "/delete/{accountNameOwner}", produces = ["application/json"])
    fun deleteByAccountNameOwner(@PathVariable accountNameOwner: String): HttpResponse<String> {
        val accountOptional: Optional<Account> = accountService.findByAccountNameOwner(accountNameOwner)

        if (accountOptional.isPresent) {
            accountService.deleteByAccountNameOwner(accountNameOwner)
            return HttpResponse.ok("account deleted")
        }
        return HttpResponse.badRequest("could not delete this account: $accountNameOwner.")
        //throw ResponseStatusException(HttpStatus.BAD_REQUEST, "could not delete this account: $accountNameOwner.")
    }

    //curl -k --header "Content-Type: application/json" --request PUT 'https://localhost:8080/account/update/test_account' --data '{}'
    @Put(value = "/update/{accountNameOwner}", produces = ["application/json"])
    fun updateAccount(
            @PathVariable("accountNameOwner") guid: String,
            @Body account: Map<String, Any>
    ): HttpResponse<String> {
        val accountToBeUpdated = BaseController.mapper.convertValue(account, Account::class.java)
        val updateStatus: Boolean = accountService.updateAccount(accountToBeUpdated)
        if (updateStatus) {
            return HttpResponse.ok("account updated")
        }

        return HttpResponse.badRequest("could not update this account: ${accountToBeUpdated.accountNameOwner}.")
    }

    //curl -k -X PUT 'https://hornsup:8080/account/rename?old=gap_kari&new=oldnavy_kari'
    //curl -k --header "Content-Type: application/json" --request PUT 'https://hornsup:8080/account/rename?old=test_brian&new=testnew_brian'
    @Get
    @Produces(MediaType.APPLICATION_JSON)
    fun renameAccountNameOwner(
            @Parameter(value = "old") oldAccountNameOwner: String,
            @Parameter(value = "new") newAccountNameOwner: String
    ): HttpResponse<String> {
        val updateStatus: Boolean = accountService.renameAccountNameOwner(oldAccountNameOwner, newAccountNameOwner)
        if (updateStatus) {
            return HttpResponse.ok("accountNameOwner renamed")
        }

        return HttpResponse.badRequest("could not rename this account: ${oldAccountNameOwner}.")
    }

    companion object {
        private val mapper = ObjectMapper()
    }
}