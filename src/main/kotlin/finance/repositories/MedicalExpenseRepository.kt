package finance.repositories

import finance.domain.ClaimStatus
import finance.domain.MedicalExpense
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import jakarta.transaction.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Repository
interface MedicalExpenseRepository : JpaRepository<MedicalExpense, Long> {
    fun findByActiveStatusTrueOrderByServiceDateDesc(): List<MedicalExpense>

    fun findByTransactionId(transactionId: Long): MedicalExpense?

    fun findByMedicalExpenseIdAndActiveStatusTrue(medicalExpenseId: Long): MedicalExpense?

    fun findByServiceDateBetween(startDate: LocalDate, endDate: LocalDate): List<MedicalExpense>

    fun findByServiceDateBetweenAndActiveStatusTrue(startDate: LocalDate, endDate: LocalDate): List<MedicalExpense>

    fun findByProviderId(providerId: Long?): List<MedicalExpense>

    fun findByProviderIdAndActiveStatusTrue(providerId: Long?): List<MedicalExpense>

    fun findByFamilyMemberId(familyMemberId: Long?): List<MedicalExpense>

    fun findByFamilyMemberIdAndActiveStatusTrue(familyMemberId: Long?): List<MedicalExpense>

    fun findByClaimStatus(claimStatus: ClaimStatus): List<MedicalExpense>

    fun findByClaimStatusAndActiveStatusTrue(claimStatus: ClaimStatus): List<MedicalExpense>

    fun findByIsOutOfNetwork(isOutOfNetwork: Boolean): List<MedicalExpense>

    fun findByIsOutOfNetworkAndActiveStatusTrue(isOutOfNetwork: Boolean): List<MedicalExpense>

    fun findByClaimNumber(claimNumber: String): MedicalExpense?

    fun findByClaimNumberAndActiveStatusTrue(claimNumber: String): MedicalExpense?

    @Query(
        value = """
            SELECT me.* FROM t_medical_expense me
            WHERE me.transaction_id IN (
                SELECT t.transaction_id FROM t_transaction t WHERE t.account_id = :accountId
            )
            AND me.active_status = true
            ORDER BY me.service_date DESC
        """,
        nativeQuery = true,
    )
    fun findByAccountId(accountId: Long): List<MedicalExpense>

    @Query(
        value = """
            SELECT me.* FROM t_medical_expense me
            WHERE me.transaction_id IN (
                SELECT t.transaction_id FROM t_transaction t WHERE t.account_id = :accountId
            )
            AND me.service_date BETWEEN :startDate AND :endDate
            AND me.active_status = true
            ORDER BY me.service_date DESC
        """,
        nativeQuery = true,
    )
    fun findByAccountIdAndServiceDateBetween(accountId: Long, startDate: LocalDate, endDate: LocalDate): List<MedicalExpense>

    @Query(
        value = "SELECT SUM(me.billed_amount) FROM t_medical_expense me WHERE EXTRACT(YEAR FROM me.service_date) = :year AND me.active_status = true",
        nativeQuery = true,
    )
    fun getTotalBilledAmountByYear(year: Int): BigDecimal?

    @Query(
        value = "SELECT SUM(me.patient_responsibility) FROM t_medical_expense me WHERE EXTRACT(YEAR FROM me.service_date) = :year AND me.active_status = true",
        nativeQuery = true,
    )
    fun getTotalPatientResponsibilityByYear(year: Int): BigDecimal?

    @Query(
        value = "SELECT SUM(me.insurance_paid) FROM t_medical_expense me WHERE EXTRACT(YEAR FROM me.service_date) = :year AND me.active_status = true",
        nativeQuery = true,
    )
    fun getTotalInsurancePaidByYear(year: Int): BigDecimal?

    @Query(
        value = """
            SELECT me.* FROM t_medical_expense me
            WHERE me.family_member_id = :familyMemberId
            AND me.service_date BETWEEN :startDate AND :endDate
            AND me.active_status = true
            ORDER BY me.service_date DESC
        """,
        nativeQuery = true,
    )
    fun findByFamilyMemberIdAndServiceDateBetween(familyMemberId: Long?, startDate: LocalDate, endDate: LocalDate): List<MedicalExpense>

    @Query(
        value = "SELECT COUNT(*) FROM t_medical_expense me WHERE me.claim_status = :claimStatus AND me.active_status = true",
        nativeQuery = true,
    )
    fun countByClaimStatusAndActiveStatusTrue(claimStatus: String): Long

    @Query(
        value = """
            SELECT me.* FROM t_medical_expense me
            WHERE me.patient_responsibility > 0
            AND me.paid_date IS NULL
            AND me.active_status = true
            ORDER BY me.service_date DESC
        """,
        nativeQuery = true,
    )
    fun findOutstandingPatientBalances(): List<MedicalExpense>

