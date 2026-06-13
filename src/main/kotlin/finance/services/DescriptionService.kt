package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Description
import finance.repositories.DescriptionRepository
import finance.repositories.TransactionRepository
import io.micrometer.core.annotation.Timed
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.apache.logging.log4j.LogManager
import java.sql.Timestamp
import java.util.*
import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException
import jakarta.validation.Validator

@Singleton
open class DescriptionService(
    @Inject val descriptionRepository: DescriptionRepository,
    @Inject val transactionRepository: TransactionRepository,
    @Inject val validator: Validator,
    @Inject val meterService: MeterService
) {

    @Timed
    open fun insertDescription(description: Description): Boolean {
        val constraintViolations: Set<ConstraintViolation<Description>> = validator.validate(description)
        if (constraintViolations.isNotEmpty()) {
            constraintViolations.forEach { constraintViolation -> logger.error(constraintViolation.message) }
            logger.error("Cannot insert description as there is a constraint violation on the data.")
            meterService.incrementExceptionThrownCounter("ValidationException")
            throw ValidationException("Cannot insert description as there is a constraint violation on the data.")
        }
        description.dateAdded = Timestamp(Calendar.getInstance().time.time)
        description.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        descriptionRepository.saveAndFlush(description)
        return true
    }

    @Timed
    open fun deleteByDescriptionName(descriptionName: String): Boolean {
        descriptionRepository.deleteByDescriptionName(descriptionName)
        return true
    }

    @Timed
    open fun deleteByDescriptionName(owner: String, descriptionName: String): Boolean {
        descriptionRepository.deleteByOwnerAndDescriptionName(owner, descriptionName)
        return true
    }

    @Timed
    open fun fetchAllDescriptions(): List<Description> {
        val descriptions = descriptionRepository.findByActiveStatusOrderByDescriptionName(true)
        if (descriptions.isNotEmpty()) {
            val countMap = transactionRepository
                .countByDescriptionNameIn(descriptions.map { it.descriptionName })
                .associate { row -> row[0] as String to row[1] as Long }
            descriptions.forEach { it.descriptionCount = countMap[it.descriptionName] ?: 0L }
        }
        return descriptions
    }

    @Timed
    open fun fetchAllDescriptions(owner: String): List<Description> {
        val descriptions = descriptionRepository.findByOwnerAndActiveStatusOrderByDescriptionName(owner)
        if (descriptions.isNotEmpty()) {
            val countMap = transactionRepository
                .countByDescriptionNameIn(descriptions.map { it.descriptionName })
                .associate { row -> row[0] as String to row[1] as Long }
            descriptions.forEach { it.descriptionCount = countMap[it.descriptionName] ?: 0L }
        }
        return descriptions
    }

    @Timed
    open fun findByDescriptionName(descriptionName: String): Optional<Description> {
        return descriptionRepository.findByDescriptionName(descriptionName)
    }

    @Timed
    open fun findByOwnerAndDescriptionName(owner: String, descriptionName: String): Optional<Description> {
        return descriptionRepository.findByOwnerAndDescriptionName(owner, descriptionName)
    }

    @Timed
    open fun updateDescription(description: Description): Boolean {
        val existing = descriptionRepository.findByDescriptionName(description.descriptionName)
        if (existing.isPresent) {
            val toUpdate = existing.get()
            toUpdate.activeStatus = description.activeStatus
            toUpdate.dateUpdated = Timestamp(Calendar.getInstance().time.time)
            descriptionRepository.saveAndFlush(toUpdate)
            return true
        }
        return false
    }

    @Timed
    open fun mergeDescriptions(targetName: String, sourceNames: List<String>): Description {
        val targetOpt = descriptionRepository.findByDescriptionName(targetName)
        if (!targetOpt.isPresent) throw RuntimeException("Target description not found: $targetName")
        sourceNames.forEach { sourceName ->
            val sourceOpt = descriptionRepository.findByDescriptionName(sourceName)
            if (sourceOpt.isPresent) {
                val updatedCount = transactionRepository.bulkUpdateDescription(sourceName, targetName)
                logger.info("Merged $updatedCount transactions from '$sourceName' into '$targetName'")
                val source = sourceOpt.get()
                source.activeStatus = false
                source.dateUpdated = Timestamp(Calendar.getInstance().time.time)
                descriptionRepository.saveAndFlush(source)
            }
        }
        return targetOpt.get()
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger(DescriptionService::class.java)
    }
}
