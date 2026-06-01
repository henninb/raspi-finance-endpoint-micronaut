package finance.services

import finance.domain.ClaimStatus
import finance.domain.MedicalExpense
import finance.exceptions.DuplicateMedicalExpenseException
import finance.repositories.MedicalExpenseRepository
import io.micrometer.core.annotation.Timed
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import org.apache.logging.log4j.LogManager
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate
import java.util.Optional

@Singleton
open class MedicalExpenseService(
    @Inject val medicalExpenseRepository: MedicalExpenseRepository,
    @Inject val validator: Validator,
    @Inject val meterService: MeterService,
) {
    @Timed
    open fun findAllActive(): List<MedicalExpense> = medicalExpenseRepository.findByActiveStatusTrueOrderByServiceDateDesc()

    @Timed
    open fun findById(id: Long): Optional<MedicalExpense> =
        Optional.ofNullable(medicalExpenseRepository.findByMedicalExpenseIdAndActiveStatusTrue(id))

    @Timed
    open fun insertMedicalExpense(medicalExpense: MedicalExpense): MedicalExpense {
        val constraintViolations: Set<ConstraintViolation<MedicalExpense>> = validator.validate(medicalExpense)
        if (constraintViolations.isNotEmpty()) {
            constraintViolations.forEach { logger.error(it.message) }
            meterService.incrementExceptionThrownCounter("ValidationException")
            throw ValidationException("Cannot insert medical expense due to constraint violation.")
        }
        medicalExpense.transactionId?.let { transactionId ->
            if (transactionId > 0) {
                val existing = medicalExpenseRepository.findByTransactionId(transactionId)
                if (existing != null) {
                    throw DuplicateMedicalExpenseException("Medical expense already exists for transaction ID: $transactionId")
                }
            }
        }
        medicalExpense.dateAdded = Timestamp(System.currentTimeMillis())
        medicalExpense.dateUpdated = Timestamp(System.currentTimeMillis())
        return medicalExpenseRepository.saveAndFlush(medicalExpense)
    }

    @Timed
    open fun updateMedicalExpense(medicalExpense: MedicalExpense): MedicalExpense {
        val existing = medicalExpenseRepository.findByMedicalExpenseIdAndActiveStatusTrue(medicalExpense.medicalExpenseId)
            ?: throw IllegalArgumentException("Medical expense not found: ${medicalExpense.medicalExpenseId}")
        medicalExpense.dateAdded = existing.dateAdded
        medicalExpense.dateUpdated = Timestamp(System.currentTimeMillis())
        return medicalExpenseRepository.saveAndFlush(medicalExpense)
    }

    @Timed
    open fun softDeleteMedicalExpense(medicalExpenseId: Long): Boolean {
        val rows = medicalExpenseRepository.softDeleteByMedicalExpenseId(medicalExpenseId)
        return rows > 0
    }

    @Timed
    open fun findByTransactionId(transactionId: Long): MedicalExpense? =
        medicalExpenseRepository.findByTransactionId(transactionId)

    @Timed
    open fun findByAccountId(accountId: Long): List<MedicalExpense> =
        medicalExpenseRepository.findByAccountId(accountId)

    @Timed
    open fun findByAccountIdAndDateRange(accountId: Long, startDate: LocalDate, endDate: LocalDate): List<MedicalExpense> =
        medicalExpenseRepository.findByAccountIdAndServiceDateBetween(accountId, startDate, endDate)

    @Timed
    open fun findByProviderId(providerId: Long): List<MedicalExpense> =
        medicalExpenseRepository.findByProviderIdAndActiveStatusTrue(providerId)

    @Timed
    open fun findByFamilyMemberId(familyMemberId: Long): List<MedicalExpense> =
        medicalExpenseRepository.findByFamilyMemberIdAndActiveStatusTrue(familyMemberId)

    @Timed
    open fun findByFamilyMemberAndDateRange(familyMemberId: Long, startDate: LocalDate, endDate: LocalDate): List<MedicalExpense> =
        medicalExpenseRepository.findByFamilyMemberIdAndServiceDateBetween(familyMemberId, startDate, endDate)

    @Timed
    open fun findByClaimStatus(claimStatus: ClaimStatus): List<MedicalExpense> =
        medicalExpenseRepository.findByClaimStatusAndActiveStatusTrue(claimStatus)

    @Timed
    open fun findOutOfNetworkExpenses(): List<MedicalExpense> =
        medicalExpenseRepository.findByIsOutOfNetworkAndActiveStatusTrue(true)

    @Timed
    open fun findOutstandingPatientBalances(): List<MedicalExpense> =
        medicalExpenseRepository.findOutstandingPatientBalances()

    @Timed
    open fun findActiveOpenClaims(): List<MedicalExpense> =
        medicalExpenseRepository.findActiveOpenClaims()

    @Timed
    open fun updateClaimStatus(medicalExpenseId: Long, claimStatus: ClaimStatus): Boolean {
        val rows = medicalExpenseRepository.updateClaimStatus(medicalExpenseId, claimStatus.label)
        return rows > 0
    }

    @Timed
    open fun getTotalBilledAmountByYear(year: Int): BigDecimal =
        medicalExpenseRepository.getTotalBilledAmountByYear(year) ?: BigDecimal.ZERO

    @Timed
    open fun getTotalPatientResponsibilityByYear(year: Int): BigDecimal =
        medicalExpenseRepository.getTotalPatientResponsibilityByYear(year) ?: BigDecimal.ZERO

    @Timed
    open fun getTotalInsurancePaidByYear(year: Int): BigDecimal =
        medicalExpenseRepository.getTotalInsurancePaidByYear(year) ?: BigDecimal.ZERO

    @Timed
    open fun getClaimStatusCounts(): Map<ClaimStatus, Long> =
        ClaimStatus.values().associateWith { status ->
            medicalExpenseRepository.countByClaimStatusAndActiveStatusTrue(status.label)
        }

    @Timed
    open fun findByProcedureCode(procedureCode: String): List<MedicalExpense> =
        medicalExpenseRepository.findByProcedureCodeAndActiveStatusTrue(procedureCode)

    @Timed
    open fun findByDiagnosisCode(diagnosisCode: String): List<MedicalExpense> =
        medicalExpenseRepository.findByDiagnosisCodeAndActiveStatusTrue(diagnosisCode)

    @Timed
    open fun findByDateRange(startDate: LocalDate, endDate: LocalDate): List<MedicalExpense> =
        medicalExpenseRepository.findByServiceDateBetweenAndActiveStatusTrue(startDate, endDate)

    @Timed
    open fun linkPaymentTransaction(medicalExpenseId: Long, transactionId: Long): MedicalExpense {
        val medicalExpense = medicalExpenseRepository.findByMedicalExpenseIdAndActiveStatusTrue(medicalExpenseId)
            ?: throw IllegalArgumentException("Medical expense not found: $medicalExpenseId")
        val existing = medicalExpenseRepository.findByTransactionId(transactionId)
        if (existing != null && existing.medicalExpenseId != medicalExpenseId) {
            throw DuplicateMedicalExpenseException("Transaction $transactionId is already linked to medical expense ${existing.medicalExpenseId}")
        }
        medicalExpense.transactionId = transactionId
        return medicalExpenseRepository.saveAndFlush(medicalExpense)
    }

    @Timed
    open fun unlinkPaymentTransaction(medicalExpenseId: Long): MedicalExpense {
        val medicalExpense = medicalExpenseRepository.findByMedicalExpenseIdAndActiveStatusTrue(medicalExpenseId)
            ?: throw IllegalArgumentException("Medical expense not found: $medicalExpenseId")
        medicalExpense.transactionId = null
        medicalExpense.paidAmount = BigDecimal.ZERO
        return medicalExpenseRepository.saveAndFlush(medicalExpense)
    }

    @Timed
    open fun syncPaymentAmount(medicalExpenseId: Long): MedicalExpense {
        val medicalExpense = medicalExpenseRepository.findByMedicalExpenseIdAndActiveStatusTrue(medicalExpenseId)
            ?: throw IllegalArgumentException("Medical expense not found: $medicalExpenseId")
        if (medicalExpense.transactionId == null) {
            medicalExpense.paidAmount = BigDecimal.ZERO
        }
        return medicalExpenseRepository.saveAndFlush(medicalExpense)
    }

    @Timed
    open fun findUnpaid(): List<MedicalExpense> = medicalExpenseRepository.findUnpaidMedicalExpenses()

    @Timed
    open fun findPartiallyPaid(): List<MedicalExpense> = medicalExpenseRepository.findPartiallyPaidMedicalExpenses()

    @Timed
    open fun findFullyPaid(): List<MedicalExpense> = medicalExpenseRepository.findFullyPaidMedicalExpenses()

    @Timed
    open fun findWithoutTransaction(): List<MedicalExpense> = medicalExpenseRepository.findMedicalExpensesWithoutTransaction()

    @Timed
    open fun findOverpaid(): List<MedicalExpense> = medicalExpenseRepository.findOverpaidMedicalExpenses()

    @Timed
    open fun getTotalPaidAmountByYear(year: Int): BigDecimal =
        medicalExpenseRepository.getTotalPaidAmountByYear(year) ?: BigDecimal.ZERO

    @Timed
    open fun getTotalUnpaidBalance(): BigDecimal =
        medicalExpenseRepository.getTotalUnpaidBalance() ?: BigDecimal.ZERO

    companion object {
        private val logger = LogManager.getLogger()
    }
}
