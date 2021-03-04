package finance.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Account
import finance.services.AccountService
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject

@Controller("/account")
class AccountController(@Inject val accountService: AccountService) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Get("/totals")
    @Produces(MediaType.APPLICATION_JSON)
    fun selectTotals(): Map<String, String> {
        val response: MutableMap<String, String> = HashMap()
        //TODO: FIX these calls.
        //response["totals"] = accountService.selectTotals().toString()
        //response["totalsCleared"] = accountService.selectTotalsCleared().toString()

        response["totals"] = "0.00"
        response["totalsCleared"] = "0.00"

        return response
    }

    @Get("/select/active")
    @Produces(MediaType.APPLICATION_JSON)
    fun selectAllActiveAccounts(): HttpResponse<List<Account>> {
        val accounts: List<Account> = accountService.findByActiveStatusOrderByAccountNameOwner()
        if (accounts.isEmpty()) {
            logger.info("no accounts found.")
            return HttpResponse.notFound()
            //TODO: not found
        }
        logger.info("select active accounts: ${accounts.size}")
        return HttpResponse.ok(accounts)
    }

//    @Get("/select/totals")
//    @Produces(MediaType.APPLICATION_JSON)
//    fun selectAccountTotals(): HttpResponse<List<Account>> {
//        //TODO: fix
//        //accountService.updateAccountTotals()
//        val accounts: List<Account> = accountService.findAllActiveAccounts()
//        if (accounts.isEmpty()) {
//            logger.info("no accounts found.")
//            return HttpResponse.notFound()
//        }
//        logger.info("select active accounts: ${accounts.size}")
//        return HttpResponse.ok(accounts)
//    }

    @Get("/select/{accountNameOwner}")
    @Produces(MediaType.APPLICATION_JSON)
    fun selectByAccountNameOwner(@PathVariable accountNameOwner: String): HttpResponse<Account> {
        val accountOptional: Optional<Account> = accountService.findByAccountNameOwner(accountNameOwner)
        if (accountOptional.isPresent) {
            return HttpResponse.ok(accountOptional.get())
        }
        return HttpResponse.notFound()
    }

    //curl --header "Content-Type: application/json" --request POST --data '{"accountNameOwner":"test_brian", "accountType": "credit", "activeStatus": "true","moniker": "0000"' http://localhost:8080/insert_account
    //curl --header "Content-Type: application/json" --request POST --data '{"accountNameOwner":"test_brian", "accountType": "credit", "activeStatus": "true","moniker": "0000", "totals": 0.00, "totalsBalanced": 0.00, "dateClosed": 0, "dateUpdated": 0, "dateAdded": 0}' http://localhost:8080/insert_account
    //http://localhost:8080/insert_account
    @Post("/insert")
    fun insertAccount(@Body account: Account): HttpResponse<String> {
        accountService.insertAccount(account)
        return HttpResponse.ok("account inserted")
    }

    //http://localhost:8080/delete_account/amex_brian
    //curl --header "Content-Type: application/json" --request DELETE http://localhost:8080/delete_account/test_brian
    @Delete("/delete/{accountNameOwner}")
    fun deleteByAccountNameOwner(@PathVariable accountNameOwner: String): HttpResponse<String> {
        val accountOptional: Optional<Account> = accountService.findByAccountNameOwner(accountNameOwner)

        if (accountOptional.isPresent) {
            accountService.deleteByAccountNameOwner(accountNameOwner)
            return HttpResponse.ok("account deleted")
        }
        //throw EmptyAccountException("account not deleted.")
        return HttpResponse.notModified()
    }

//    @Patch("/update")
//    fun updateTransaction(@Body account: Map<String, String>): HttpResponse<String> {
//        val toBePatchedTransaction = mapper.convertValue(account, Account::class.java)
//        val updateStatus: Boolean = accountService.patchAccount(toBePatchedTransaction)
//        if (updateStatus) {
//            return HttpResponse.ok("account updated")
//        }
//
//        return HttpResponse.notModified()
//        //throw EmptyAccountException("account not updated.")
//    }

//    @ResponseStatus(HttpStatus.BAD_REQUEST) //400
//    @ExceptionHandler(value = [ConstraintViolationException::class, NumberFormatException::class, MethodArgumentTypeMismatchException::class, HttpMessageNotReadableException::class])
//    fun handleBadHttpRequests(throwable: Throwable): Map<String, String>? {
//        val response: MutableMap<String, String> = HashMap()
//        logger.error("Bad Request", throwable)
//        response["response"] = "BAD_REQUEST: " + throwable.javaClass.simpleName + " , message: " + throwable.message
//        return response
//    }

//    @ResponseStatus(HttpStatus.NOT_FOUND)
//    @ExceptionHandler(value = [EmptyAccountException::class])
//    fun handleHttpNotFound(throwable: Throwable): Map<String, String> {
//        val response: MutableMap<String, String> = HashMap()
//        logger.error("not found: ", throwable)
//        response["response"] = "NOT_FOUND: " + throwable.javaClass.simpleName + " , message: " + throwable.message
//        return response
//    }

    companion object {
        private val mapper = ObjectMapper()
    }
}