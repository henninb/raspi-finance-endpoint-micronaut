package finance.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Transaction
import finance.services.TransactionService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject

@Controller("/transaction")
class TransactionController(@Inject val transactionService: TransactionService) {
//    private val logger = LoggerFactory.getLogger(this.javaClass)
//
//    //curl http://localhost:8080/transaction/account/select/usbankcash_brian
//    @Get("/account/select/{accountNameOwner}")
//    fun selectByAccountNameOwner(@PathVariable("accountNameOwner") accountNameOwner: String): HttpResponse<List<Transaction>> {
//        val transactions: List<Transaction> = transactionService.findByAccountNameOwnerIgnoreCaseOrderByTransactionDate(accountNameOwner)
//        if (transactions.isEmpty()) {
//            return HttpResponse.notFound()
//        }
//        return HttpResponse.ok(transactions)
//    }
//
//    //curl http://localhost:8080/transaction/account/totals/chase_brian
//    @Get("/account/totals/{accountNameOwner}")
//    fun selectTotalsCleared(@PathVariable("accountNameOwner") accountNameOwner: String): HttpResponse<String> {
//        val results: MutableMap<String, BigDecimal> = HashMap()
//        var totalsCleared = 0.00
//        var totals = 0.00
//
//        results["totals"] = BigDecimal(totals)
//        results["totalsCleared"] = BigDecimal(totalsCleared)
//
//        //val results: Map<String, BigDecimal> = transactionService.getTotalsByAccountNameOwner(accountNameOwner)
//        //val results: Map<String, BigDecimal> = transactionService.getTotalsByAccountNameOwner(accountNameOwner)
//
//        logger.info("totals=${results}")
//
//        return HttpResponse.ok(mapper.writeValueAsString(results))
//    }
//
//    //curl http://localhost:8080/transaction/select/340c315d-39ad-4a02-a294-84a74c1c7ddc
//    @Get("/select/{guid}")
//    fun findTransaction(@PathVariable("guid") guid: String): HttpResponse<Transaction> {
//        logger.debug("findTransaction() guid = $guid")
//        val transactionOption: Optional<Transaction> = transactionService.findByGuid(guid)
//        if (transactionOption.isPresent) {
//            val transaction: Transaction = transactionOption.get()
//            println("transaction.categries = ${transaction.categories}")
//            return HttpResponse.ok(transaction)
//        }
//
//        logger.info("guid not found = $guid")
//        //return ResponseEntity.notFound().build()  //404
//        //throw EmptyTransactionException("Cannot find transaction.")
//        return HttpResponse.notModified()
//    }
//
//    //curl --header "Content-Type: application/json-patch+json" -X PATCH -d '{"guid":"9b9aea08-0dc2-4720-b20c-00b0df6af8ce", "description":"new"}' http://localhost:8080/transaction/update/9b9aea08-0dc2-4720-b20c-00b0df6af8ce
//    //curl --header "Content-Type: application/json-patch+json" -X PATCH -d '{"guid":"a064b942-1e78-4913-adb3-b992fc1b4dd3","sha256":"","accountType":"credit","accountNameOwner":"discover_brian","description":"Last Updated","category":"","notes":"","cleared":0,"reoccurring":false,"amount":"0.00","transactionDate":1512730594,"dateUpdated":1487332021,"dateAdded":1487332021}' http://localhost:8080/transaction/update/a064b942-1e78-4913-adb3-b992fc1b4dd3
//    //@PatchMapping(path = [("/update/{guid}")], consumes = [("application/json-patch+json")], produces = [("application/json")])
//    @Patch("/update/{guid}")
//    @Consumes("application/json-patch+json")
//    fun updateTransaction(@PathVariable("guid") guid: String, @Body transaction: Map<String, String>): HttpResponse<String> {
//        val toBePatchedTransaction = mapper.convertValue(transaction, Transaction::class.java)
//        val updateStatus: Boolean = transactionService.patchTransaction(toBePatchedTransaction)
//        if (updateStatus) {
//            return HttpResponse.ok("transaction patched")
//        }
//        return HttpResponse.badRequest("cannot patch transaction $transaction")
//    }
//
//    //curl --header "Content-Type: application/json" http://localhost:8080/transaction/insert -X POST -d ''
//    //@PostMapping(path = [("/insert")], consumes = [("application/json")], produces = [("application/json")])
//    @Post("/insert")
//    fun insertTransaction(@Body transaction: Transaction): HttpResponse<String> {
//        logger.info("insert - transaction.transactionDate: $transaction")
//        if (transactionService.insertTransaction(transaction)) {
//            logger.info(transaction.toString())
//            return HttpResponse.ok("transaction inserted")
//        }
//        return HttpResponse.badRequest("cannot insert transaction $transaction")
//    }
//
//    //curl --header "Content-Type: application/json" -X DELETE http://localhost:8080/transaction/delete/38739c5b-e2c6-41cc-82c2-d41f39a33f9a
//    //curl --header "Content-Type: application/json" -X DELETE http://localhost:8080/transaction/delete/00000000-e2c6-41cc-82c2-d41f39a33f9a
//    @Delete("/delete/{guid}")
//    fun deleteTransaction(@PathVariable("guid") guid: String): HttpResponse<String> {
//        val transactionOption: Optional<Transaction> = transactionService.findByGuid(guid)
//        if (transactionOption.isPresent) {
//            if (transactionService.deleteByGuid(guid)) {
//                return HttpResponse.ok("resource deleted")
//            }
//            return HttpResponse.badRequest("cannot delete transaction guild = $guid.")
//        }
//        return HttpResponse.badRequest("cannot delete transaction guild = $guid, transaction not present.")
//    }
//
////    //curl --header "Content-Type: application/json" http://localhost:8080/transaction/insert -X POST -d '{"accountType":"Credit"}'
////    //curl --header "Content-Type: application/json" http://localhost:8080/transaction/insert -X POST -d '{"amount":"abc"}'
////    @ResponseStatus(HttpStatus.BAD_REQUEST) //400
////    @ExceptionHandler(value = [ConstraintViolationException::class, NumberFormatException::class, EmptyResultDataAccessException::class,
////        MethodArgumentTypeMismatchException::class, HttpMessageNotReadableException::class, HttpMediaTypeNotSupportedException::class,
////        IllegalArgumentException::class, DataIntegrityViolationException::class])
////    fun handleBadHttpRequests(throwable: Throwable): Map<String, String> {
////        val response: MutableMap<String, String> = HashMap()
////        logger.info("Bad Request: ", throwable)
////        response["response"] = "BAD_REQUEST: " + throwable.javaClass.simpleName + " , message: " + throwable.message
////        logger.info(response.toString())
////        return response
////    }
////
////    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
////    @ExceptionHandler(value = [Exception::class])
////    fun handleHttpInternalError(throwable: Throwable): Map<String, String> {
////        val response: MutableMap<String, String> = HashMap()
////        logger.error("internal server error: ", throwable)
////        response["response"] = "INTERNAL_SERVER_ERROR: " + throwable.javaClass.simpleName + " , message: " + throwable.message
////        logger.info("response: $response")
////        return response
////    }
////
////    @ResponseStatus(HttpStatus.NOT_FOUND)
////    @ExceptionHandler(value = [EmptyTransactionException::class])
////    fun handleHttpNotFound(throwable: Throwable): Map<String, String> {
////        val response: MutableMap<String, String> = HashMap()
////        logger.error("not found: ", throwable)
////        response["response"] = "NOT_FOUND: " + throwable.javaClass.simpleName + " , message: " + throwable.message
////        return response
////    }
//
//    companion object {
//        private val mapper = ObjectMapper()
//    }
}