package finance.controllers

import finance.domain.ClaimStatus
import finance.domain.MedicalExpense
import finance.exceptions.DuplicateMedicalExpenseException
import finance.services.MedicalExpenseService
import finance.services.OwnerExtractorService
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import jakarta.inject.Inject
import java.math.BigDecimal
import java.time.LocalDate

@Controller("/api/medical-expenses")
class MedicalExpenseController(
    @Inject val medicalExpenseService: MedicalExpenseService,
    @Inject val ownerExtractorService: OwnerExtractorService,
) {

    @Get("/active", produces = ["application/json"])
    fun selectAllActive(): HttpResponse<List<MedicalExpense>> =
        HttpResponse.ok(medicalExpenseService.findAllActive())

    @Get("/{medicalExpenseId}", produces = ["application/json"])
    fun selectById(@PathVariable medicalExpenseId: Long): HttpResponse<MedicalExpense> {
        val optional = medicalExpenseService.findById(medicalExpenseId)
        return if (optional.isPresent) HttpResponse.ok(optional.get()) else HttpResponse.notFound()
    }

    @Post(consumes = ["application/json"], produces = ["application/json"])
    fun insertMedicalExpense(@Body medicalExpense: MedicalExpense, request: HttpRequest<*>): HttpResponse<MedicalExpense> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        if (medicalExpense.owner.isBlank()) medicalExpense.owner = owner
        return try {
            medicalExpenseService.insertMedicalExpense(medicalExpense)
            HttpResponse.status<MedicalExpense>(HttpStatus.CREATED).body(medicalExpense)
        } catch (e: DuplicateMedicalExpenseException) {
            HttpResponse.badRequest()
        }
    }

    @Put("/{medicalExpenseId}", consumes = ["application/json"], produces = ["application/json"])
    fun updateMedicalExpense(@PathVariable medicalExpenseId: Long, @Body medicalExpense: MedicalExpense, request: HttpRequest<*>): HttpResponse<MedicalExpense> {
        val owner = ownerExtractorService.extractOwner(request) ?: return HttpResponse.status(HttpStatus.UNAUTHORIZED)
        medicalExpense.medicalExpenseId = medicalExpenseId
        if (medicalExpense.owner.isBlank()) medicalExpense.owner = owner
        return try {
            medicalExpenseService.updateMedicalExpense(medicalExpense)
            HttpResponse.ok(medicalExpense)
        } catch (e: IllegalArgumentException) {
            HttpResponse.notFound()
        }
    }

    @Delete("/{medicalExpenseId}", produces = ["application/json"])
    fun deleteByMedicalExpenseId(@PathVariable medicalExpenseId: Long): HttpResponse<MedicalExpense> {
        val optional = medicalExpenseService.findById(medicalExpenseId)
        if (optional.isPresent) {
            medicalExpenseService.softDeleteMedicalExpense(medicalExpenseId)
            return HttpResponse.ok(optional.get())
        }
        return HttpResponse.notFound()
    }

    @Put("/{medicalExpenseId}/claim-status", produces = ["application/json"])
    fun updateClaimStatus(@PathVariable medicalExpenseId: Long, @QueryValue claimStatus: ClaimStatus): HttpResponse<Map<String, String>> =
        if (medicalExpenseService.updateClaimStatus(medicalExpenseId, claimStatus))
            HttpResponse.ok(mapOf("message" to "claim status updated"))
        else HttpResponse.notFound()

    @Get("/transaction/{transactionId}", produces = ["application/json"])
    fun selectByTransactionId(@PathVariable transactionId: Long): HttpResponse<MedicalExpense> {
        val expense = medicalExpenseService.findByTransactionId(transactionId)
        return if (expense != null) HttpResponse.ok(expense) else HttpResponse.notFound()
    }

    @Get("/account/{accountId}", produces = ["application/json"])
    fun selectByAccountId(@PathVariable accountId: Long): HttpResponse<List<MedicalExpense>> =
        HttpResponse.ok(medicalExpenseService.findByAccountId(accountId))

    @Get("/account/{accountId}/date-range", produces = ["application/json"])
    fun selectByAccountIdAndDateRange(@PathVariable accountId: Long, @QueryValue startDate: LocalDate, @QueryValue endDate: LocalDate): HttpResponse<List<MedicalExpense>> =
        HttpResponse.ok(medicalExpenseService.findByAccountIdAndDateRange(accountId, startDate, endDate))

    @Get("/provider/{providerId}", produces = ["application/json"])
    fun selectByProviderId(@PathVariable providerId: Long): HttpResponse<List<MedicalExpense>> =
        HttpResponse.ok(medicalExpenseService.findByProviderId(providerId))

    @Get("/family-member/{familyMemberId}", produces = ["application/json"])
    fun selectByFamilyMemberId(@PathVariable familyMemberId: Long): HttpResponse<List<MedicalExpense>> =
        HttpResponse.ok(medicalExpenseService.findByFamilyMemberId(familyMemberId))

    @Get("/family-member/{familyMemberId}/date-range", produces = ["application/json"])
    fun selectByFamilyMemberIdAndDateRange(@PathVariable familyMemberId: Long, @QueryValue startDate: LocalDate, @QueryValue endDate: LocalDate): HttpResponse<List<MedicalExpense>> =
        HttpResponse.ok(medicalExpenseService.findByFamilyMemberAndDateRange(familyMemberId, startDate, endDate))

    @Get("/claim-status/{claimStatus}", produces = ["application/json"])
    fun selectByClaimStatus(@PathVariable claimStatus: ClaimStatus): HttpResponse<List<MedicalExpense>> =
        HttpResponse.ok(medicalExpenseService.findByClaimStatus(claimStatus))

    @Get("/out-of-network", produces = ["application/json"])
    fun selectOutOfNetwork(): HttpResponse<List<MedicalExpense>> = HttpResponse.ok(medicalExpenseService.findOutOfNetworkExpenses())

    @Get("/outstanding-balances", produces = ["application/json"])
    fun selectOutstandingBalances(): HttpResponse<List<MedicalExpense>> = HttpResponse.ok(medicalExpenseService.findOutstandingPatientBalances())

    @Get("/open-claims", produces = ["application/json"])
    fun selectOpenClaims(): HttpResponse<List<MedicalExpense>> = HttpResponse.ok(medicalExpenseService.findActiveOpenClaims())

    @Get("/totals/year/{year}", produces = ["application/json"])
    fun selectTotalsByYear(@PathVariable year: Int): HttpResponse<Map<String, BigDecimal>> =
        HttpResponse.ok(mapOf(
            "totalBilled" to medicalExpenseService.getTotalBilledAmountByYear(year),
            "totalPatientResponsibility" to medicalExpenseService.getTotalPatientResponsibilityByYear(year),
            "totalInsurancePaid" to medicalExpenseService.getTotalInsurancePaidByYear(year),
        ))

    @Get("/claim-status-counts", produces = ["application/json"])
    fun selectClaimStatusCounts(): HttpResponse<Map<ClaimStatus, Long>> = HttpResponse.ok(medicalExpenseService.getClaimStatusCounts())

    @Get("/procedure-code/{procedureCode}", produces = ["application/json"])
    fun selectByProcedureCode(@PathVariable procedureCode: String): HttpResponse<List<MedicalExpense>> = HttpResponse.ok(medicalExpenseService.findByProcedureCode(procedureCode))

    @Get("/diagnosis-code/{diagnosisCode}", produces = ["application/json"])
    fun selectByDiagnosisCode(@PathVariable diagnosisCode: String): HttpResponse<List<MedicalExpense>> = HttpResponse.ok(medicalExpenseService.findByDiagnosisCode(diagnosisCode))

    @Get("/date-range", produces = ["application/json"])
    fun selectByDateRange(@QueryValue startDate: LocalDate, @QueryValue endDate: LocalDate): HttpResponse<List<MedicalExpense>> = HttpResponse.ok(medicalExpenseService.findByDateRange(startDate, endDate))

    @Post("/{medicalExpenseId}/payments/{transactionId}", produces = ["application/json"])
    fun linkPaymentTransaction(@PathVariable medicalExpenseId: Long, @PathVariable transactionId: Long): HttpResponse<MedicalExpense> =
        try { HttpResponse.ok(medicalExpenseService.linkPaymentTransaction(medicalExpenseId, transactionId)) }
        catch (e: DuplicateMedicalExpenseException) { HttpResponse.badRequest() }
        catch (e: IllegalArgumentException) { HttpResponse.notFound() }

    @Delete("/{medicalExpenseId}/payments", produces = ["application/json"])
    fun unlinkPaymentTransaction(@PathVariable medicalExpenseId: Long): HttpResponse<MedicalExpense> =
        try { HttpResponse.ok(medicalExpenseService.unlinkPaymentTransaction(medicalExpenseId)) }
        catch (e: IllegalArgumentException) { HttpResponse.notFound() }

    @Put("/{medicalExpenseId}/sync-payment", produces = ["application/json"])
    fun syncPaymentAmount(@PathVariable medicalExpenseId: Long): HttpResponse<MedicalExpense> =
        try { HttpResponse.ok(medicalExpenseService.syncPaymentAmount(medicalExpenseId)) }
        catch (e: IllegalArgumentException) { HttpResponse.notFound() }

    @Get("/unpaid", produces = ["application/json"])
    fun selectUnpaid(): HttpResponse<List<MedicalExpense>> = HttpResponse.ok(medicalExpenseService.findUnpaid())

    @Get("/partially-paid", produces = ["application/json"])
    fun selectPartiallyPaid(): HttpResponse<List<MedicalExpense>> = HttpResponse.ok(medicalExpenseService.findPartiallyPaid())

    @Get("/fully-paid", produces = ["application/json"])
    fun selectFullyPaid(): HttpResponse<List<MedicalExpense>> = HttpResponse.ok(medicalExpenseService.findFullyPaid())

    @Get("/without-transaction", produces = ["application/json"])
    fun selectWithoutTransaction(): HttpResponse<List<MedicalExpense>> = HttpResponse.ok(medicalExpenseService.findWithoutTransaction())

    @Get("/overpaid", produces = ["application/json"])
    fun selectOverpaid(): HttpResponse<List<MedicalExpense>> = HttpResponse.ok(medicalExpenseService.findOverpaid())

    @Get("/totals/year/{year}/paid", produces = ["application/json"])
    fun selectTotalPaidByYear(@PathVariable year: Int): HttpResponse<Map<String, BigDecimal>> =
        HttpResponse.ok(mapOf("totalPaid" to medicalExpenseService.getTotalPaidAmountByYear(year), "year" to BigDecimal(year)))

    @Get("/totals/unpaid-balance", produces = ["application/json"])
    fun selectTotalUnpaidBalance(): HttpResponse<Map<String, BigDecimal>> =
        HttpResponse.ok(mapOf("totalUnpaidBalance" to medicalExpenseService.getTotalUnpaidBalance()))
}
