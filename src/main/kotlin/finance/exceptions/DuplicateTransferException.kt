package finance.exceptions

import finance.domain.DomainException

class DuplicateTransferException(
    message: String,
) : DomainException(message)
