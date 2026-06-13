package finance.repositories

import finance.domain.Transaction
import finance.domain.TransactionState

/**
 * Named filter constants and helper data class for building owner-scoped
 * transaction searches. The actual query execution goes through
 * [TransactionRepository.search].
 */
object TransactionSpecifications {

    data class SearchCriteria(
        val owner: String,
        val accountNameOwner: String? = null,
        val startDate: String? = null,
        val endDate: String? = null,
        val transactionState: String? = null,
        val minAmount: Double? = null,
        val maxAmount: Double? = null,
        val description: String? = null,
        val category: String? = null,
    )

    fun validate(criteria: SearchCriteria) {
        require(criteria.owner.isNotBlank()) { "owner must not be blank" }
        if (criteria.startDate != null && criteria.endDate != null) {
            require(criteria.startDate <= criteria.endDate) { "startDate must not be after endDate" }
        }
        if (criteria.minAmount != null && criteria.maxAmount != null) {
            require(criteria.minAmount <= criteria.maxAmount) { "minAmount must not exceed maxAmount" }
        }
    }
}