    @Query(
        value = """
            SELECT me.* FROM t_medical_expense me
            WHERE me.claim_status NOT IN ('paid', 'closed', 'denied')
            AND me.active_status = true
            ORDER BY me.service_date DESC
        """,
        nativeQuery = true,
    )
    fun findActiveOpenClaims(): List<MedicalExpense>

    @Transactional
    @Query(
        value = "UPDATE t_medical_expense SET active_status = false, date_updated = NOW() WHERE medical_expense_id = :medicalExpenseId",
        nativeQuery = true,
    )
    fun softDeleteByMedicalExpenseId(medicalExpenseId: Long): Int

    @Transactional
    @Query(
        value = "UPDATE t_medical_expense SET claim_status = :claimStatus, date_updated = NOW() WHERE medical_expense_id = :medicalExpenseId",
        nativeQuery = true,
    )
    fun updateClaimStatus(medicalExpenseId: Long, claimStatus: String): Int

    @Query(
        value = "SELECT me.* FROM t_medical_expense me WHERE me.procedure_code = :procedureCode AND me.active_status = true ORDER BY me.service_date DESC",
        nativeQuery = true,
    )
    fun findByProcedureCodeAndActiveStatusTrue(procedureCode: String): List<MedicalExpense>

    @Query(
        value = "SELECT me.* FROM t_medical_expense me WHERE me.diagnosis_code = :diagnosisCode AND me.active_status = true ORDER BY me.service_date DESC",
        nativeQuery = true,
    )
    fun findByDiagnosisCodeAndActiveStatusTrue(diagnosisCode: String): List<MedicalExpense>

    @Query(
        value = "SELECT me.* FROM t_medical_expense me WHERE me.paid_amount = 0 AND me.active_status = true ORDER BY me.service_date DESC",
        nativeQuery = true,
    )
    fun findUnpaidMedicalExpenses(): List<MedicalExpense>

    @Query(
        value = "SELECT me.* FROM t_medical_expense me WHERE me.paid_amount > 0 AND me.paid_amount < me.patient_responsibility AND me.active_status = true ORDER BY me.service_date DESC",
        nativeQuery = true,
    )
    fun findPartiallyPaidMedicalExpenses(): List<MedicalExpense>

    @Query(
        value = "SELECT me.* FROM t_medical_expense me WHERE me.paid_amount >= me.patient_responsibility AND me.active_status = true ORDER BY me.service_date DESC",
        nativeQuery = true,
    )
    fun findFullyPaidMedicalExpenses(): List<MedicalExpense>

    @Query(
        value = "SELECT me.* FROM t_medical_expense me WHERE me.transaction_id IS NULL AND me.active_status = true ORDER BY me.service_date DESC",
        nativeQuery = true,
    )
    fun findMedicalExpensesWithoutTransaction(): List<MedicalExpense>

    @Query(
        value = "SELECT me.* FROM t_medical_expense me WHERE me.paid_amount > me.patient_responsibility AND me.active_status = true ORDER BY me.service_date DESC",
        nativeQuery = true,
    )
    fun findOverpaidMedicalExpenses(): List<MedicalExpense>

    @Query(
        value = "SELECT SUM(me.paid_amount) FROM t_medical_expense me WHERE EXTRACT(YEAR FROM me.service_date) = :year AND me.active_status = true",
        nativeQuery = true,
    )
    fun getTotalPaidAmountByYear(year: Int): BigDecimal?

    @Query(
        value = "SELECT SUM(me.patient_responsibility - me.paid_amount) FROM t_medical_expense me WHERE me.paid_amount < me.patient_responsibility AND me.active_status = true",
        nativeQuery = true,
    )
    fun getTotalUnpaidBalance(): BigDecimal?

    @Transactional
    @Query(
        value = "UPDATE t_medical_expense SET paid_amount = :paidAmount, date_updated = NOW() WHERE medical_expense_id = :medicalExpenseId",
        nativeQuery = true,
    )
    fun updatePaidAmount(medicalExpenseId: Long, paidAmount: BigDecimal): Int

    fun findByOwnerAndActiveStatusTrueOrderByServiceDateDesc(owner: String): List<MedicalExpense>

    fun findByOwnerAndTransactionId(owner: String, transactionId: Long): MedicalExpense?

    fun findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(owner: String, medicalExpenseId: Long): MedicalExpense?

    fun findByOwnerAndServiceDateBetweenAndActiveStatusTrue(owner: String, startDate: LocalDate, endDate: LocalDate): List<MedicalExpense>

    fun findByOwnerAndProviderIdAndActiveStatusTrue(owner: String, providerId: Long?): List<MedicalExpense>

    fun findByOwnerAndFamilyMemberIdAndActiveStatusTrue(owner: String, familyMemberId: Long?): List<MedicalExpense>

    fun findByOwnerAndClaimStatusAndActiveStatusTrue(owner: String, claimStatus: ClaimStatus): List<MedicalExpense>

