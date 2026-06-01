package finance.exceptions

import finance.domain.DomainException

class DuplicateMedicalExpenseException(
    message: String,
) : DomainException(message)
