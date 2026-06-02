package finance.domain

import com.fasterxml.jackson.annotation.JsonCreator

enum class PaymentBehavior(val label: String, val description: String) {
    // asset → liability: source decreases, liability decreases (debt paid down)
    BILL_PAYMENT(
        "bill_payment",
        "Payment from asset account to liability account (paying down debt)",
    ),

    // asset → asset: source decreases, destination increases
    TRANSFER(
        "transfer",
        "Transfer between two asset accounts",
    ),

    // liability → asset: liability increases (more debt), asset increases (cash received)
    CASH_ADVANCE(
        "cash_advance",
        "Cash advance from liability account to asset account (borrowing)",
    ),

    // liability → liability: source liability increases (charging), destination liability decreases (paid off)
    BALANCE_TRANSFER(
        "balance_transfer",
        "Balance transfer between two liability accounts",
    ),

    UNDEFINED(
        "undefined",
        "Unknown or unsupported account type combination",
    );

    override fun toString(): String = label

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(value: String?): PaymentBehavior =
            values().firstOrNull { it.label == value?.lowercase() } ?: UNDEFINED

        @JvmStatic
        fun inferBehavior(
            sourceAccountType: AccountType,
            destinationAccountType: AccountType,
        ): PaymentBehavior {
            val sourceIsAsset = sourceAccountType == AccountType.Debit
            val sourceIsLiability = sourceAccountType == AccountType.Credit
            val destIsAsset = destinationAccountType == AccountType.Debit
            val destIsLiability = destinationAccountType == AccountType.Credit
            return when {
                sourceIsAsset && destIsLiability -> BILL_PAYMENT
                sourceIsAsset && destIsAsset -> TRANSFER
                sourceIsLiability && destIsAsset -> CASH_ADVANCE
                sourceIsLiability && destIsLiability -> BALANCE_TRANSFER
                else -> UNDEFINED
            }
        }
    }
}
