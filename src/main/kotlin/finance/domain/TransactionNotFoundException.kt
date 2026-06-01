package finance.domain

class TransactionNotFoundException(
    message: String,
) : DomainException(message)