    fun findByOwnerAndIsOutOfNetworkAndActiveStatusTrue(owner: String, isOutOfNetwork: Boolean): List<MedicalExpense>

    @Query(
        value = """
            SELECT me.* FROM t_medical_expense me
            WHERE me.owner = :owner
            AND me.patient_responsibility > 0
            AND me.paid_date IS NULL
            AND me.active_status = true
            ORDER BY me.service_date DESC
        """,
        nativeQuery = true,
    )
    fun findOutstandingPatientBalancesByOwner(owner: String): List<MedicalExpense>

    @Query(
        value = """
            SELECT me.* FROM t_medical_expense me
            WHERE me.owner = :owner
            AND me.claim_status NOT IN ('paid', 'closed', 'denied')
            AND me.active_status = true
            ORDER BY me.service_date DESC
        """,
        nativeQuery = true,
    )
    fun findActiveOpenClaimsByOwner(owner: String): List<MedicalExpense>

    @Transactional
    @Query(
        value = "UPDATE t_medical_expense SET active_status = false, date_updated = NOW() WHERE medical_expense_id = :medicalExpenseId AND owner = :owner",
        nativeQuery = true,
    )
    fun softDeleteByOwnerAndMedicalExpenseId(owner: String, medicalExpenseId: Long): Int

    @Transactional
    @Query(
        value = "UPDATE t_medical_expense SET claim_status = :claimStatus, date_updated = NOW() WHERE medical_expense_id = :medicalExpenseId AND owner = :owner",
        nativeQuery = true,
    )
    fun updateClaimStatusByOwner(owner: String, medicalExpenseId: Long, claimStatus: String): Int

    @Query(
        value = "SELECT me.* FROM t_medical_expense me WHERE me.owner = :owner AND me.procedure_code = :procedureCode AND me.active_status = true ORDER BY me.service_date DESC",
        nativeQuery = true,
    )
    fun findByOwnerAndProcedureCodeAndActiveStatusTrue(owner: String, procedureCode: String): List<MedicalExpense>

    @Query(
        value = "SELECT me.* FROM t_medical_expense me WHERE me.owner = :owner AND me.diagnosis_code = :diagnosisCode AND me.active_status = true ORDER BY me.service_date DESC",
        nativeQuery = true,
    )
    fun findByOwnerAndDiagnosisCodeAndActiveStatusTrue(owner: String, diagnosisCode: String): List<MedicalExpense>

    @Query(
        value = "SELECT me.* FROM t_medical_expense me WHERE me.owner = :owner AND me.paid_amount = 0 AND me.active_status = true ORDER BY me.service_date DESC",
        nativeQuery = true,
    )
    fun findUnpaidMedicalExpensesByOwner(owner: String): List<MedicalExpense>

    @Query(
        value = "SELECT me.* FROM t_medical_expense me WHERE me.owner = :owner AND me.paid_amount > 0 AND me.paid_amount < me.patient_responsibility AND me.active_status = true ORDER BY me.service_date DESC",
        nativeQuery = true,
    )
    fun findPartiallyPaidMedicalExpensesByOwner(owner: String): List<MedicalExpense>

    @Query(
        value = "SELECT me.* FROM t_medical_expense me WHERE me.owner = :owner AND me.paid_amount >= me.patient_responsibility AND me.active_status = true ORDER BY me.service_date DESC",
        nativeQuery = true,
    )
    fun findFullyPaidMedicalExpensesByOwner(owner: String): List<MedicalExpense>

    @Query(
        value = "SELECT me.* FROM t_medical_expense me WHERE me.owner = :owner AND me.transaction_id IS NULL AND me.active_status = true ORDER BY me.service_date DESC",
        nativeQuery = true,
    )
    fun findMedicalExpensesWithoutTransactionByOwner(owner: String): List<MedicalExpense>

    @Query(
        value = "SELECT me.* FROM t_medical_expense me WHERE me.owner = :owner AND me.paid_amount > me.patient_responsibility AND me.active_status = true ORDER BY me.service_date DESC",
        nativeQuery = true,
    )
    fun findOverpaidMedicalExpensesByOwner(owner: String): List<MedicalExpense>

    @Query(
        value = "SELECT SUM(me.paid_amount) FROM t_medical_expense me WHERE me.owner = :owner AND EXTRACT(YEAR FROM me.service_date) = :year AND me.active_status = true",
        nativeQuery = true,
    )
    fun getTotalPaidAmountByOwnerAndYear(owner: String, year: Int): BigDecimal?

    @Query(
        value = "SELECT SUM(me.patient_responsibility - me.paid_amount) FROM t_medical_expense me WHERE me.owner = :owner AND me.paid_amount < me.patient_responsibility AND me.active_status = true",
        nativeQuery = true,
    )
    fun getTotalUnpaidBalanceByOwner(owner: String): BigDecimal?
}
