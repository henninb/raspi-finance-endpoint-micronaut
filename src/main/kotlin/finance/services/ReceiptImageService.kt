package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.ReceiptImage
import finance.repositories.ReceiptImageRepository
import io.micrometer.core.annotation.Timed
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.apache.logging.log4j.LogManager
import java.sql.Timestamp
import java.util.*
import javax.validation.ConstraintViolation
import javax.validation.ValidationException
import javax.validation.Validator

@Singleton
open class ReceiptImageService(
    @Inject val receiptImageRepository: ReceiptImageRepository,
    @Inject val validator: Validator,
    @Inject val meterService: MeterService
) {
    @Timed
    open fun insertReceiptImage(receiptImage: ReceiptImage): ReceiptImage {

        val constraintViolations: Set<ConstraintViolation<ReceiptImage>> = validator.validate(receiptImage)
        if (constraintViolations.isNotEmpty()) {
            constraintViolations.forEach { constraintViolation -> logger.error(constraintViolation.message) }
            logger.error("Cannot insert receiptImage as there is a constraint violation on the data.")
            meterService.incrementExceptionThrownCounter("ValidationException")
            throw ValidationException("Cannot insert receiptImage as there is a constraint violation on the data.")
        }

        receiptImage.dateAdded = Timestamp(Calendar.getInstance().time.time)
        receiptImage.dateUpdated = Timestamp(Calendar.getInstance().time.time)

        return receiptImageRepository.saveAndFlush(receiptImage)
    }

    @Timed
    open fun findByReceiptImageId(receiptImageId: Long): Optional<ReceiptImage> {
        return receiptImageRepository.findById(receiptImageId)
    }

    @Timed
    open fun deleteReceiptImage(receiptImage: ReceiptImage): Boolean {
        receiptImageRepository.deleteById(receiptImage.receiptImageId)
        return true
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}
