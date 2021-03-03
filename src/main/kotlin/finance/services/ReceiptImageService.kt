package finance.services

import finance.repositories.PaymentRepository
import finance.repositories.ReceiptImageRepository
import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.Validator

@Singleton
class ReceiptImageService(@Inject val receiptImageRepository: ReceiptImageRepository, @Inject val validator: Validator, @Inject val meterService: MeterService) {
}
