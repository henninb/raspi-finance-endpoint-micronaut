package finance.domain

class InvalidTransactionStateException(
    message: String,
) : DomainException(message)
